package com.Sparta.UploadService.Encoding.Implementation;

import com.Sparta.UploadService.Encoding.Interfaces.Encode;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class AvEncoding implements Encode {

    @Override
    public void encode(String inputPath, String outputPath) throws Exception {
        File inputFile = new File(inputPath);
        long fileSizeMB = inputFile.length() / (1024 * 1024);

        // Auto-select CRF based on file size
        int crf = (fileSizeMB > 1000) ? 36 : (fileSizeMB > 500 ? 34 : 32);

        // Auto-detect resolution (optional enhancement)
        String resolution = getResolution(inputPath);
        int preset = 8; // configurable
        int audioBitrate = 64; // in kbps

        String ffmpegPath = getFfmpegPath();

        String command = String.format("\"%s\" -i \"%s\" -c:v libsvtav1 -crf %d -preset %d -c:a libopus -b:a %dk -ac 2 \"%s\"",
                ffmpegPath, inputPath, crf, preset, audioBitrate, outputPath);

        System.out.println("Running FFmpeg command:\n" + command);
        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);

        int exit = executor.execute(cmdLine);
        if (exit != 0) {
            throw new RuntimeException("AV1 encoding failed with exit code: " + exit);
        }

        System.out.println("Encoding completed successfully from " + inputPath + " to " + outputPath);
    }

    public  String replaceLastFfmpegWithFfprobe(String path) {
        String target = "ffmpeg";
        String replacement = "ffprobe";

        int lastIndex = path.lastIndexOf(target);
        if (lastIndex == -1) {
            return path; // no change if not found
        }
        String sts=path.substring(0, lastIndex) + replacement + path.substring(lastIndex + target.length());
        System.out.println(sts);
        return sts;
    }


    private String getResolution(String inputPath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                this.replaceLastFfmpegWithFfprobe(getFfmpegPath()),
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0:s=x",
                inputPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.readLine(); // format: 1280x720
        }
    }


}
