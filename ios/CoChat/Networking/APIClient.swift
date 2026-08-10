import Foundation

enum NetworkConfig {
    // The iOS Simulator shares the host Mac's network namespace, so it can
    // reach the locally running backend directly via localhost — no emulator
    // alias needed (unlike Android). For a physical device on the same LAN,
    // swap this for the dev machine's LAN IP and add it to the ATS exception
    // list in project.yml; a production build should point at https:// instead.
    static let baseURL = URL(string: "http://localhost:4000/api/")!
    static let socketURL = URL(string: "http://localhost:4000")!
}

struct APIError: Error, LocalizedError {
    let statusCode: Int
    let message: String
    var errorDescription: String? { message }
}

actor APIClient {
    private let tokenStore: TokenStore
    private let session = URLSession(configuration: .default)
    private let decoder: JSONDecoder = { let d = JSONDecoder(); return d }()
    private let encoder: JSONEncoder = { let e = JSONEncoder(); return e }()

    /// Guards against concurrent requests all triggering their own refresh
    /// call the instant the access token expires.
    private var refreshTask: Task<Void, Error>?

    init(tokenStore: TokenStore) {
        self.tokenStore = tokenStore
    }

    // MARK: - JSON requests

    func get<T: Decodable>(_ path: String, query: [String: String] = [:]) async throws -> T {
        try await send(path: path, method: "GET", query: query, body: nil as Data?)
    }

    func post<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        let data = try encoder.encode(body)
        return try await send(path: path, method: "POST", query: [:], body: data)
    }

    func patch<T: Decodable, B: Encodable>(_ path: String, body: B) async throws -> T {
        let data = try encoder.encode(body)
        return try await send(path: path, method: "PATCH", query: [:], body: data)
    }

    /// For endpoints whose response body is empty/unused (204s and similar) —
    /// named distinctly from the generic `post`/`patch`/`delete` above so Swift
    /// never has to infer an unconstrained `T` from a fully-discarded result.
    func postVoid(_ path: String) async throws {
        let _: EmptyResponse = try await send(path: path, method: "POST", query: [:], body: nil as Data?)
    }

    func postVoid<B: Encodable>(_ path: String, body: B) async throws {
        let data = try encoder.encode(body)
        let _: EmptyResponse = try await send(path: path, method: "POST", query: [:], body: data)
    }

    func deleteVoid(_ path: String) async throws {
        let _: EmptyResponse = try await send(path: path, method: "DELETE", query: [:], body: nil as Data?)
    }

    // MARK: - Multipart requests

    func postMultipart<T: Decodable>(
        _ path: String,
        fields: [String: String] = [:],
        fileField: String? = nil,
        fileData: Data? = nil,
        fileName: String? = nil,
        mimeType: String? = nil
    ) async throws -> T {
        let (body, contentType) = buildMultipartBody(fields: fields, fileField: fileField, fileData: fileData, fileName: fileName, mimeType: mimeType)
        return try await send(path: path, method: "POST", query: [:], body: body, contentType: contentType)
    }

    func patchMultipart<T: Decodable>(
        _ path: String,
        fields: [String: String] = [:],
        fileField: String? = nil,
        fileData: Data? = nil,
        fileName: String? = nil,
        mimeType: String? = nil
    ) async throws -> T {
        let (body, contentType) = buildMultipartBody(fields: fields, fileField: fileField, fileData: fileData, fileName: fileName, mimeType: mimeType)
        return try await send(path: path, method: "PATCH", query: [:], body: body, contentType: contentType)
    }

    private func buildMultipartBody(
        fields: [String: String],
        fileField: String?,
        fileData: Data?,
        fileName: String?,
        mimeType: String?
    ) -> (Data, String) {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        for (key, value) in fields {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(value)\r\n".data(using: .utf8)!)
        }
        if let fileField, let fileData, let fileName {
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"\(fileField)\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: \(mimeType ?? "application/octet-stream")\r\n\r\n".data(using: .utf8)!)
            body.append(fileData)
            body.append("\r\n".data(using: .utf8)!)
        }
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        return (body, "multipart/form-data; boundary=\(boundary)")
    }

    // MARK: - Core

    private func send<T: Decodable>(
        path: String,
        method: String,
        query: [String: String],
        body: Data?,
        contentType: String? = nil
    ) async throws -> T {
        let isAuthRoute = path.hasPrefix("auth/")
        let request = try buildRequest(path: path, method: method, query: query, body: body, contentType: contentType, token: isAuthRoute ? nil : tokenStore.accessToken)

        let (data, response) = try await session.data(for: request)
        let http = response as? HTTPURLResponse

        if http?.statusCode == 401, !isAuthRoute, tokenStore.refreshToken != nil {
            try await refreshTokenIfNeeded()
            let retried = try buildRequest(path: path, method: method, query: query, body: body, contentType: contentType, token: tokenStore.accessToken)
            let (retryData, retryResponse) = try await session.data(for: retried)
            return try decode(retryData, retryResponse)
        }

        return try decode(data, response)
    }

    private func refreshTokenIfNeeded() async throws {
        if let existing = refreshTask {
            _ = try await existing.value
            return
        }
        let task = Task<Void, Error> {
            guard let refreshToken = tokenStore.refreshToken else { return }
            let body = try encoder.encode(RefreshRequest(refreshToken: refreshToken))
            let request = try buildRequest(path: "auth/refresh", method: "POST", query: [:], body: body, contentType: nil, token: nil)
            let (data, response) = try await session.data(for: request)
            let auth: AuthResponse = try decode(data, response)
            tokenStore.save(accessToken: auth.accessToken, refreshToken: auth.refreshToken, user: auth.user)
        }
        refreshTask = task
        defer { refreshTask = nil }
        _ = try await task.value
    }

    private func buildRequest(path: String, method: String, query: [String: String], body: Data?, contentType: String?, token: String?) throws -> URLRequest {
        var url = NetworkConfig.baseURL.appendingPathComponent(path)
        if !query.isEmpty {
            var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
            components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
            url = components.url!
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            request.httpBody = body
            request.setValue(contentType ?? "application/json", forHTTPHeaderField: "Content-Type")
        }
        return request
    }

    private func decode<T: Decodable>(_ data: Data, _ response: URLResponse) throws -> T {
        guard let http = response as? HTTPURLResponse else {
            throw APIError(statusCode: -1, message: "No HTTP response")
        }
        guard (200...299).contains(http.statusCode) else {
            let message = (try? decoder.decode(ServerErrorBody.self, from: data).message) ?? "Request failed (\(http.statusCode))"
            throw APIError(statusCode: http.statusCode, message: message)
        }
        if T.self == EmptyResponse.self { return EmptyResponse() as! T }
        return try decoder.decode(T.self, from: data)
    }
}

struct EmptyResponse: Decodable {}
private struct ServerErrorBody: Decodable { let message: String }
