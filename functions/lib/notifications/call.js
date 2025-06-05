const { onValueCreated } = require('firebase-functions/v2/database');
const functions = require('firebase-functions');
const admin = require('firebase-admin');

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