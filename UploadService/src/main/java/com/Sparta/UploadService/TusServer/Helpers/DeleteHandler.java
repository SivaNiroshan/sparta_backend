package com.Sparta.UploadService.TusServer.Helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeleteHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteHandler.class);

    // Track active threads and queues
    public final Map<String, UploadContext> contextMap = new ConcurrentHashMap<>();

    public void registerContext(String uuid, UploadContext ctx) {
        contextMap.put(uuid, ctx);
    }

    public void cancelUpload(String uuid) {
        UploadContext ctx = contextMap.remove(uuid);
        if (ctx == null) return;

        safeInterrupt(ctx.readerThread, "Reader thread");
        safeInterrupt(ctx.writerThread, "Writer thread");

        clearBuffer(ctx.bufferA);
        clearBuffer(ctx.bufferB);

        ctx.fullBuffers.clear();
        ctx.emptyBuffers.clear();

        deleteFile(ctx.filePath);
    }

    public void handleFailure(Thread readerThread,
                              Thread writerThread,
                              byte[] bufferA,
                              byte[] bufferB,
                              Path filePath,
                              BlockingQueue<byte[]> fullBuffers,
                              BlockingQueue<byte[]> emptyBuffers) {

        safeInterrupt(readerThread, "Reader thread");
        safeInterrupt(writerThread, "Writer thread");

        clearBuffer(bufferA);
        clearBuffer(bufferB);

        fullBuffers.clear();
        emptyBuffers.clear();

        deleteFile(filePath);
    }

    private void safeInterrupt(Thread thread, String name) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            logger.info("[DeleteHandler] {} interrupted", name);
        }
    }

    private void clearBuffer(byte[] buffer) {
        if (buffer != null) Arrays.fill(buffer, (byte) 0);
    }

    public void deleteFile(Path filePath) {
        try {
            if (filePath != null && Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("[DeleteHandler] File deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("[DeleteHandler] File deletion failed: {}", filePath, e);
        }
    }

    public static class UploadContext {
        public Thread readerThread;
        public Thread writerThread;
        public byte[] bufferA;
        public byte[] bufferB;
        public Path filePath;
        public BlockingQueue<byte[]> fullBuffers;
        public BlockingQueue<byte[]> emptyBuffers;
    }
}
