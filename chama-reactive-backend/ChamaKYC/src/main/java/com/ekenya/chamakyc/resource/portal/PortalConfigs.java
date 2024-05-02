package com.ekenya.chamakyc.resource.portal;

import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
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

import javax.validation.constraints.Pattern;

@RestController
@RequestMapping("/portal/kyc")
@RequiredArgsConstructor
public class PortalConfigs {

    private final ChamaGroupService chamaGroupService;
    /**
     * Gets country flag.
     *
     * @param code the code
     * @return the country flag
     */
    @GetMapping("/country/flag/{code}")
    @ApiOperation(value = "Use the value `code` from `/req/countries` to retrieve country flag e.g. for Kenya use `KE`. Case does not matter.")
    public Mono<ResponseEntity<Resource>> getCountryFlag(@PathVariable @Pattern(regexp = "[a-zA-Z]",message = "Code cannot have special characters and digits") String code) {
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

    @GetMapping("/countries")
    public Mono<ResponseEntity<?>> fetchCountries() {
        return chamaGroupService.findAllCountries()
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
