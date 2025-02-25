const { onValueCreated } = require('firebase-functions/v2/database');
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

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

    // Lấy tên người dùng từ senderId
    const senderSnapshot = await admin.database().ref(`/users/${senderId}/username`).once('value');
    const senderUsername = senderSnapshot.val() || 'Unknown';

    const tokens = [];
    for (const recipientId of recipientIds) {
        const userSnapshot = await admin.database().ref(`/users/${recipientId}/fcmToken`).once('value');
        const token = userSnapshot.val();
        if (token) {
            tokens.push(token);
        } else {
            functions.logger.warn(`No FCM token for user: ${recipientId}`);
        }
    }

    functions.logger.log('Recipient IDs:', recipientIds);
    functions.logger.log('Fetched tokens:', tokens);
    if (tokens.length === 0) {
        functions.logger.log('No valid recipients found');
        return null;
    }

    const truncatedBody = message.content && message.content.length > 50
        ? message.content.substring(0, 47) + "..."
        : (message.content || "New message");

    const sendPromises = tokens.map(token => {
        const payload = {
            token: token,
            notification: {
                title: senderUsername,
                body: truncatedBody
            },
            data: {
                conversationId: conversationId,
                messageId: event.params.messageId,
                senderUsername: senderUsername
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