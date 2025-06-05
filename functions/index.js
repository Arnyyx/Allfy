const functions = require('firebase-functions');
const admin = require('firebase-admin');

if (!admin.apps.length) {
    admin.initializeApp();
}

// Export all functions
exports.sendChatNotification = require('./lib/notifications/chat').sendChatNotification;
exports.sendCallNotification = require('./lib/notifications/call').sendCallNotification;
exports.generateVideoThumbnail = require('./lib/storage/thumbnail').generateVideoThumbnail;
exports.deleteExpiredStories = require('./lib/stories/cleanup').deleteExpiredStories;