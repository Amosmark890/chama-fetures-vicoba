package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.FileHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {
    @Value("${file.server.host}")
    private String serverHost;
    private final String basePath = "chama_files";

    public Resource downloadFile(String fileName) {
        String path = Paths.get(basePath).resolve(fileName).toAbsolutePath().normalize().toString();
        return new FileSystemResource(path);
    }

    public String uploadFile(FilePart filePart) {
        String fileName = System.nanoTime() + "-" + filePart.filename();
        if (createFile(filePart, fileName)) {
            return fileName;
        }
        return "";
    }

    @Override
    public Mono<ResponseEntity<?>> downloadFileFromUrl(String filename) {
        return Mono.fromSupplier(() -> {
                    Resource resource = downloadFile(filename);
                    if (resource == null) {
                        return ResponseEntity.notFound().build();
                    } else if (Objects.requireNonNull(resource.getFilename()).endsWith("PNG") || Objects.requireNonNull(resource.getFilename()).endsWith("JPG")) {
                        MimeType mimeType = (resource.getFilename().endsWith("PNG")) ? MimeTypeUtils.IMAGE_PNG : MimeTypeUtils.IMAGE_JPEG;
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=\"" + filename + "\"")
                                .header(HttpHeaders.CONTENT_TYPE, String.valueOf(mimeType))
                                .body(resource);
                    } else if (Objects.requireNonNull(resource.getFilename()).endsWith("xlsx")) {
                        return ResponseEntity
                                .ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                .cacheControl(CacheControl.noCache())
                                .body(resource);
                    } else {
                        return ResponseEntity.ok().body(resource);
                    }
                })
                .publishOn(Schedulers.boundedElastic());
    }

    private boolean createFile(FilePart filePart, String fileName) {
        try {
            Path path = Files.createFile(Paths.get(basePath).resolve(fileName).toAbsolutePath().normalize());
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
            DataBufferUtils.write(filePart.content(), channel, 0)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> {
                        try {
                            channel.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).subscribe();
            return true;
        } catch (IOException e) {
            log.error("ERROR CreateFile: {}", e.getMessage());
        }
        return false;
    }

    //replace uri.getHost with public url for internally deployed apps
    @Override
    public String getFileUrl(String fileName) {
        if(fileName.isBlank()) return null;
        return serverHost + "/api/v2/payment/file/download/" + fileName;
    }

}
