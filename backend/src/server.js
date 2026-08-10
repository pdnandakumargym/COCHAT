const http = require('http');
const env = require('./config/env');
const connectDB = require('./config/db');
const createApp = require('./app');
const createSocketServer = require('./realtime/socket');
const chats = require('./modules/chats');
const groups = require('./modules/groups');
const notifications = require('./modules/notifications');

async function main() {
  await connectDB();

  const app = createApp();
  const httpServer = http.createServer(app);
  const io = createSocketServer(httpServer);

  // give REST modules access to the socket server for real-time fan-out
  chats.attachIO(io);
  groups.attachIO(io);
  notifications.attachIO(io);

  httpServer.listen(env.port, () => {
    console.log(`[server] listening on http://localhost:${env.port} (${env.nodeEnv})`);
  });
}

main().catch((err) => {
  console.error('[server] failed to start', err);
  process.exit(1);
});
