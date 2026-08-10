const mongoose = require('mongoose');
const env = require('./env');

function redactCredentials(uri) {
  return uri.replace(/\/\/([^:/?#]+):([^@]+)@/, '//$1:****@');
}

async function connectDB() {
  mongoose.set('strictQuery', true);
  await mongoose.connect(env.mongoUri);
  console.log(`[db] connected to ${redactCredentials(env.mongoUri)}`);
}

module.exports = connectDB;
