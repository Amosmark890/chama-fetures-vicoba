package com.eclectics.chamapoll.api.mobile;

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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/poll")
public class PollResource {
    private final PollService pollService;

    @Operation(summary = "Creates a poll", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/create")
    @PreAuthorize("hasPermission(#pollWrapper.getGroupId(), 'poll', @objectAction.initFields('polls','cancreate'))")
    public Mono<ResponseEntity<UniversalResponse>> createPoll(@RequestBody CreatePollWrapper pollWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.createPoll(pollWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Cancels a poll", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/cancel")
    @PreAuthorize("hasPermission(#cancelPollWrapper.getGroupId(), 'poll', @objectAction.initFields('polls','candelete'))")
    public Mono<ResponseEntity<UniversalResponse>> cancelPoll(@Parameter @RequestBody CancelPollWrapper cancelPollWrapper) {
        return pollService.cancelPoll(cancelPollWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete a poll", tags = "Poll Resource")
    @PreAuthorize("hasPermission(#cancelPollWrapper.getGroupId(), 'poll', @objectAction.initFields('polls','candelete'))")
    public Mono<ResponseEntity<UniversalResponse>> deletePoll(@Parameter @RequestBody CancelPollWrapper cancelPollWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.deletePoll(cancelPollWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Lists active polls", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @GetMapping("/active-polls")
    public Mono<ResponseEntity<UniversalResponse>> getActivePolls() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(pollService::getMemberActivePolls)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Lists poll positions by poll ID", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @GetMapping("/positions")
    public Mono<ResponseEntity<UniversalResponse>> getPollPositions(@Parameter @RequestParam(value = "pollId") long pollId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.getPollPositions(pollId, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Allows a member to vye for a position", tags = "Poll Resource")
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

    @Operation(summary = "Allows a member to exit a vied position", tags = "Poll Resource")
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

    @Operation(summary = " Allows a group member to vote ", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/vote")
    public Mono<ResponseEntity<UniversalResponse>> voteForPosition(@Parameter @RequestBody VoteRequestWrapper voteRequestWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.votePositions(voteRequestWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @Operation(summary = "Allows a member to get results by poll ID", tags = "Poll Resource")
    @ApiResponse(responseCode = "200", description = "success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UniversalResponse.class)))
    @PostMapping("/results/{pollId}")
    public Mono<ResponseEntity<UniversalResponse>> getResults(@Parameter @PathVariable(value = "pollId") long pollId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> pollService.getPollResults(pollId, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/add-candidate")
    @PreAuthorize("hasPermission(#vyePositionRequest.getGroupId(), 'poll', @objectAction.initFields('polls','canedit'))")
    @ApiOperation(value = "Used by the secretary to add candidates.", notes = "This endpoint allows only 3 candidates to be added. To be used in Vicoba.", tags = "Poll Resource")
    public Mono<ResponseEntity<UniversalResponse>> addPositionCandidate(@Parameter @RequestBody VyePositionRequest vyePositionRequest) {
        return pollService.addPositionPollCandidate(vyePositionRequest.getPositionName(), vyePositionRequest.getPollId(), vyePositionRequest.getPhoneNumber())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-polls")
    @ApiOperation(value = "Fetch polls in group", tags = "Poll Resource")
    public Mono<ResponseEntity<?>> getPollsInGroup(@RequestBody GroupIdWrapper groupIdWrapper) {
        return pollService.findPollsInGroup(groupIdWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
