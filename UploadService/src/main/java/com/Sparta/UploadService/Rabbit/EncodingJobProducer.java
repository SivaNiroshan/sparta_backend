package com.Sparta.UploadService.Rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EncodingJobProducer {

    private final RabbitTemplate rabbitTemplate;

    public EncodingJobProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendJob(EncodingJobDTO job) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.ENCODING_QUEUE, job);
        System.out.println(" Queued encoding job for: " + job.getInputPath());
    }
}
