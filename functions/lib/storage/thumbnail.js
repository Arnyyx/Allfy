const { onObjectFinalized } = require('firebase-functions/v2/storage');
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { spawn } = require('child-process-promise');
const path = require('path');
const os = require('os');
const fs = require('fs');
const ffmpegPath = require('@ffmpeg-installer/ffmpeg').path;

exports.generateVideoThumbnail = onObjectFinalized({
    region: 'asia-southeast1'
}, async (event) => {
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
});