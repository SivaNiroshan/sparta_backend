package com.Sparta.UploadService.TusServer.Helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;


@Component
public class DeleteHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteHandler.class);

    public void handleFailure(Thread processingThread,
                              Thread writerThread,
                              byte[] bufferA,
                              byte[] bufferB,
                              Path filePath,
                              BlockingQueue<byte[]> fullBuffers,
                              BlockingQueue<byte[]> emptyBuffers) {

        safeInterrupt(processingThread, "Processing thread");
        safeInterrupt(writerThread, "Writer thread");

        clearBuffer(bufferA);
        clearBuffer(bufferB);

        fullBuffers.clear();
        emptyBuffers.clear();

        deleteFile(filePath);
    }

    private void safeInterrupt(Thread thread, String threadName) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            logger.info("[DeleteHandler] {} interrupted.", threadName);
        }
    }

    private void clearBuffer(byte[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    private void deleteFile(Path filePath) {
        try {
            if (filePath != null && Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("[DeleteHandler] File deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("[DeleteHandler] Failed to delete file: {}", filePath, e);
        }
    }
}
