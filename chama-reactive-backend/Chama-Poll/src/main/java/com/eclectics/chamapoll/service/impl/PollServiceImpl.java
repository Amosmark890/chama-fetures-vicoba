package com.eclectics.chamapoll.service.impl;

import com.eclectics.chamapoll.model.Poll;
import com.eclectics.chamapoll.model.PollCandidates;
import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.model.PollVoteData;
import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.repository.PollCandidatesRepository;
import com.eclectics.chamapoll.repository.PollPositionsRepository;
import com.eclectics.chamapoll.repository.PollRepository;
import com.eclectics.chamapoll.repository.PollVoteDataRepository;
import com.eclectics.chamapoll.service.ChamaKycService;
import com.eclectics.chamapoll.service.NotificationService;
import com.eclectics.chamapoll.service.PollService;
import com.eclectics.chamapoll.service.PublishService;
import com.eclectics.chamapoll.wrappers.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {
    private final ChamaKycService chamaKycService;
    private final PollRepository pollRepository;
    private final PollPositionsRepository pollPositionsRepository;
    private final PollCandidatesRepository pollCandidatesRepository;
    private final PollVoteDataRepository pollVoteDataRepository;
    private final PublishService publishService;
    private final ResourceBundleMessageSource source;
    private final NotificationService notificationService;

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    /**
     * Creates a Poll only without any positions and candidates.
     * Used by the USSD channel to make the process much easier.
     *
     * @param createPollRequest
     * @param loggedUserPhone
     * @return
     */
    @Override
    public Mono<UniversalResponse> createEmptyPoll(CreatePollRequest createPollRequest, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            //get the groupId
            Long groupId = createPollRequest.getGroupId();
            //get loggedUser
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            //get the id of the logged-in user
            long loggedUserId = member.getId();
            //get group by passed id
            Optional<GroupWrapper> optionalGroups = chamaKycService.getGroupById(groupId);
            if (optionalGroups.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            GroupWrapper groups = optionalGroups.get();
            //check if group has already running poll, maximum  active poll limit is one
            boolean groupActivePoll = pollRepository.findPollByGroupIdAndStatus(groupId, Status.ACTIVE).isPresent();
            if (groupActivePoll)
                return new UniversalResponse("fail", getResponseMessage("groupMaxPolls"));

            //get the poll description
            String description = createPollRequest.getDescription() == null ?
                    String.format("% poll", groups.getName()) : createPollRequest.getDescription();
            //Registration date start and end validations
            Date registrationStart = createPollRequest.getRegistrationStart();
            Date registrationEnd = createPollRequest.getRegistrationEnd();
            if (registrationEnd.before(registrationStart))
                return new UniversalResponse("fail", getResponseMessage("incorrectRegistrationPeriod"));

            Calendar regCal = Calendar.getInstance();
            regCal.setTime(registrationStart);
            Calendar nowCal = Calendar.getInstance();
            if (regCal.get(Calendar.DAY_OF_YEAR) < nowCal.get(Calendar.DAY_OF_YEAR))
                return new UniversalResponse("fail", getResponseMessage("invalidRegistrationPeriod"));

            //vote dates validations
            Date voteStart = createPollRequest.getVoteStart();
            Date voteEnd = createPollRequest.getVoteEnd();
            if (voteEnd.before(voteStart) || voteEnd.equals(voteStart))
                return new UniversalResponse("fail", getResponseMessage("invalidVotingPeriodDefined"));

            if (voteStart.before(registrationStart))
                return new UniversalResponse("fail", getResponseMessage("invalidVoteStart"));

            Poll poll = new Poll(description, registrationStart, registrationEnd,
                    voteStart, Status.ACTIVE, voteEnd, loggedUserId, groupId);
            Poll savePoll = pollRepository.save(poll);
            //
            Map<String, Object> data = Map.of(
                    "id", savePoll.getId(),
                    "group", groups.getName(),
                    "description", savePoll.getDescription(),
                    "registration start", savePoll.getRegistrationStart(),
                    "registration end", savePoll.getRegistrationEnd(),
                    "voting start", savePoll.getVotingStart(),
                    "voting end", savePoll.getVotingEnd()
            );
            return new UniversalResponse("success", getResponseMessage("pollCreatedSuccessfully"), data);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> createPoll(CreatePollWrapper createPollWrapper, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            //get the groupId
            Long groupId = createPollWrapper.getGroupId();
            //get loggedUser
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            //get the id of the logged-in user
            long loggedUserId = member.getId();
            //get group by passed id
            Optional<GroupWrapper> optionalGroups = chamaKycService.getGroupById(groupId);
            if (optionalGroups.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            GroupWrapper groups = optionalGroups.get();
            //check if group has already running poll, maximum  active poll limit is one
            boolean groupActivePoll = pollRepository.findPollByGroupIdAndStatus(groupId, Status.ACTIVE).isPresent();
            if (groupActivePoll)
                return new UniversalResponse("fail", getResponseMessage("groupMaxPolls"));

            PollPositionWrapper pollPositions = createPollWrapper.getPositions();
            if (!pollPositions.isChairman() && !pollPositions.isTreasurer() && !pollPositions.isSecretary())
                return new UniversalResponse("fail", getResponseMessage("pollPositionNotDefined"));

            //get the poll description
            String description = createPollWrapper.getDescription() == null ? "" : createPollWrapper.getDescription();
            //Registration date start and end validations
            Date registrationStart = createPollWrapper.getRegistrationStart();
            Date registrationEnd = createPollWrapper.getRegistrationEnd();
            if (registrationEnd.before(registrationStart))
                return new UniversalResponse("fail", getResponseMessage("incorrectRegistrationPeriod"));

            Calendar regCal = Calendar.getInstance();
            regCal.setTime(registrationStart);
            Calendar nowCal = Calendar.getInstance();
            if (regCal.get(Calendar.DAY_OF_YEAR) < nowCal.get(Calendar.DAY_OF_YEAR))
                return new UniversalResponse("fail", getResponseMessage("invalidRegistrationPeriod"));

            //vote dates validations
            Date voteStart = createPollWrapper.getVoteStart();
            Date voteEnd = createPollWrapper.getVoteEnd();
            if (voteEnd.before(voteStart) || voteEnd.equals(voteStart))
                return new UniversalResponse("fail", getResponseMessage("invalidVotingPeriodDefined"));

            if (voteStart.before(registrationStart))
                return new UniversalResponse("fail", getResponseMessage("invalidVoteStart"));

            Poll poll = new Poll(description, registrationStart, registrationEnd,
                    voteStart, Status.ACTIVE, voteEnd, loggedUserId, groupId);
            Poll savePoll = pollRepository.save(poll);
            List<PollPositions> pollPositionsList = getPollPositions(groups, pollPositions, savePoll);
            List<PollPositions> savedPollPositions = pollPositionsRepository.saveAll(pollPositionsList);
            try {
                if (createPollWrapper.getPollCandidatesWrapper() != null)
                    addPositionPollCandidates(createPollWrapper.getPollCandidatesWrapper(), savePoll, savedPollPositions);
            } catch (IllegalArgumentException exception) {
                log.error(exception.getMessage());
                return new UniversalResponse("fail", exception.getMessage());
            }

            return new UniversalResponse("success", getResponseMessage("pollCreatedSuccessfully"), Collections.emptyMap());
        }).publishOn(Schedulers.boundedElastic());
    }

    private void addPositionPollCandidates(PollCandidatesWrapper pollCandidatesWrapper, Poll savePoll, List<PollPositions> savedPollPositions) {
        if (pollCandidatesWrapper.getChairman().isEmpty() && pollCandidatesWrapper.getSecretary().isEmpty() && pollCandidatesWrapper.getTreasurer().isEmpty())
            throw new IllegalArgumentException("No candidate specified");

        if (!pollCandidatesWrapper.getChairman().isEmpty())
            addChairmanPositionPollCandidates(pollCandidatesWrapper.getChairman(), savePoll, savedPollPositions);

        if (!pollCandidatesWrapper.getSecretary().isEmpty())
            addSecretaryPositionPollCandidates(pollCandidatesWrapper.getSecretary(), savePoll, savedPollPositions);

        if (!pollCandidatesWrapper.getTreasurer().isEmpty())
            addTreasurerPositionPollCandidates(pollCandidatesWrapper.getTreasurer(), savePoll, savedPollPositions);
    }

    private void addTreasurerPositionPollCandidates(List<String> treasurer, Poll savePoll, List<PollPositions> savedPollPositions) {
        Optional<PollPositions> pollPosition = savedPollPositions.parallelStream()
                .filter(pollPositions -> pollPositions.getName().equalsIgnoreCase("treasurer"))
                .findFirst();

        if (pollPosition.isEmpty())
            throw new IllegalArgumentException(getResponseMessage("pollPositionNotFound"));

        treasurer.forEach(candidate -> vyePosition(pollPosition.get().getId(), savePoll.getId(), candidate)
                .subscribe(res -> log.info("Vying response... {}", res.getMessage())));
    }

    private void addSecretaryPositionPollCandidates(List<String> chairman, Poll savePoll, List<PollPositions> savedPollPositions) {
        Optional<PollPositions> pollPosition = savedPollPositions.parallelStream()
                .filter(pollPositions -> pollPositions.getName().equalsIgnoreCase("secretary"))
                .findFirst();

        if (pollPosition.isEmpty())
            throw new IllegalArgumentException(getResponseMessage("pollPositionNotFound"));

        chairman.forEach(candidate -> vyePosition(pollPosition.get().getId(), savePoll.getId(), candidate)
                .subscribe(res -> log.info("Vying response... {}", res.getMessage())));
    }

    private void addChairmanPositionPollCandidates(List<String> chairman, Poll savePoll, List<PollPositions> savedPollPositions) {
        Optional<PollPositions> pollPosition = savedPollPositions.parallelStream()
                .filter(pollPositions -> pollPositions.getName().equalsIgnoreCase("chairperson"))
                .findFirst();

        if (pollPosition.isEmpty())
            throw new IllegalArgumentException(getResponseMessage("pollPositionNotFound"));

        chairman.forEach(candidate -> vyePosition(pollPosition.get().getId(), savePoll.getId(), candidate)
                .subscribe(res -> log.info("Vying response... {}", res.getMessage())));
    }

    private List<PollPositions> getPollPositions(GroupWrapper groups, PollPositionWrapper pollPositions, Poll savePoll) {
        List<PollPositions> pollPositionsList = new ArrayList<>();
        if (pollPositions.isChairman()) {
            PollPositions chairman = PollPositions.builder()
                    .name(PositionName.CHAIRPERSON.name())
                    .description(String.format("%s %s", groups.getName(), "chairman"))
                    .status(true)
                    .totalCandidates(0)
                    .totalVotesCasted(0)
                    .poll(savePoll)
                    .build();
            pollPositionsList.add(chairman);
        }
        if (pollPositions.isTreasurer()) {
            PollPositions treasurer = PollPositions.builder()
                    .name(PositionName.TREASURER.name())
                    .description(String.format("%s %s", groups.getName(), "treasurer"))
                    .status(true)
                    .totalCandidates(0)
                    .totalVotesCasted(0)
                    .poll(savePoll)
                    .build();
            pollPositionsList.add(treasurer);
        }
        if (pollPositions.isSecretary()) {
            PollPositions secretary = PollPositions.builder()
                    .name(PositionName.SECRETARY.name())
                    .description(String.format("%s %s", groups.getName(), "secretary"))
                    .status(true)
                    .totalCandidates(0)
                    .totalVotesCasted(0)
                    .poll(savePoll)
                    .build();
            pollPositionsList.add(secretary);
        }
        return pollPositionsList;
    }

    @Override
    public Mono<UniversalResponse> cancelPoll(CancelPollWrapper cancelPollWrapper) {
        return Mono.fromCallable(() -> {
            long groupId = cancelPollWrapper.getGroupId();
            Optional<GroupWrapper> optionalGroup = chamaKycService.getGroupById(groupId);
            if (optionalGroup.isEmpty()) {
                return new UniversalResponse("fail", "Group not found");
            }

            long pollId = cancelPollWrapper.getPollId();
            Poll poll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE).orElse(null);
            if (poll == null)
                return new UniversalResponse("fail", getResponseMessage("pollIsCanceled"));

            poll.setStatus(Status.CANCELLED);
            Poll savedPoll = pollRepository.save(poll);
            Map<String, Object> data = Map.of(
                    "group", optionalGroup.get().getName(),
                    "pollId", savedPoll.getId(),
                    "groupId", savedPoll.getGroupId(),
                    "status", "deactivated",
                    "deactivated on", savedPoll.getLastModifiedDate()
            );
            return new UniversalResponse("success", getResponseMessage("pollCanceled"), data);
        }).publishOn(Schedulers.boundedElastic());
    }

    /**
     * Performs a destructive delete.
     *
     * @param cancelPollWrapper that contains the group id and poll id
     * @param username
     * @return a success or failure
     */
    @Override
    @Transactional
    public Mono<UniversalResponse> deletePoll(CancelPollWrapper cancelPollWrapper, String username) {
        return Mono.fromCallable(() -> {
            Optional<MemberWrapper> memberWrapper = chamaKycService.searchMemberByPhoneNumber(username);
            if (memberWrapper.isEmpty()) return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
            long groupId = cancelPollWrapper.getGroupId();
            Optional<GroupWrapper> optionalGroup = chamaKycService.getGroupById(groupId);
            if (optionalGroup.isEmpty()) {
                return new UniversalResponse("fail", "Group not found");
            }

            long pollId = cancelPollWrapper.getPollId();
            Poll poll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE).orElse(null);
            if (poll == null)
                return new UniversalResponse("fail", getResponseMessage("pollIsCanceled"));

            List<PollPositions> pollPositions = pollPositionsRepository.findAllByPollIdAndStatus(pollId, true);

            pollPositions.forEach(this::deletePollRelationships);
            pollRepository.delete(poll);
            try {
                sendDeletePollMessage(memberWrapper.get(), optionalGroup.get());
            } catch (Exception e)
            {
                log.info("...........{}",e);
            }
//            sendDeletePollMessage(memberWrapper.get(), optionalGroup.get());
            return new UniversalResponse("success", getResponseMessage("pollDeleted"));
        }).publishOn(Schedulers.boundedElastic());
    }


    @Modifying
    @Transactional
    public void deletePollRelationships(PollPositions pos) {
        pollVoteDataRepository.deleteAllByPollPositionsById(pos.getId());
        pollCandidatesRepository.deleteAllByPollPositionsId(pos.getId());
        pollPositionsRepository.delete(pos);
    }

    private void sendDeletePollMessage(MemberWrapper memberWrapper, GroupWrapper groupWrapper) {
        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        chamaKycService.findAllGroupMembersPhonesAndLanguage(groupWrapper.getId())
                .parallelStream()
                .filter(pair -> !pair.getFirst().equals(memberWrapper.getPhonenumber()))
                .forEach(pair -> notificationService.sendDeleteGroupMessage(groupWrapper.getName(), memberName, pair.getFirst(), pair.getSecond()));
    }

    @Override
    public Mono<UniversalResponse> getMemberActivePolls(String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            Optional<MemberWrapper> optionalMemberWrapper = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone);
            if (optionalMemberWrapper.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            MemberWrapper member = optionalMemberWrapper.get();
            Set<Long> groupIds = chamaKycService.getMemberGroupIdsByMemberId(member.getId());
            Date now = new Date();
            log.info("Now::: {}", now);
            List<Poll> pollList = groupIds.stream()
                    .map(groupId -> pollRepository.findPollByGroupIdAndStatusAndVotingEndGreaterThanEqual(groupId, Status.ACTIVE, now))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            List<PollListResponseWrapper> polls = pollList.stream()
                    .map(poll -> {
                        Optional<GroupWrapper> optionalGroupWrapper = chamaKycService.getGroupById(poll.getGroupId());
                        return optionalGroupWrapper.map(groupWrapper -> new PollListResponseWrapper(poll.getId(), poll.getGroupId(), groupWrapper.getName(), poll.getDescription(), poll.getVotingStart(), poll.getVotingEnd())).orElse(null);
                    }).collect(Collectors.toList());
            return new UniversalResponse("success", getResponseMessage("currentActivePolls"), polls);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getPollPositions(long pollId, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            Optional<Poll> optionalPoll = pollRepository.findById(pollId);
            if (optionalPoll.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("pollNotFound"));

            Poll poll = optionalPoll.get();
            GroupWrapper group = chamaKycService.getGroupById(poll.getGroupId()).get();
            boolean memberIsPartOfGroup = chamaKycService.getMemberGroupByMemberIdAndGroupId(member.getId(), group.getId());
            if (!memberIsPartOfGroup)
                return new UniversalResponse("fail", getResponseMessage("nonMember"));

            List<PollPositions> pollPositions = pollPositionsRepository.findAllByPollIdAndStatus(poll.getId(), true);
            List<PollPositionListResponseWrapper> pollPos = pollPositions
                    .stream()
                    .map(pos -> new PollPositionListResponseWrapper(pos.getId(), pos.getName(), pos.getTotalCandidates(), viedForPosition(pos.getId(), member.getId())))
                    .collect(Collectors.toList());

            return new UniversalResponse("success", getResponseMessage("pollPositionsList"), pollPos);
        }).publishOn(Schedulers.boundedElastic());
    }

    private boolean viedForPosition(long positionId, Long memberId) {
        PollCandidates pollCandidate = pollCandidatesRepository.findPollCandidatesByPositionIdAndMemberIdAndStatus(positionId, memberId, Status.ACTIVE).orElse(null);
        return pollCandidate == null;
    }

    @Override
    public Mono<UniversalResponse> vyePosition(long positionId, long pollId, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            Optional<Poll> optionalPoll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE);
            if (optionalPoll.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("pollByStatus"));

            Poll poll = optionalPoll.get();
            boolean isMemberPartOfGroup = chamaKycService.getMemberGroupByMemberIdAndGroupId(member.getId(), poll.getGroupId());
            if (!isMemberPartOfGroup) {
                return new UniversalResponse("fail", getResponseMessage("nonMember"));
            }
            PollPositions pollPositions = pollPositionsRepository.findPollPositionsByIdAndStatus(positionId, true).orElse(null);
            if (pollPositions == null) {
                return new UniversalResponse("fail", getResponseMessage("inactivePollPosition"));
            }
            Optional<PollCandidates> pollCandidate =
                    pollCandidatesRepository.findPollCandidatesByPollIdAndMemberIdAndStatus(poll.getId(), member.getId(), Status.ACTIVE);

            if (pollCandidate.isPresent())
                return new UniversalResponse("fail", "You have already vied for a position.");

            // Check disabled for now
//            if (new Date().after(poll.getRegistrationStart()) && new Date().before(poll.getRegistrationEnd())) {}
            PollCandidates candidates = createPollCandidate(member, pollPositions, poll);
            VyePositionResponse wrapper = new VyePositionResponse("registered", candidates.getFirstName(),
                    candidates.getLastName(), candidates.getPositionId(), candidates.getId(), pollPositions.getName());

            return new UniversalResponse("success", getResponseMessage("registerSuccessfully"), wrapper);
//            return new UniversalResponse("fail", getResponseMessage("pollRegistrationEnded"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> exitPosition(long positionId, long pollId, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            Optional<Poll> optionalPoll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE);
            if (optionalPoll.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("inactivePoll"));
            }
            Poll poll = optionalPoll.get();
            PollPositions pollPositions = pollPositionsRepository.findPollPositionsByIdAndStatus(positionId, true).orElse(null);

            if (pollPositions == null)
                return new UniversalResponse("fail", getResponseMessage("inactivePollPosition"));

            if (new Date().after(poll.getRegistrationStart()) && new Date().before(poll.getRegistrationEnd())) {
                PollCandidates pollCandidates = pollCandidatesRepository.findPollCandidatesByPositionIdAndMemberIdAndStatus(positionId, member.getId(), Status.ACTIVE).orElse(null);
                if (pollCandidates != null) {
                    pollCandidatesRepository.delete(pollCandidates);
                    return new UniversalResponse("success", getResponseMessage("removeCandidate"));
                }
                return new UniversalResponse("fail", getResponseMessage("notVied"));
            }
            return new UniversalResponse("fail", getResponseMessage("registrationEnded"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Transactional
    @Override
    public Mono<UniversalResponse> votePositions(VoteRequestWrapper requestWrapper, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            long pollId = requestWrapper.getPollId();
            Optional<Poll> optionalPoll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE);
            if (optionalPoll.isEmpty()) {
                return new UniversalResponse("fail", "Poll is  deactivated or does not exist");
            }
            Poll poll = optionalPoll.get();
            boolean isMemberPartOfGroup = chamaKycService.getMemberGroupByMemberIdAndGroupId(member.getId(), poll.getGroupId());
            if (!isMemberPartOfGroup) {
                return new UniversalResponse("fail", getResponseMessage("inactivePoll"));
            }
            Optional<PollPositions> optionalPollPositions = pollPositionsRepository.findPollPositionsByIdAndStatus(requestWrapper.getPositionId(), true);
            if (optionalPollPositions.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("inactivePollPosition"));
            }
            PollPositions pollPositions = optionalPollPositions.get();
            Optional<PollCandidates> optionalPollCandidates = pollCandidatesRepository.findPollCandidatesByIdAndStatus(requestWrapper.getCandidateId(), Status.ACTIVE);
            if (optionalPollCandidates.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("pollCandidateNotFound"));
            }
            PollCandidates pollCandidates = optionalPollCandidates.get();
            boolean voted = pollVoteDataRepository.existsByCreatorIdAndPollPositions(member.getId(), pollPositions);
            if (voted)
                return new UniversalResponse("fail", getResponseMessage("alreadyVoted"));

            //increment counts
            pollCandidates.setVoteCounts(pollCandidates.getVoteCounts() + 1);
            pollPositions.setTotalVotesCasted(pollPositions.getTotalVotesCasted() + 1);
            PollCandidates savedPollCandidates = pollCandidatesRepository.save(pollCandidates);
            PollPositions savedPollPosition = pollPositionsRepository.save(pollPositions);
            savePollVoteData(member.getId(), savedPollPosition, savedPollCandidates);
            LinkedHashMap<String, Object> respData = new LinkedHashMap<>();
            respData.put("positionName", savedPollPosition.getName());
            respData.put("candidateId", savedPollCandidates.getMemberId());
            respData.put("votesCasted", savedPollPosition.getTotalVotesCasted());
            return new UniversalResponse("success", getResponseMessage("votedSuccessfully"), respData);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void savePollVoteData(long memberId, PollPositions pollPosition, PollCandidates pollCandidate) {
        PollVoteData pollVoteData = new PollVoteData();
        pollVoteData.setCreatorId(memberId);
        pollVoteData.setCandidateId(pollCandidate.getId());
        pollVoteData.setMemberId(pollCandidate.getMemberId());
        pollVoteData.setPositionId(pollPosition.getId());
        pollVoteData.setVoteTime(LocalDateTime.now());
        pollVoteData.setPollPositions(pollPosition);
        pollVoteData.setPollCandidates(pollCandidate);
        pollVoteDataRepository.save(pollVoteData);
    }

    @Override
    public Mono<UniversalResponse> getPollResults(long pollId, String username) {
        return Mono.fromCallable(() -> getPollSummary(pollId, username)).publishOn(Schedulers.boundedElastic());
    }

    private UniversalResponse getPollSummary(long pollId, String username) {
        MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(username).orElseGet(null);
        if (member == null)
            return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

        Optional<Poll> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return new UniversalResponse("fail", getResponseMessage("pollNotFound"));
        }
        Poll poll = optionalPoll.get();
        GroupWrapper group = chamaKycService.getGroupById(poll.getGroupId()).get();
        List<PollPositions> pollPositionsList = pollPositionsRepository.findAllByPollIdAndStatus(poll.getId(), true);
        List<PositionResultDataResp> positionDataResp = pollPositionsList
                .stream()
                .map(pos -> {
                    List<PollCandidatesResultResp> res = pollCandidatesRepository
                            .findPollCandidatesByPositionIdAndStatusOrderByVoteCounts(pos.getId(), Status.ACTIVE)
                            .stream()
                            .map(can -> new PollCandidatesResultResp(can.getId(), can.getFirstName(), can.getLastName(), can.getVoteCounts()))
                            .collect(Collectors.toList());
                    return new PositionResultDataResp(pos.getId(), pos.getName(), res.size(), pos.getTotalVotesCasted(), res);
                }).collect(Collectors.toList());
        Long groupMembers = chamaKycService.countGroupMembers(poll.getGroupId());

        PollCandidates pollCandidate =
                pollCandidatesRepository.findPollCandidatesByPollIdAndMemberIdAndStatus(poll.getId(), member.getId(), Status.ACTIVE)
                        .orElse(null);

        Long vyingPositionId = null;
        if (pollCandidate != null)
            vyingPositionId = pollCandidate.getPositionId();

        // In an ideal case, the poll
//            boolean isVying = poll.getRegistrationEnd().after(new Date());
        PollVoteDataResp voteDataResp = new PollVoteDataResp(poll.getId(), group.getName(), poll.getDescription(),
                groupMembers, poll.getStatus().name(), false, vyingPositionId, poll.getRegistrationStart(),
                poll.getRegistrationEnd(), poll.getVotingStart(), poll.getVotingEnd(), positionDataResp);
        return new UniversalResponse("success", "Poll vote data", voteDataResp);
    }

    @Override
    public Mono<UniversalResponse> addPositionPollCandidate(String positionName, long pollId, String candidatePhone) {
        return Mono.fromCallable(() -> {
            Optional<MemberWrapper> memberWrapper = chamaKycService.searchMemberByPhoneNumber(candidatePhone);

            if (memberWrapper.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Optional<Poll> optionalPoll = pollRepository.findById(pollId);
            if (optionalPoll.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("pollNotFound"));

            Optional<PollPositions> optionalPollPosition = pollPositionsRepository.findByPollAndName(optionalPoll.get(), positionName.toLowerCase());

            if (optionalPollPosition.isEmpty()) {
                PollPositions createdPollPositions = createPollPosition(optionalPoll.get(), positionName.toLowerCase());
                return addMemberToPosition(createdPollPositions, pollId, candidatePhone).block();
            }
            List<PollCandidates> pollCandidates = pollCandidatesRepository.findAllByPositionIdAndStatus(optionalPollPosition.get().getId(), Status.ACTIVE);

            // set a limit of 3 candidates
            if (pollCandidates.size() > 3) {
                return new UniversalResponse("fail", getResponseMessage("maxVyingCandidates"));
            }

            // Send message to indicate poll has been created
            if (pollCandidates.isEmpty())
                sendPollStartMessage(optionalPoll.get());

            Optional<PollCandidates> pollCandidate = pollCandidates.parallelStream().filter(pc -> pc.getMemberId() == memberWrapper.get().getId())
                    .findFirst();

            if (pollCandidate.isPresent())
                return new UniversalResponse("fail", getResponseMessage("candidateIsInPoll"));

            return addMemberToPosition(optionalPollPosition.get(), pollId, candidatePhone).block();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendPollStartMessage(Poll poll) {
        Optional<GroupWrapper> groupWrapperOptional = chamaKycService.getGroupById(poll.getGroupId());

        groupWrapperOptional.ifPresentOrElse(group -> {
            chamaKycService.getGroupMembers(group.getId())
                    .parallelStream()
                    .forEach(member -> notificationService.sendPollStartMessage(group.getName(), poll.getVotingStart(),
                            poll.getVotingEnd(), member.getPhonenumber(), member.getLanguage()));
        }, () -> log.info("Group not found... On sending group poll init"));
    }

    private PollPositions createPollPosition(Poll poll, String positionName) {
        return Mono.fromCallable(() -> {
            PollPositions pollPositions = PollPositions.builder()
                    .poll(poll)
                    .description(positionName)
                    .status(true)
                    .name(positionName)
                    .totalCandidates(0)
                    .totalVotesCasted(0)
                    .build();
            return pollPositionsRepository.save(pollPositions);
        }).publishOn(Schedulers.boundedElastic()).block();
    }

    @Override
    public Mono<UniversalResponse> addMemberToPosition(PollPositions pollPosition, long pollId, String loggedUserPhone) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMemberByPhoneNumber(loggedUserPhone).get();
            Optional<Poll> optionalPoll = pollRepository.findPollByIdAndStatus(pollId, Status.ACTIVE);
            if (optionalPoll.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("pollByStatus"));

            Poll poll = optionalPoll.get();
            boolean isMemberPartOfGroup = chamaKycService.getMemberGroupByMemberIdAndGroupId(member.getId(), poll.getGroupId());
            if (!isMemberPartOfGroup) {
                return new UniversalResponse("fail", getResponseMessage("nonMember"));
            }

            Optional<PollCandidates> pollCandidate =
                    pollCandidatesRepository.findPollCandidatesByPollIdAndMemberIdAndStatus(poll.getId(), member.getId(), Status.ACTIVE);

            if (pollCandidate.isPresent())
                return new UniversalResponse("fail", "Member has already vied for a position" + pollPosition.getName());

            PollCandidates candidates = createPollCandidate(member, pollPosition, poll);
            VyePositionResponse wrapper = new VyePositionResponse("registered", candidates.getFirstName(),
                    candidates.getLastName(), candidates.getPositionId(), candidates.getId(), pollPosition.getName());

            return new UniversalResponse("success", getResponseMessage("registerSuccessfully"), wrapper);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> findPollsInGroup(GroupIdWrapper groupIdWrapper) {
        return Mono.fromCallable(() -> {
            List<Poll> pollsInGroup = pollRepository.findAllByGroupIdAndStatusAndVotingEndGreaterThanEqual(groupIdWrapper.getGroupId(), Status.ACTIVE, new Date());

            List<PollListResponseWrapper> polls = pollsInGroup.stream()
                    .map(poll -> {
                        Optional<GroupWrapper> optionalGroupWrapper = chamaKycService.getGroupById(poll.getGroupId());
                        return optionalGroupWrapper.map(groupWrapper -> new PollListResponseWrapper(poll.getId(), poll.getGroupId(), groupWrapper.getName(), poll.getDescription(), poll.getRegistrationStart(), poll.getVotingEnd())).orElse(null);
                    }).collect(Collectors.toList());
            return new UniversalResponse("success", getResponseMessage("groupPolls"), polls);
        }).publishOn(Schedulers.boundedElastic());
    }

    private PollCandidates createPollCandidate(MemberWrapper member, PollPositions pollPosition, Poll poll) {
        PollCandidates candidates = new PollCandidates();
        candidates.setFirstName(member.getFirstname());
        candidates.setLastName(member.getLastname());
        candidates.setStatus(Status.ACTIVE);
        candidates.setPositionId(pollPosition.getId());
        candidates.setPollId(poll.getId());
        candidates.setMemberId(member.getId());
        candidates.setCreatedOn(new Date());
        candidates.setPollPositions(pollPosition);
        candidates = pollCandidatesRepository.save(candidates);
        return candidates;
    }

}
