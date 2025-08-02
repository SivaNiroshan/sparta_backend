package com.Sparta.UploadService.Rabbit;

import com.Sparta.UploadService.Dash.VideoSplit;
import com.Sparta.UploadService.Encoding.Implementation.AvEncoding;
import com.Sparta.UploadService.MongoDB.MetaService;
import com.Sparta.UploadService.TusServer.Helpers.DeleteHandler;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
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



    @RabbitListener(queues = RabbitMQConfig.ENCODING_QUEUE)
    public void receiveJob(EncodingJobDTO job) {
        System.out.println("Received job: " + job.getInputPath());
        try {
            List<Integer> encodedQualities=avEncoding.encode(job.getInputPath(), job.getOutputPath());
            deleteHandler.deleteFile(Path.of(job.getInputPath())); // Use DeleteHandler for cleanup
            MetaRequest file=job.getFile();
            System.out.println(" Encoding complete: " );
            video_split.packageToDash(job.getInputPath(), encodedQualities);
            if(file != null) {
                file.setQualities(encodedQualities);
                meta_service.saveMeta(file); // Save metadata after encoding
                System.out.println(" Metadata saved for: " );
            } else {
                System.out.println(" No metadata to save for: " );
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(" Encoding failed for: " + job.getInputPath());
        }
    }
}

