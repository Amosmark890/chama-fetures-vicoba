package com.ekenya.chamakyc.resource.portal;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.AccountNumberWrapper;
import com.ekenya.chamakyc.wrappers.request.ActivateGroupRequest;
import com.ekenya.chamakyc.wrappers.request.MessageTemplateWrapper;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

import static com.ekenya.chamakyc.util.StringConstantsUtil.EMPTY_OR_LOWER_CASE_MATCH;

/**
 * @author Alex Maina
 * @created 10/01/2022
 */
@Validated
@RequestMapping("/portal/kyc")
@RestController
@RequiredArgsConstructor
public class PortalGroupResource {
    private final ChamaGroupService chamaGroupService;

    @GetMapping("/all-groups")
    @ApiOperation(value = "Fetch an individual group or all groups", notes = "The group id is optional. If it is not available, the endpoint will fetch all groups")
    public Mono<ResponseEntity<?>> getChamaGroups(@RequestParam Optional<Long> groupId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        if (groupId.isPresent()) {
            return chamaGroupService.getGroupDetails(groupId.get())
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }

        return chamaGroupService.getAllGroups(page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group/lookup")
    public Mono<ResponseEntity<?>> groupLookUp(@RequestBody AccountNumberWrapper accountNumberWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.groupAccountLookup(accountNumberWrapper.getAccount(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group/{action}")
    public Mono<ResponseEntity<?>> getGroupObjects(
            @PathVariable
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Action should not contain special characters")
            String action,
            @RequestParam
            long groupId,
            @RequestParam
            int page,
            @RequestParam int size) {
        return Mono.fromCallable(() -> {
                    if (action.equalsIgnoreCase("invites")) {
                        return chamaGroupService.getInvitesByGroup(groupId, page, size);
                    } else {
                        return chamaGroupService.getAllMembersInGroup(groupId, page, size);
                    }
                })
                .flatMap(res -> res)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/upload/")
    public Mono<ResponseEntity<?>> uploadDocuments(
            @RequestParam("files") Flux<FilePart> partFlux,
            @RequestParam long groupId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> {
                    Mono<List<String>> listMono = partFlux
                            .flatMap(filePart -> chamaGroupService.uploadDocuments(groupId, filePart, filePart.filename(), username, Channel.PORTAL))
                            .map(res -> res.getMessage())
                            .collectList();
                    return listMono.flatMap(res -> Mono.just(new UniversalResponse("success", "upload results", res)));
                })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping(value = "/group/categories")
    public Mono<ResponseEntity<?>> getGroupCategories() {
        return chamaGroupService.getAllGroupCategories()
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group/disable/{id}")
    public Mono<ResponseEntity<?>> disableGroup(@PathVariable Long id) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.disableGroup(id, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group/activate/{id}")
    public Mono<ResponseEntity<?>> activateGroup(@PathVariable Long id) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.activateGroup(id, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group/enable")
    public Mono<ResponseEntity<?>> enableGroup(@RequestBody ActivateGroupRequest activateGroupRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.enableGroup(activateGroupRequest.getGroupId(), activateGroupRequest.getCbsAccount(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/message-templates")
    public Mono<ResponseEntity<UniversalResponse>> getMessageTemplates(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return chamaGroupService.getMessageTemplates(page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PutMapping("/edit-template")
    public Mono<ResponseEntity<?>> editMessageTemplate(@RequestBody @Valid MessageTemplateWrapper messageTemplateWrapper) {
        return chamaGroupService.editMessageTemplate(messageTemplateWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/add-template")
    public Mono<ResponseEntity<?>> addMessageTemplate(@RequestBody @Valid MessageTemplateWrapper messageTemplateWrapper) {
        return chamaGroupService.addMessageTemplate(messageTemplateWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
