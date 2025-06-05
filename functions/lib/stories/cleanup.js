const { onSchedule } = require('firebase-functions/v2/scheduler');
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.deleteExpiredStories = onSchedule({
    schedule: 'every 60 minutes', // Chạy mỗi giờ
    region: 'asia-southeast1',
    timeoutSeconds: 300
}, async () => {
    functions.logger.log('Starting deletion of expired stories');
    try {
        const currentTime = Date.now();
        const storiesRef = admin.firestore().collection('stories');
        const snapshot = await storiesRef.get();

        if (snapshot.empty) {
            functions.logger.log('No stories found');
            return null;
        }

        const batch = admin.firestore().batch();
        const deletePromises = [];

        for (const doc of snapshot.docs) {
            const story = doc.data();
            const expiryTime = story.timestamp.toMillis() + (story.duration * 1000);

            if (currentTime > expiryTime) {
                batch.delete(doc.ref);

                const userStoryRef = admin.firestore()
                    .collection('users')
                    .doc(story.userID)
                    .collection('stories')
                    .doc(story.storyID);
                batch.delete(userStoryRef);

                // Delete media from Storage
                const storagePath = `stories/${story.userID}/${story.storyID}`;
                const bucket = admin.storage().bucket();
                deletePromises.push(bucket.deleteFiles({ prefix: storagePath }));

                functions.logger.log(`Marked story ${story.storyID} for deletion`);
            }
        }

        // Commit Firestore batch
        await batch.commit();
        functions.logger.log('Firestore batch deletion completed');

        // Execute Storage deletions
        await Promise.all(deletePromises);
        functions.logger.log('Storage files deletion completed');

        return null;
    } catch (error) {
        functions.logger.error('Error deleting expired stories:', error);
        throw error;
    }
});