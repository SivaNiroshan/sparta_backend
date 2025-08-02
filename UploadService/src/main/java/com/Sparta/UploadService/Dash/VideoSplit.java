package com.Sparta.UploadService.Dash;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

@Component
public class VideoSplit {
    public void packageToDash(String outputBasePath, List<Integer> encodedQualities) throws IOException {
        String packagerPath = this.GetPathDASH();
        System.out.println(outputBasePath);

        // Get only the file name from the given base path (e.g., SooraraiPottru_Aagasam_1080p.mp4)
        String fileName = new File(outputBasePath).getName();

        // Build the base directory prefix
        String prefixPath = "C:\\Users\\THABENDRA\\Desktop\\sparta_backend\\";

        // Now recreate full path for audio and video inputs
        String audioInput = prefixPath + fileName.replace(".mp4", "_encoded_" + encodedQualities.get(0) + "p.mp4");
        System.out.println("Audio Input"+audioInput);
        String outputDir = prefixPath+"storage\\"+ "dash_output";
        System.out.println("Output Directory :"+outputDir);
        new File(outputDir).mkdirs(); // create output folder

        StringBuilder command = new StringBuilder("\"" + packagerPath + "\"");

        // Add video inputs
        for (int quality : encodedQualities) {
            String videoInput = prefixPath + fileName.replace(".mp4", "_encoded_" + quality + "p.mp4");
            System.out.println("Video input  PAth"+videoInput);
            String videoOut = outputDir + File.separator + "video_" + quality + "p_dash.mp4";
            System.out.println("Video Output path"+videoOut);
            command.append(" input=\"").append(videoInput).append("\",stream=video,output=\"").append(videoOut).append("\"");
            System.out.println("Command0 for visual frame"+command);
        }

        // Add audio input (assumed to be from the highest quality)
        String audioOut = outputDir + File.separator + "audio.mp4";
        System.out.println("Audio Output"+audioOut);
        command.append(" input=\"").append(audioInput).append("\",stream=audio,output=\"").append(audioOut).append("\"");
        System.out.println("Command1 for audio "+command);
        // Append MPD output
        command.append(" --segment_duration 2 --mpd_output=\"").append(outputDir).append(File.separator).append("manifest.mpd").append("\"");
        System.out.println("Command2 for segment the video"+command);
        System.out.println("Running DASH Packager:\n" + command);
        Path tempScript = Files.createTempFile("dash_packager_", ".bat");
        Files.write(tempScript, command.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",tempScript.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Output logs
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Packager] " + line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Packager exited with code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Packaging interrupted", e);
        }

        System.out.println("DASH packaging completed: " + tempScript.toString());
    }

    private String GetPathDASH(){
        String os = System.getProperty("os.name").toLowerCase();
        String executableName = os.contains("win") ? "packager.exe" : "packager";

        Path packagerPath = Paths.get("UploadService", "bin", "Dash-executable", os.contains("win") ? "windows" : "linux", executableName)
                .normalize()
                .toAbsolutePath();

        if (!packagerPath.toFile().exists()) {
            throw new IllegalStateException("DASH Packager binary not found at: " + packagerPath);
        }
        System.out.println("DASH Packager Path: " + packagerPath);

        return packagerPath.toString();

    }

}
