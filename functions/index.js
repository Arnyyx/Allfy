const { onValueCreated } = require('firebase-functions/v2/database');
const { onValueDeleted } = require('firebase-functions/v2/database');
const { onObjectFinalized } = require("firebase-functions/v2/storage");
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { onCall } = require('firebase-functions/v2/https');
const { spawn } = require("child-process-promise");
const path = require("path");
const os = require("os");
const fs = require("fs");
const ffmpegPath = require("@ffmpeg-installer/ffmpeg").path;

admin.initializeApp();
const firestore = admin.firestore();
const storage = admin.storage();

if (!admin.apps.length) {
    admin.initializeApp();
}

exports.sendChatNotification = onValueCreated({
    ref: '/conversations/{conversationId}/messages/{messageId}',
    region: 'asia-southeast1'
}, async (event) => {
    functions.logger.log('Function triggered for message:', event.params.messageId);
    const message = event.data.val();
    if (!message || !message.senderId) {
        functions.logger.error('Message or senderId is missing');
        return null;
    }

    const conversationId = event.params.conversationId;

    const conversationSnapshot = await admin.database().ref(`/conversations/${conversationId}`).once('value');
    if (!conversationSnapshot.exists()) {
        functions.logger.error('Conversation not found');
        return null;
    }

    const conversation = conversationSnapshot.val();
    const participants = conversation.participants || [];
    if (!Array.isArray(participants)) {
        functions.logger.error('Participants is not an array');
        return null;
    }

    const senderId = message.senderId;
    const recipientIds = participants.filter(id => id !== senderId);

    const senderDoc = await admin.firestore().collection('users').doc(senderId).get();
    const senderUsername = senderDoc.exists ? senderDoc.data().username || 'Unknown' : 'Unknown';

    const tokens = [];
    for (const recipientId of recipientIds) {
        const userDoc = await admin.firestore().collection('users').doc(recipientId).get();
        if (userDoc.exists) {
            const token = userDoc.data().fcmToken;
            if (token) tokens.push(token);
            else functions.logger.warn(`No FCM token for user: ${recipientId}`);
        } else {
            functions.logger.warn(`User document not found for: ${recipientId}`);
        }
    }

    functions.logger.log('Recipient IDs:', recipientIds);
    functions.logger.log('Fetched tokens:', tokens);
    if (tokens.length === 0) {
        functions.logger.log('No valid recipients found');
        return null;
    }

    const messageType = message.type || 'TEXT';
    let notificationBody;
    switch (messageType) {
        case 'IMAGE':
            notificationBody = `${senderUsername} đã gửi ảnh`;
            break;
        case 'VIDEO':
            notificationBody = `${senderUsername} đã gửi video`;
            break;
        case 'FILE':
            notificationBody = `${senderUsername} đã gửi tệp`;
            break;
        case 'VOICE':
            notificationBody = `${senderUsername} đã gửi tin nhắn thoại`;
            break;
        case 'TEXT':
        default:
            notificationBody = message.content && message.content.length > 50
                ? message.content.substring(0, 47) + "..."
                : (message.content || "New message");
            break;
    }

    const sendPromises = tokens.map(token => {
        const payload = {
            token: token,
            notification: {
                title: senderUsername,
                body: notificationBody
            },
            data: {
                type: "message",
                conversationId: conversationId,
                messageId: event.params.messageId,
                senderUsername: senderUsername,
                otherUserId: senderId,
                messageType: messageType
            }
        };

        return admin.messaging().send(payload)
            .then(() => functions.logger.log(`Notification sent to token: ${token}`))
            .catch(err => functions.logger.error(`Error sending to token ${token}:`, err));
    });

    try {
        await Promise.all(sendPromises);
        functions.logger.log('All notifications processed');
    } catch (err) {
        functions.logger.error('Error processing notifications:', err);
    }
});

exports.sendCallNotification = onValueCreated({
    ref: '/conversations/{conversationId}/calls/callerId',
    region: 'asia-southeast1'
}, async (event) => {
    functions.logger.log('Function triggered for call from callerId:', event.data.val());
    const callerId = event.data.val();
    const conversationId = event.params.conversationId;

    if (!callerId) {
        functions.logger.error('CallerId is missing');
        return null;
    }

    const conversationSnapshot = await admin.database()
        .ref(`/conversations/${conversationId}`)
        .once('value');

    if (!conversationSnapshot.exists()) {
        functions.logger.error('Conversation not found');
        return null;
    }

    const conversation = conversationSnapshot.val();
    const participants = conversation.participants || [];

    if (!Array.isArray(participants)) {
        functions.logger.error('Participants is not an array');
        return null;
    }

    const receiverIds = participants.filter(id => id !== callerId);

    if (receiverIds.length === 0) {
        functions.logger.log('No receivers found for conversation:', conversationId);
        return null;
    }

    const callerDoc = await admin.firestore().collection('users').doc(callerId).get();
    const callerUsername = callerDoc.exists ? callerDoc.data().username || 'Unknown' : 'Unknown';

    const tokens = [];
    for (const receiverId of receiverIds) {
        const userDoc = await admin.firestore().collection('users').doc(receiverId).get();
        if (userDoc.exists) {
            const token = userDoc.data().fcmToken;
            if (token) tokens.push(token);
            else functions.logger.warn(`No FCM token for user: ${receiverId}`);
        } else {
            functions.logger.warn(`User document not found for: ${receiverId}`);
        }
    }

    functions.logger.log('Receiver IDs:', receiverIds);
    functions.logger.log('Fetched tokens:', tokens);

    if (tokens.length === 0) {
        functions.logger.log('No valid FCM tokens found for receivers');
        return null;
    }

    const sendPromises = tokens.map(token => {
        const payload = {
            token: token,
            notification: {
                title: `${callerUsername} đang gọi`,
                body: 'Nhấn để tham gia cuộc gọi'
            },
            data: {
                type: 'call',
                callerId: callerId,
                conversationId: conversationId,
                callerUsername: callerUsername
            }
        };

        return admin.messaging().send(payload)
            .then(() => functions.logger.log(`Notification sent to token: ${token}`))
            .catch(err => functions.logger.error(`Error sending to token ${token}:`, err));
    });

    try {
        await Promise.all(sendPromises);
        functions.logger.log('All call notifications processed');
    } catch (err) {
        functions.logger.error('Error processing call notifications:', err);
    }

    return null;
});


exports.generateVideoThumbnail = onObjectFinalized(
  {
    region: "asia-southeast1",
  },
  async (event) => {
    const object = event.data;
    const fileBucket = object.bucket;
    const filePath = object.name;
    const contentType = object.contentType;

    if (!contentType || !contentType.startsWith("video/")) {
      functions.logger.log("Not a video file, exiting.");
      return null;
    }

    const fileName = path.basename(filePath);
    const fileDir = path.dirname(filePath);
    const bucket = admin.storage().bucket(fileBucket);
    const tempFilePath = path.join(os.tmpdir(), fileName);
    const thumbFileName = `thumb_${fileName.split(".")[0]}.jpg`;
    const tempThumbPath = path.join(os.tmpdir(), thumbFileName);
    const thumbStoragePath = path.join(fileDir, thumbFileName);

    try {
      await bucket.file(filePath).download({ destination: tempFilePath });
      functions.logger.log(`Video downloaded to ${tempFilePath}`);

      await spawn(ffmpegPath, [
        "-i",
        tempFilePath,
        "-ss",
        "0",
        "-frames:v",
        "1",
        "-q:v",
        "2",
        tempThumbPath,
        "-y",
      ]);
      functions.logger.log(`Thumbnail created at ${tempThumbPath}`);

      await bucket.upload(tempThumbPath, {
        destination: thumbStoragePath,
        metadata: { contentType: "image/jpeg" },
      });
      functions.logger.log(`Thumbnail uploaded to ${thumbStoragePath}`);

      const thumbFile = bucket.file(thumbStoragePath);
      let thumbUrl;
      try {
        const [signedUrl] = await thumbFile.getSignedUrl({
          action: "read",
          expires: "03-17-2099",
        });
        thumbUrl = signedUrl;
      } catch (urlError) {
        functions.logger.error("Failed to generate signed URL:", urlError);
        thumbUrl = `https://storage.googleapis.com/${fileBucket}/${thumbStoragePath}`;
      }

      const postId = fileDir.split("/")[1];
      const postRef = admin.firestore().collection("posts").doc(postId);
      const postDoc = await postRef.get();

      if (postDoc.exists) {
        const postData = postDoc.data();
        const updatedMediaItems = postData.mediaItems.map((item) =>
          item.url.includes(fileName) && item.mediaType === "video"
            ? { ...item, thumbnailUrl: thumbUrl }
            : item
        );

        await postRef.update({
          mediaItems: updatedMediaItems,
        });
        functions.logger.log(`Updated Post ${postId} with thumbnail URL: ${thumbUrl} for video ${fileName}`);
      } else {
        functions.logger.log(`Post ${postId} not found in Firestore`);
      }
    } catch (error) {
      functions.logger.error("Error in generateVideoThumbnail:", error);
      throw error;
    } finally {
      if (fs.existsSync(tempFilePath)) fs.unlinkSync(tempFilePath);
      if (fs.existsSync(tempThumbPath)) fs.unlinkSync(tempThumbPath);
    }

    return null;
  }
);