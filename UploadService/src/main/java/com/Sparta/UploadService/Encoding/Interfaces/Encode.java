package com.Sparta.UploadService.Encoding.Interfaces;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Encode {
    void encode(String inputPath, String outputPath) throws Exception;

    default String getFfmpegPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";

        // Use relative path to the ffmpeg executable
        Path ffmpegPath = Paths.get("UploadService", "bin", "ffmpeg-build", "bin", executableName)
                .normalize()
                .toAbsolutePath();
        if (!ffmpegPath.toFile().exists()) {
            throw new IllegalStateException("FFmpeg binary not found at: " + ffmpegPath);
        }


        return ffmpegPath.toString();
    }
}
