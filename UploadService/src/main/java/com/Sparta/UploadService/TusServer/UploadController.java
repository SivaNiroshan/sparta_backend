package com.Sparta.UploadService.TusServer;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import com.Sparta.UploadService.TusServer.model.TusFile;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/upload")
public class UploadController {

    private final Path storageDir;
    private final Map<String, TusFile> fileMap = new HashMap<>();

    private boolean paused = false;



    @Autowired
    public UploadController(Environment env) {
        this.storageDir = Paths.get(env.getProperty("tusserver.storagefolder", "uploads"));
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage folder", e);
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> processOptions() {
        return ResponseEntity.status(204)
                .headers(setTusHeaders())
                .build();
    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = "/{uuid}")
    public ResponseEntity<Void> processOptionsUuid(@PathVariable String uuid) {
        TusFile file = fileMap.get(uuid);
        if (file == null) return ResponseEntity.notFound().build();

        HttpHeaders headers = setTusHeaders();
        headers.set("Upload-Offset", String.valueOf(file.getOffset()));
        return ResponseEntity.status(204).headers(headers).build();
    }

    @PostMapping
    public ResponseEntity<Void> processPost(
            @RequestHeader("Upload-Length") Integer uploadLength,
            UriComponentsBuilder uriBuilder,
            @RequestBody MetaRequest metaRequest // get metadata from request body
    ) {
        if (uploadLength == null || uploadLength < 1)
            return ResponseEntity.badRequest().build();

        UUID uuid = UUID.randomUUID();
        TusFile file = new TusFile(uuid, uploadLength);

        // Set metadata
        if (metaRequest != null) {
            file.setName(metaRequest.getName());
            file.setDescription(metaRequest.getDescription());
            file.setDistributor(metaRequest.getDistributor());
            file.setTimeline(metaRequest.getTimeline());
        }

        fileMap.put(String.valueOf(uuid), file);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Expose-Headers", "Location, Tus-Resumable");
        headers.set("Location", uuid.toString());
        headers.set("Tus-Resumable", "1.0.0");
        System.out.println(uuid.toString());

        return ResponseEntity.status(201).headers(headers).build();
    }


    @RequestMapping(method = RequestMethod.HEAD, value = "/{uuid}")
    public ResponseEntity<Void> processHead(@PathVariable String uuid) {
        TusFile file = fileMap.get(uuid);
        if (file == null) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Expose-Headers", "Upload-Offset, Upload-Length, Tus-Resumable");
        headers.set("Upload-Offset", String.valueOf(file.getOffset()));
        headers.set("Upload-Length", String.valueOf(file.getUploadLength()));
        headers.set("Tus-Resumable", "1.0.0");

        return ResponseEntity.ok().headers(headers).build();
    }



    @RequestMapping(method = RequestMethod.PATCH, value = "/{uuid}")
    public ResponseEntity<?> processPatch(@PathVariable String uuid,
                                          @RequestHeader("Upload-Offset") Integer offset,
                                          @RequestHeader("Content-Length") Integer contentLength,
                                          @RequestHeader("Content-Type") String contentType,
                                          @RequestHeader(value = "Resumed-Status", required = false) Boolean resumedStatus,
                                          InputStream inputStream) throws IOException, InterruptedException {

        if (!"application/offset+octet-stream".equals(contentType)) {
            return ResponseEntity.badRequest().body("Invalid Content-Type");
        }

        TusFile file = fileMap.get(uuid);
        if (file == null) return ResponseEntity.notFound().build();

        if (!offset.equals(file.getOffset())) {
            return ResponseEntity.status(409).body("Mismatched offset");
        }

        Path filePath = storageDir.resolve(file.getName());

        final int BUFFER_SIZE = 2 * 1024 * 1024;

        // Create exactly two reusable buffers
        byte[] bufferA = new byte[BUFFER_SIZE];
        byte[] bufferB = new byte[BUFFER_SIZE];

        // Queues to manage buffers
        final BlockingQueue<byte[]> fullBuffers = new LinkedBlockingQueue<>();
        final BlockingQueue<byte[]> emptyBuffers = new LinkedBlockingQueue<>();

        // Add the two buffers to the pool initially
        emptyBuffers.put(bufferA);
        emptyBuffers.put(bufferB);

        // Writer thread
        Thread writerThread = new Thread(() -> {
            try (OutputStream out = new BufferedOutputStream(
                    Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

                while (true) {
                    byte[] buffer = fullBuffers.take();
                    if (buffer.length == 0) break; // poison pill

                    out.write(buffer, 0, buffer.length);

                    // Reuse buffer
                    Arrays.fill(buffer, (byte) 0);
                    emptyBuffers.put(buffer);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error writing to file", e);
            }
        });

        writerThread.start();

        int totalBytes = 0;

        try {
            while (true) {
                byte[] buffer = emptyBuffers.take(); // get reusable buffer
                int read = inputStream.read(buffer);

                if (read == -1) break;

                totalBytes += read;

                byte[] exact = new byte[read];
                System.arraycopy(buffer, 0, exact, 0, read);
                fullBuffers.put(exact);

            }

            fullBuffers.put(new byte[0]); // poison pill
            writerThread.join(); // wait for writer to finish

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Thread interrupted");
        }

        file.setOffset(file.getOffset() + totalBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Upload-Offset", String.valueOf(file.getOffset()));
        headers.set("Tus-Resumable", "1.0.0");
        headers.set("Access-Control-Expose-Headers", "Upload-Offset, Tus-Resumable");

        return ResponseEntity.noContent().headers(headers).build();
    }



    @GetMapping("/{uuid}")
    public ResponseEntity<Resource> download(@PathVariable String uuid) throws IOException {
        TusFile file = fileMap.get(uuid);
        Path path = storageDir.resolve(uuid);

        if (file == null || !Files.exists(path)) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uuid + "\"")
                .body(resource);
    }

    private HttpHeaders setTusHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Expose-Headers", "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension");
        headers.set("Tus-Resumable", "1.0.0");
        headers.set("Tus-Version", "1.0.0,0.2.2,0.2.1");
        headers.set("Tus-Max-Size", "10737418240");
        headers.set("Tus-Extension", "creation,expiration");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH,OPTIONS");
        return headers;
    }


}
