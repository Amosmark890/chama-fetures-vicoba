package com.ekenya.chamakyc.service.Interfaces;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileHandlerService {
    String getFileUrl(String fileName);

    Resource downloadFile(String fileName);

    String uploadFile(FilePart filePart);

    Mono<ResponseEntity<?>> downloadFileFromUrl(String filename);
}
