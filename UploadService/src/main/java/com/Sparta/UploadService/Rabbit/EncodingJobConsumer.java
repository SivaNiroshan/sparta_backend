package com.Sparta.UploadService.Rabbit;

import com.Sparta.UploadService.Encoding.Implementation.AvEncoding;
import com.Sparta.UploadService.TusServer.Helpers.DeleteHandler;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class EncodingJobConsumer {

    @Autowired
    private DeleteHandler deleteHandler;
    @Autowired
    private  AvEncoding avEncoding;



    @RabbitListener(queues = RabbitMQConfig.ENCODING_QUEUE)
    public void receiveJob(EncodingJobDTO job) {
        System.out.println("Received job: " + job.getInputPath());
        try {
            avEncoding.encode(job.getInputPath(), job.getOutputPath());
            deleteHandler.deleteFile(Path.of(job.getInputPath())); // Use DeleteHandler for cleanup
            MetaRequest file=job.getFile();
            System.out.println(" Encoding complete: " + job.getOutputPath());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(" Encoding failed for: " + job.getInputPath());
        }
    }
}

