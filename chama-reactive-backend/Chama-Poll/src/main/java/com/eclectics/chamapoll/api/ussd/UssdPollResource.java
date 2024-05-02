package com.eclectics.chamapoll.api.ussd;

import com.eclectics.chamapoll.config.CustomAuthenticationUtil;
import com.eclectics.chamapoll.service.PollService;
import com.eclectics.chamapoll.wrappers.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/poll/ussd")
public class UssdPollResource {
    private final PollService pollService;

    @Operation(summary = "Creates a poll", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/create")
    @PreAuthorize("hasPermission(#createPollRequest.groupId, 'poll', @objectAction.initFields('polls','cancreate'))")
    public Mono<ResponseEntity<UniversalResponse>> createPoll(@Parameter @RequestBody CreatePollRequest createPollRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.createEmptyPoll(createPollRequest, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Cancels a poll", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/cancel")
    @PreAuthorize("hasPermission(#cancelPollWrapper.groupId, 'poll', @objectAction.initFields('polls','candelete'))")
    public Mono<ResponseEntity<UniversalResponse>> cancelPoll(@Parameter @RequestBody CancelPollWrapper cancelPollWrapper) {
        return pollService.cancelPoll(cancelPollWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/delete")
    @Operation(summary = "Cancels a poll", tags = "Poll USSD Resource")
    @PreAuthorize("hasPermission(#cancelPollWrapper.groupId, 'poll', @objectAction.initFields('polls','candelete'))")
    public Mono<ResponseEntity<UniversalResponse>> deletePoll(@Parameter @RequestBody CancelPollWrapper cancelPollWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.deletePoll(cancelPollWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Lists active polls", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/active-polls")
    public Mono<ResponseEntity<UniversalResponse>> getActivePolls() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(pollService::getMemberActivePolls)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Lists poll positions by poll ID", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/positions")
    public Mono<ResponseEntity<UniversalResponse>> getPollPositions(@RequestBody PollPositionRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.getPollPositions(request.getPollId(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Allows a member to vye for a position", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/vye")
    public Mono<ResponseEntity<UniversalResponse>> vyePositions(@Parameter @RequestBody VyePositionRequest vyePositionRequest) {
        long positionId = vyePositionRequest.getPositionId();
        long pollId = vyePositionRequest.getPollId();
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.vyePosition(positionId, pollId, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Allows a member to exit a vied position", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/exit-position")
    public Mono<ResponseEntity<UniversalResponse>> exitViedPosition(@Parameter @RequestBody ExitPollRequest pollRequest) {
        long positionId = pollRequest.getPositionId();
        long pollId = pollRequest.getPollId();
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.exitPosition(positionId, pollId, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = " Allows a group member to vote ", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/vote")
    public Mono<ResponseEntity<UniversalResponse>> voteForPosition(@Parameter @RequestBody VoteRequestWrapper voteRequestWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.votePositions(voteRequestWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Allows a member to get results by poll ID", tags = "Poll USSD Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/results")
    public Mono<ResponseEntity<UniversalResponse>> getResults(@RequestBody PollResultRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.getPollResults(request.getPollId(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/add-candidate")
    @PreAuthorize("hasPermission(#vyePositionRequest.groupId, 'poll', @objectAction.initFields('polls','canedit'))")
    @ApiOperation(value = "Used by the secretary to add candidates.", notes = "This endpoint allows only 3 candidates to be added. To be used in Vicoba.", tags = "Poll USSD Resource")
    public Mono<ResponseEntity<UniversalResponse>> addPositionCandidate(@Parameter @RequestBody VyePositionRequest vyePositionRequest) {
        return pollService.addPositionPollCandidate(vyePositionRequest.getPositionName(), vyePositionRequest.getPollId(), vyePositionRequest.getPhoneNumber())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-polls")
    @ApiOperation(value = "Fetch polls in group", tags = "Poll USSD Resource")
    public Mono<ResponseEntity<?>> getPollsInGroup(@RequestBody GroupIdWrapper groupIdWrapper) {
        return pollService.findPollsInGroup(groupIdWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
