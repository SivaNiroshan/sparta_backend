package com.Sparta.UploadService.Rabbit;

import com.Sparta.UploadService.Dash.VideoSplit;
import com.Sparta.UploadService.Encoding.Implementation.AvEncoding;
import com.Sparta.UploadService.MongoDB.MetaService;
import com.Sparta.UploadService.S3.S3Service;
import com.Sparta.UploadService.TusServer.Helpers.DeleteHandler;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class EncodingJobConsumer {

    @Autowired
    private DeleteHandler deleteHandler;
    @Autowired
    private  AvEncoding avEncoding;

    @Autowired
    private MetaService meta_service;
    @Autowired
    private VideoSplit video_split;
    @Autowired
    private S3Service s3Service;



    @RabbitListener(queues = RabbitMQConfig.ENCODING_QUEUE)
    public void receiveJob(EncodingJobDTO job) {
        System.out.println("Received job: " + job.getInputPath());
        
        // Track files to delete
        List<String> encodedFilePaths = new ArrayList<>();
        String dashOutputDir = null;
        
        try {
            // Step 1: Encode video
            List<Integer> encodedQualities = avEncoding.encode(job.getInputPath(), job.getOutputPath());
            
            // Track encoded file paths for cleanup
            // The outputPath from job is like "name_encoded.mp4", and AvEncoding creates "name_encoded_1080p.mp4", etc.
            String outputBasePath = job.getOutputPath();
            String prefixPath = "C:\\Users\\THABENDRA\\Desktop\\sparta_backend\\";
            String uploadServicePath = prefixPath + "UploadService\\";
            
            for (Integer quality : encodedQualities) {
                // AvEncoding creates files like: {outputBasePath}.replace(".mp4", "_1080p.mp4")
                // So if outputBasePath is "name_encoded.mp4", it becomes "name_encoded_1080p.mp4"
                String encodedFileName = outputBasePath.replace(".mp4", "_" + quality + "p.mp4");
                String encodedFilePath = uploadServicePath + encodedFileName;
                encodedFilePaths.add(encodedFilePath);
            }
            
            // Delete original uploaded file (no longer needed after encoding)
            deleteHandler.deleteFile(Path.of(job.getInputPath()));
            System.out.println("Original file deleted: " + job.getInputPath());
            
            MetaRequest file = job.getFile();
            System.out.println(" ************************************************Encoding completed **************************************** " );
            System.out.println("file path :" + job.getInputPath());
            
            // Step 2: Package to DASH
            System.out.println("************************************************Dash packaging started *************************************************");
            dashOutputDir = video_split.packageToDash(job.getInputPath(), encodedQualities);
            System.out.println("************************************************DASH packaging completed *************************************************");
            
            // Step 3: Delete encoded files (no longer needed after DASH packaging)
            System.out.println("************************************************Cleaning up encoded files *************************************************");
            for (String encodedFilePath : encodedFilePaths) {
                deleteHandler.deleteFile(Path.of(encodedFilePath));
                System.out.println("Deleted encoded file: " + encodedFilePath);
            }
            System.out.println("************************************************Encoded files cleanup completed *************************************************");
            
            // Step 4: Generate video ID
            String videoId = generateVideoId(file);
            
            // Step 5: Upload DASH files to S3
            System.out.println("************************************************S3 upload started *************************************************");
            String s3BaseUrl = s3Service.uploadDashFilesToS3(videoId, dashOutputDir, encodedQualities);
            System.out.println("Successfully uploaded to S3: " + s3BaseUrl);
            System.out.println("************************************************S3 upload completed *************************************************");
            
            // Step 6: Delete DASH output directory (no longer needed after S3 upload)
            System.out.println("************************************************Cleaning up DASH output directory *************************************************");
            deleteHandler.deleteDirectory(dashOutputDir);
            System.out.println("Deleted DASH output directory: " + dashOutputDir);
            System.out.println("************************************************DASH directory cleanup completed *************************************************");
            
            // Step 7: Save metadata
            if(file != null) {
                file.setQualities(encodedQualities);
                meta_service.saveMeta(file);
                System.out.println(" Metadata saved for: " + videoId);
                System.out.println("************************************************Metadata saved *************************************************");
            } else {
                System.out.println(" No metadata to save for: " + videoId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(" Encoding failed for: " + job.getInputPath());
            System.out.println("************************************************Encoding failed *************************************************");
            
            // Cleanup on failure - try to delete what we can
            try {
                // Delete encoded files if they exist
                for (String encodedFilePath : encodedFilePaths) {
                    deleteHandler.deleteFile(Path.of(encodedFilePath));
                }
                // Delete DASH directory if it exists
                if (dashOutputDir != null) {
                    deleteHandler.deleteDirectory(dashOutputDir);
                }
            } catch (Exception cleanupException) {
                System.err.println("Cleanup after failure also failed: " + cleanupException.getMessage());
            }
        }
    }

    /**
     * Generate a unique video ID for S3 storage
     * Uses MongoDB ID if available, otherwise creates a slug from the name
     */
    private String generateVideoId(MetaRequest file) {
        if (file != null && file.getId() != null && !file.getId().isEmpty()) {
            return file.getId();
        }
        
        if (file != null && file.getName() != null) {
            // Create a slug from the name
            String name = file.getName();
            // Remove extension
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                name = name.substring(0, lastDot);
            }
            // Replace spaces and special characters with hyphens, convert to lowercase
            return name.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
        }
        
        // Fallback: use timestamp
        return "video-" + System.currentTimeMillis();
    }
}

