package com.eclectics.chamapayments.resource.mobile;

import com.eclectics.chamapayments.service.FileHandlerService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Alex Maina
 * @created 03/01/2022
 */
@RestController
@RequestMapping("/api/v2/payment")
@RequiredArgsConstructor
public class FileResource {
    private final FileHandlerService fileHandlerService;

    @GetMapping("/files/download/{filename:.+}")
    public Mono<ResponseEntity<?>> downloadFile(@PathVariable String filename) {
        return fileHandlerService.downloadFileFromUrl(filename);
    }

    /**
     * Gets country flag.
     *
     * @param code the code
     * @return the country flag
     */
    @GetMapping("/country/flag/{code}")
    @ApiOperation(value = "Use the value `code` from `/req/countries` to retrieve country flag e.g. for Kenya use `KE`. Case does not matter.")
    public Mono<ResponseEntity<Resource>> getCountryFlag(@PathVariable String code) {
        String file = code.toLowerCase().concat(".png");
        Resource resource = new ClassPathResource("flags/" + file);
        if (resource.exists()) {
            // Try to determine file's content type
            return Mono.fromCallable(() -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/png"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                    .body(resource));
        } else {
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
}
