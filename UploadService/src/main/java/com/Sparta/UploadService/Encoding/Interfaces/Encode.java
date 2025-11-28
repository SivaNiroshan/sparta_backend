package com.Sparta.UploadService.Encoding.Interfaces;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public interface Encode {
    List<Integer> encode(String inputPath, String outputPath) throws Exception;

    default String getFfmpegPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";

        // Try path relative to current working directory (assuming we're in UploadService)
        Path ffmpegPath = Paths.get("bin", "ffmpeg-build", "bin", executableName)
                .normalize()
                .toAbsolutePath();
        
        // If not found, try with UploadService prefix (in case we're in parent directory)
        if (!ffmpegPath.toFile().exists()) {
            ffmpegPath = Paths.get("UploadService", "bin", "ffmpeg-build", "bin", executableName)
                    .normalize()
                    .toAbsolutePath();
        }
        
        if (!ffmpegPath.toFile().exists()) {
            throw new IllegalStateException("FFmpeg binary not found at: " + ffmpegPath);
        }

        return ffmpegPath.toString();
    }
}
