import Foundation

final class GroupsRepository {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func create(name: String, memberIds: [String]) async throws -> GroupInfo {
        let res: GroupEnvelope = try await api.post("groups", body: CreateGroupRequest(name: name, memberIds: memberIds))
        return res.group
    }

    func get(id: String) async throws -> GroupInfo {
        let res: GroupEnvelope = try await api.get("groups/\(id)")
        return res.group
    }

    func updateName(id: String, name: String) async throws -> GroupInfo {
        let res: GroupEnvelope = try await api.patchMultipart("groups/\(id)", fields: ["name": name])
        return res.group
    }

    func updateAvatar(id: String, data: Data, fileName: String, mimeType: String) async throws -> GroupInfo {
        let res: GroupEnvelope = try await api.patchMultipart(
            "groups/\(id)", fileField: "avatar", fileData: data, fileName: fileName, mimeType: mimeType
        )
        return res.group
    }

    func addMembers(id: String, memberIds: [String]) async throws -> GroupInfo {
        let res: GroupEnvelope = try await api.post("groups/\(id)/members", body: AddMembersRequest(memberIds: memberIds))
        return res.group
    }

    func removeMember(id: String, userId: String) async throws {
        try await api.deleteVoid("groups/\(id)/members/\(userId)")
    }

    func leave(id: String) async throws {
        try await api.postVoid("groups/\(id)/leave")
    }
}
