package com.Sparta.UploadService.S3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String basePath;

    public S3Service(S3Client s3Client,
                     @Value("${aws.s3.bucket-name}") String bucketName,
                     @Value("${aws.s3.base-path:videos}") String basePath) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.basePath = basePath;
    }

    /**
     * Upload DASH files to S3
     * Structure: videos/{video-id}/manifest.mpd, videos/{video-id}/video/, videos/{video-id}/audio/
     *
     * @param videoId          Unique video identifier
     * @param dashOutputDir    Local directory containing DASH files
     * @param encodedQualities List of encoded quality heights
     * @return S3 base URL for the uploaded video
     */
    public String uploadDashFilesToS3(String videoId, String dashOutputDir, List<Integer> encodedQualities) {
        logger.info("Starting S3 upload for video: {} from directory: {}", videoId, dashOutputDir);

        try {
            Path outputPath = Paths.get(dashOutputDir);
            if (!Files.exists(outputPath)) {
                throw new IOException("DASH output directory does not exist: " + dashOutputDir);
            }

            // Upload manifest.mpd
            Path manifestPath = outputPath.resolve("manifest.mpd");
            if (Files.exists(manifestPath)) {
                String manifestKey = String.format("%s/%s/manifest.mpd", basePath, videoId);
                uploadFile(manifestPath.toFile(), manifestKey, "application/dash+xml");
                logger.info("Uploaded manifest.mpd to S3: {}", manifestKey);
            } else {
                logger.warn("Manifest file not found: {}", manifestPath);
            }

            // Get base file name from output directory
            String baseFileName = getBaseFileName(dashOutputDir);
            
            // Upload video files
            for (Integer quality : encodedQualities) {
                // File naming matches VideoSplit: {fileNameNoExt}video_{quality}p_dash.mp4
                String videoFileName = baseFileName + "video_" + quality + "p_dash.mp4";
                Path videoPath = outputPath.resolve(videoFileName);
                
                if (Files.exists(videoPath)) {
                    String videoKey = String.format("%s/%s/video/%s", basePath, videoId, videoFileName);
                    uploadFile(videoPath.toFile(), videoKey, "video/mp4");
                    logger.info("Uploaded video file to S3: {} (quality: {}p)", videoKey, quality);
                } else {
                    logger.warn("Video file not found: {}", videoPath);
                }
            }

            // Upload audio file
            // File naming matches VideoSplit: {fileNameNoExt}audio.mp4
            String audioFileName = baseFileName + "audio.mp4";
            Path audioPath = outputPath.resolve(audioFileName);
            
            if (Files.exists(audioPath)) {
                String audioKey = String.format("%s/%s/audio/%s", basePath, videoId, audioPath.getFileName().toString());
                uploadFile(audioPath.toFile(), audioKey, "audio/mp4");
                logger.info("Uploaded audio file to S3: {}", audioKey);
            } else {
                logger.warn("Audio file not found: {}", audioPath);
            }

            String s3BaseUrl = String.format("s3://%s/%s/%s", bucketName, basePath, videoId);
            logger.info("Successfully uploaded all DASH files to S3. Base URL: {}", s3BaseUrl);
            return s3BaseUrl;

        } catch (Exception e) {
            logger.error("Failed to upload DASH files to S3 for video: {}", videoId, e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    /**
     * Upload a single file to S3
     */
    private void uploadFile(File file, String s3Key, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            RequestBody requestBody = RequestBody.fromFile(file);
            s3Client.putObject(putObjectRequest, requestBody);

            logger.debug("Successfully uploaded file to S3: {} (size: {} bytes)", s3Key, file.length());

        } catch (S3Exception e) {
            logger.error("S3 error uploading file: {}", s3Key, e);
            throw new RuntimeException("Failed to upload file to S3: " + s3Key, e);
        } catch (Exception e) {
            logger.error("Error uploading file to S3: {}", s3Key, e);
            throw new RuntimeException("Failed to upload file to S3: " + s3Key, e);
        }
    }

    /**
     * Extract base file name from DASH output directory path
     * Example: "dash_outputSooraraiPottru_Aagasam_1080p" -> "SooraraiPottru_Aagasam_1080p"
     */
    private String getBaseFileName(String dashOutputDir) {
        Path path = Paths.get(dashOutputDir);
        String dirName = path.getFileName().toString();
        
        // Remove "dash_output" prefix if present
        if (dirName.startsWith("dash_output")) {
            return dirName.substring("dash_output".length());
        }
        return dirName;
    }

    /**
     * Delete a file from S3
     */
    public void deleteFile(String s3Key) {
        try {
            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());
            logger.info("Deleted file from S3: {}", s3Key);
        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}", s3Key, e);
        }
    }
}

