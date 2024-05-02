package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.repository.*;
import com.eclectics.chamapayments.service.*;
import com.eclectics.chamapayments.service.enums.LoanStatusEnum;
import com.eclectics.chamapayments.service.enums.LoanType;
import com.eclectics.chamapayments.service.enums.PaymentEnum;
import com.eclectics.chamapayments.util.TransactionIdGenerator;
import com.eclectics.chamapayments.wrappers.request.*;
import com.eclectics.chamapayments.wrappers.response.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.eclectics.chamapayments.util.RequestConstructor.constructBody;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {
    public static final String GROUP_NOT_FOUND = "groupNotFound";
    public static final String MEMBER_NOT_FOUND = "memberNotFound";
    private final ESBService esbService;
    private final WithdrawallogsRepo withdrawallogsRepo;
    private final LoanproductsRepository loanproductsRepository;
    private final LoanapplicationsRepo loanapplicationsRepo;
    private final LoansdisbursedRepo loansdisbursedRepo;
    private final LoanrepaymentpendingapprovalRepo loanrepaymentpendingapprovalRepo;
    private final LoansrepaymentRepo loansrepaymentRepo;
    private final FileHandlerServiceImpl fileHandlerService;
    private final GuarantorsRepository guarantorsRepository;
    private final AccountsRepository accountsRepository;
    private final TransactionlogsRepo transactionlogsRepo;
    private final LoanPenaltyRepository loanPenaltyRepository;
    private final LoanPenaltyPaymentRepository loanPenaltyPaymentRepository;
    private final ChamaKycService chamaKycService;
    private final RouterService routerService;
    private final NotificationService notificationService;
    private final ESBLoggingService esbLoggingService;
    private final ContributionRepository contributionRepository;
    private final Gson gson;
    private final ContributionsPaymentRepository contributionsPaymentRepository;
    private final ContributionLoanRepository contributionLoanRepository;
    private final ResourceBundleMessageSource source;

    @Value("${app-configs.online-checkout}")
    public String onlineCheckoutURL;
    @Value("${app-configs.online-query}")
    public String mpesaQueryURL;
    @Value("${app-configs.b2c-url}")
    public String B2CURL;
    @Value("${app-configs.b2c-online-query}")
    public String B2CURLQueryTransactionStatus;
    @Value("${app-configs.pg-client-username}")
    public String pgUsername;
    @Value("${app-configs.pg-client-id}")
    public String pgClientId;
    @Value("${app-configs.pg-client-name}")
    public String pgClientName;
    @Value("${app-configs.pg-client-password}")
    public String pgClientPassword;
    @Value("${app-configs.pg-service-id}")
    public String pgServiceId;

    @Value("${vicoba.url}")
    private String vicobaUrl;
    private WebClient webClient;

    @PostConstruct
    private void init() {
        webClient = WebClient.builder().baseUrl(vicobaUrl).build();
    }

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    public boolean checkUserIsPartOfGroup(long groupId) {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phone);
        if (memberWrapper == null) return false;
        GroupMemberWrapper optionalGroupMembership = chamaKycService.getMonoGroupMembershipByGroupIdAndMemberId(groupId, memberWrapper.getId());
        return optionalGroupMembership != null;
    }

    @Override
    public int checkLoanLimit(String phoneNumber, Long contributionId) {
        List<ContributionPayment> userContributionPayments = contributionsPaymentRepository.findUsersContribution(contributionId, phoneNumber);
        if (userContributionPayments.isEmpty()) {
            return 0;
        }
        return userContributionPayments.stream()
                .mapToInt(ContributionPayment::getAmount)
                .sum();
    }

    @Override
    public boolean checkIfUserHasExistingLoans(String phoneNumber) {
        return !contributionLoanRepository.findUserLoans(phoneNumber).isEmpty();
    }

    @Override
    public Mono<UniversalResponse> giveUserLoan(Loan loan) {
        return Mono.fromCallable(() -> {
            //send message to guarantors for approval
            List<Loan> loanList = contributionLoanRepository.findUserLoans(loan.getPhoneNumber());
            if (!loanList.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("youHavePreExistingLoans"));
            }
            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(loan.getPhoneNumber());
            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            loan.setRepaymentDate(cal.getTime());
            loan.setInterestRate(5.878f);
            loan.setLoanStatus(LoanStatusEnum.APPROVED.name());
            loan.setLoanType(LoanType.PERCENTANGE_LOAN.toString());
            contributionLoanRepository.save(loan);
            return routerService.makeB2Crequest(loan.getPhoneNumber(), loan.getAmount(), loan.getId())
                    .map(res -> {
                        if (!res) {
                            return new UniversalResponse("fail", getResponseMessage("couldNotSendMoneyToClient"));
                        } else {
                            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(loan.getPhoneNumber());
                            notificationService.sendLoanApprovalText(member, loan.getAmount(), member.getLanguage());
                            return new UniversalResponse("success", getResponseMessage("loanSaved"));
                        }
                    }).block();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> giveUserGuarantorLoan(Loan loan) {
        return Mono.fromCallable(() -> {
            List<Guarantors> guarantors = loan.getGuarantors();
            Optional<LoanApplications> loanApplicationsOptional = loanapplicationsRepo.findById(loan.getId());
            if (loanApplicationsOptional.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanNotFound"));
            }
            LoanApplications loanApplications = loanApplicationsOptional.get();
            //check if guarantor qualifies to give that guarantee that loan
            //check if user has existing guarantor loans
            StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loanApplications.getMemberId());
            boolean selfGuaranteed = guarantors.stream()
                    .map(Guarantors::getPhoneNumber)
                    .anyMatch(guarantorPhone -> member.getPhonenumber().equalsIgnoreCase(guarantorPhone.trim().replace(" ", "")));
            if (selfGuaranteed) {
                return new UniversalResponse("fail", getResponseMessage("cannotGuaranteeSelfLoan"));
            }
            //perform check on guarantor to check if they qualifies to give loan guarantorship
            try {
                checkIfGuarantorQualify(loanApplications, loanApplications.getLoanProducts().getContributions().getId(), guarantors, stringJoiner);
            } catch (Exception ex) {
                return new UniversalResponse("fail", ex.getMessage());
            }
            List<Guarantors> updatedGuarantors = guarantors
                    .stream()
                    .map(guarantor -> {
                        MemberWrapper guarantorDetails = chamaKycService.searchMonoMemberByPhoneNumber(guarantor.getPhoneNumber());
                        if (guarantorDetails == null) return null;
                        return Guarantors
                                .builder()
                                .loanId(loan.getId())
                                .loanStatus(LoanStatusEnum.PENDING_APPROVAL.name())
                                .guarantorName(String.format("%s %s", guarantorDetails.getFirstname(), guarantorDetails.getLastname()))
                                .phoneNumber(guarantorDetails.getPhonenumber())
                                .loanStatus(LoanStatusEnum.PENDING_APPROVAL.name())
                                .build();
                    }).collect(Collectors.toList());

            guarantorsRepository.saveAll(updatedGuarantors);

            List<String> phoneNumbers = updatedGuarantors.stream()
                    .map(Guarantors::getPhoneNumber)
                    .collect(Collectors.toList());

            String memberName = String.format("%s %s", member.getFirstname(), member.getLastname());
            notificationService.sendGuarantorsInviteMessage(phoneNumbers, loan.getAmount(), memberName, member.getLanguage());

            return new UniversalResponse("success", getResponseMessage("guarantorAdded"));
        }).publishOn(Schedulers.boundedElastic());
    }

    private void checkIfGuarantorQualify(LoanApplications loanApplications, Long contributionLoan, List<Guarantors> guarantorsList, StringJoiner stringJoiner) throws RuntimeException {
        //check if loan is already fully guaranteed
        double totalGuaranteedAmount = guarantorsList.stream()
                .map(guarantors -> guarantorsRepository.findGuarantorsByPhoneNumberAndLoanId(guarantors.getPhoneNumber(), loanApplications.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(guarantors -> guarantors.getLoanStatus().equalsIgnoreCase(PaymentEnum.PAYMENT_SUCCESS.name()))
                .mapToDouble(Guarantors::getAmount)
                .sum();
        if (totalGuaranteedAmount >= loanApplications.getAmount()) {
            throw new RuntimeException("Loan is already fully guaranteed");
        }
        //check if added guarantor already declined the  loan request
        long deniedGuarantorRequestCount = guarantorsList.stream()
                .map(guarantors -> guarantorsRepository.findGuarantorsByPhoneNumberAndLoanIdAndLoanStatus(guarantors.getPhoneNumber(), loanApplications.getId(), LoanStatusEnum.DECLINED.name()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(guarantors -> stringJoiner.add(guarantors.getPhoneNumber()))
                .count();
        if (deniedGuarantorRequestCount > 0) {
            throw new RuntimeException(String.format(getResponseMessage("guarantorDOesNotQualify"), stringJoiner));
        }

        //check if guarantors have enough savings
        AtomicBoolean hasEnoughSavings = new AtomicBoolean(true);

        guarantorsList.forEach(guarantors -> {
            List<ContributionPayment> contributionPayments = contributionsPaymentRepository.findUsersContribution(contributionLoan, guarantors.getPhoneNumber());
            double contributionAmount = contributionPayments.stream().mapToDouble(ContributionPayment::getAmount).sum();
            if (guarantors.getAmount() > contributionAmount) {
                stringJoiner.add(guarantors.getPhoneNumber());
                hasEnoughSavings.set(false);
            }
        });

        if (!hasEnoughSavings.get()) {
            throw new RuntimeException(String.format(getResponseMessage("guarantorDOesNotQualify"), stringJoiner));
        }
        //check if guarantors have already guaranteed other loans and their savings can guarantee new loan request
        boolean hasAmountToGuarantee = true;
        for (Guarantors guarantors : guarantorsList) {
            MemberWrapper guarantor = chamaKycService.searchMonoMemberByPhoneNumber(guarantors.getPhoneNumber());
            List<Guarantors> guarantedLoans = guarantorsRepository.findGuarantorsWithExistingLoans(guarantors.getPhoneNumber(), LoanStatusEnum.APPROVED.name(), LoanStatusEnum.PENDING_APPROVAL.name());
            List<ContributionPayment> totalContributions = contributionsPaymentRepository.findTotalContributions(contributionLoan, PaymentEnum.PAYMENT_SUCCESS.name(), guarantors.getPhoneNumber());
            List<LoansDisbursed> guarantorLoans = loansdisbursedRepo.findByMemberIdAndDueamountIsLessThanOrderByCreatedOnDesc(guarantor.getId(), 1);

            double totalGuarantedLoans = guarantedLoans.stream().mapToDouble(Guarantors::getAmount).sum();
            double totalGuarantorsLoans = guarantorLoans.stream().mapToDouble(LoansDisbursed::getDueamount).sum();
            double totalGuarantorContribution = totalContributions.stream().mapToDouble(ContributionPayment::getAmount).sum();

            double amountLeftToGuarantee = totalGuarantorContribution - (totalGuarantorsLoans + totalGuarantedLoans);

            if (amountLeftToGuarantee < guarantors.getAmount()) {
                stringJoiner.add(guarantors.getPhoneNumber());
                hasAmountToGuarantee = false;
            }
        }
        if (!hasAmountToGuarantee) {
            throw new RuntimeException(String.format(getResponseMessage("guarantorDOesNotQualify"), stringJoiner));
        }
    }

    @Override
    public Mono<UniversalResponse> getGuarantorLoans(String phoneNumber) {
        return Mono.fromCallable(() -> {
            List<Guarantors> guarantorsList =
                    guarantorsRepository.findGuarantorsByPhoneNumberAndLoanStatusAndSoftDeleteFalse(phoneNumber, LoanStatusEnum.APPROVED.name());
            if (guarantorsList.isEmpty()) {
                return new UniversalResponse("success", getResponseMessage("noLoansGuaranteed"));
            }
            return new UniversalResponse("success", "guarantor loans", guarantorsList);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> approveDenyGuarantorRequest(Long loanId, boolean guarantee, String username) {
        return Mono.fromCallable(() -> {
            Optional<Guarantors> optionalGuarantors =
                    guarantorsRepository.findGuarantorsByPhoneNumberAndLoanIdAndLoanStatus(username, loanId, LoanStatusEnum.PENDING_APPROVAL.name());

            if (optionalGuarantors.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("guarantorRecordDoesNotExist"));
            }
            Guarantors guarantor = optionalGuarantors.get();
            Optional<LoanApplications> contributionLoanOptional = loanapplicationsRepo.findById(guarantor.getLoanId());
            if (contributionLoanOptional.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotFound"));
            }
            LoanApplications loanApplication = contributionLoanOptional.get();
            MemberWrapper applicant = chamaKycService.getMonoMemberDetailsById(loanApplication.getMemberId());

            if (applicant == null)
                return new UniversalResponse("fail", getResponseMessage("loanApplicantNotFound"));

            String memberName = String.format("%s %s", applicant.getFirstname(), applicant.getLastname());

            handleApprovalGuarantoship(guarantee, guarantor, loanApplication, applicant, memberName);

            return new UniversalResponse("success", getResponseMessage("requestProcessedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    private void handleApprovalGuarantoship(boolean guarantee, Guarantors guarantor, LoanApplications loanApplication, MemberWrapper memberWrapper, String memberName) {
        if (guarantee) {
            guarantor.setLoanStatus(LoanStatusEnum.APPROVED.name());
            guarantorsRepository.save(guarantor);
            notificationService.sendGuarantorshipApprovalMessage(guarantor.getPhoneNumber(),
                    guarantor.getGuarantorName(), memberWrapper.getPhonenumber(),
                    memberWrapper.getFirstname(), guarantor.getAmount(), memberWrapper.getLanguage());
            chamaKycService.getFluxMembersLanguageAndPhonesInGroup(loanApplication.getLoanProducts().getGroupId())
                    .toStream()
                    .filter(pair -> !Objects.equals(pair.getFirst(), memberWrapper.getPhonenumber()))
                    .forEach(pair -> notificationService.sendGroupGuarantorshipApprovedMessage(
                            pair.getFirst(),
                            guarantor.getGuarantorName(),
                            memberWrapper.getPhonenumber(),
                            memberName, guarantor.getAmount(),
                            pair.getSecond()));
        } else {
            guarantor.setLoanStatus(LoanStatusEnum.DECLINED.name());
            guarantorsRepository.save(guarantor);
            notificationService.sendGuarantorshipDeclinedMessage(guarantor.getPhoneNumber(),
                    guarantor.getGuarantorName(), memberWrapper.getPhonenumber(),
                    memberWrapper.getFirstname(), guarantor.getAmount(), memberWrapper.getLanguage());

            chamaKycService.getFluxMembersLanguageAndPhonesInGroup(loanApplication.getLoanProducts().getGroupId())
                    .toStream()
                    .filter(pair -> !Objects.equals(pair.getFirst(), memberWrapper.getPhonenumber()))
                    .forEach(pair -> notificationService.sendGroupGuarantorshipDeclinedMessage(
                            pair.getFirst(),
                            guarantor.getGuarantorName(),
                            memberWrapper.getPhonenumber(),
                            memberName, guarantor.getAmount(),
                            pair.getSecond()));
        }
    }

    @Override
    public Mono<UniversalResponse> getUserDeclinedGuarantorLoans(String phoneNumber) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);
            if (member == null) {
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));
            }
            List<Guarantors> guarantorList = guarantorsRepository.findAllByMemberIdAndLoanStatus(member.getId(), LoanStatusEnum.DECLINED.name());
            List<LoanApplications> loanApplicationsList = guarantorList.stream()
                    .map(guarantor -> loanapplicationsRepo.findById(guarantor.getLoanId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            List<GuarantorLoansWrapper> declinedGuarantorLoansWrapperList = loanApplicationsList.stream()
                    .map(loans -> {
                        List<Guarantors> guarantors = guarantorsRepository.findAllByLoanIdAndLoanStatus(loans.getId(), LoanStatusEnum.DECLINED.name());
                        double totalApproved = guarantors.stream()
                                .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.APPROVED.name()))
                                .mapToDouble(Guarantors::getAmount)
                                .sum();
                        double totalDeclined = guarantors.stream()
                                .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.DECLINED.name()))
                                .mapToDouble(Guarantors::getAmount)
                                .sum();
                        double totalPendingApproval = guarantors.stream()
                                .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.PENDING_APPROVAL.name()))
                                .mapToDouble(Guarantors::getAmount)
                                .sum();
                        return GuarantorLoansWrapper.builder()
                                .guarantorsList(guarantors)
                                .loanApplications(loans)
                                .totalLoanAmount(loans.getAmount())
                                .totalPendingApprovalAmount(totalPendingApproval)
                                .totalApprovedAmount(totalApproved)
                                .totalDeclinedAmount(totalDeclined)
                                .remainingAmount(loans.getAmount() - totalApproved)
                                .build();
                    })
                    .collect(Collectors.toList());
            return new UniversalResponse("success", getResponseMessage("userDeclinedGuarantorLoans"), declinedGuarantorLoansWrapperList);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanGuarantors(Long loanId) {
        return Mono.fromCallable(() -> {
            Optional<LoanApplications> loanApplicationsOptional = loanapplicationsRepo.findById(loanId);
            if (loanApplicationsOptional.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotFound"));
            }
            LoanApplications loanApplications = loanApplicationsOptional.get();
            List<Guarantors> guarantors = guarantorsRepository.findAllByLoanIdAndLoanStatus(loanApplications.getId(), LoanStatusEnum.DECLINED.name());
            double totalApproved = guarantors.stream()
                    .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.APPROVED.name()))
                    .mapToDouble(Guarantors::getAmount)
                    .sum();
            double totalDeclined = guarantors.stream()
                    .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.DECLINED.name()))
                    .mapToDouble(Guarantors::getAmount)
                    .sum();
            double totalPendingApproval = guarantors.stream()
                    .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.PENDING_APPROVAL.name()))
                    .mapToDouble(Guarantors::getAmount)
                    .sum();
            GuarantorLoansWrapper loansWrapper = GuarantorLoansWrapper.builder()
                    .guarantorsList(guarantors)
                    .loanApplications(loanApplications)
                    .totalApprovedAmount(totalApproved)
                    .totalLoanAmount(loanApplications.getAmount())
                    .totalPendingApprovalAmount(totalPendingApproval)
                    .totalDeclinedAmount(totalDeclined)
                    .remainingAmount(loanApplications.getAmount() - totalApproved)
                    .build();
            return new UniversalResponse("success", getResponseMessage("loanGuarantorDetails"), loansWrapper);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> createLoanProduct(LoanproductWrapper loanproductWrapper, String createdBy) {
        return Mono.fromCallable(() -> {
            if (loanproductWrapper.getMax_principal() <= loanproductWrapper.getMin_principal())
                return new UniversalResponse("fail", getResponseMessage("principalNotValid"));

            if (loanproductWrapper.getPaymentperiodtype() == null || loanproductWrapper.getPaymentperiodtype().isBlank())
                return new UniversalResponse("fail", getResponseMessage("loanProductPaymentPeriodNotFound"), Collections.emptyList());

            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(loanproductWrapper.getGroupid());
            if (groupWrapper == null) {
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));
            }
            Optional<Contributions> optionalContribution = contributionRepository.findById(loanproductWrapper.getContributionid());
            if (optionalContribution.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));

            //make sure the group does not duplicate the product
            if (loanproductsRepository.countByGroupIdAndProductname(loanproductWrapper.getGroupid(), loanproductWrapper.getProductname()) > 0) {
                return new UniversalResponse("fail", getResponseMessage("loanProductExists"));
            }
            Optional<Accounts> accountsOptional = accountsRepository.findFirstByGroupId(groupWrapper.getId());
            if (accountsOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupAccountNotFound"));

            LoanProducts loanProducts = new LoanProducts();
            loanProducts.setDescription(loanproductWrapper.getDescription());
            loanProducts.setGroupId(loanproductWrapper.getGroupid());
            loanProducts.setInteresttype(loanproductWrapper.getInteresttype());
            loanProducts.setInterestvalue(loanproductWrapper.getInterestvalue());
            loanProducts.setMax_principal(loanproductWrapper.getMax_principal());
            loanProducts.setMin_principal(loanproductWrapper.getMin_principal());
            loanProducts.setProductname(loanproductWrapper.getProductname());
            loanProducts.setPaymentperiod(loanproductWrapper.getPaymentperiod());
            loanProducts.setPaymentperiodtype(loanproductWrapper.getPaymentperiodtype());
            loanProducts.setContributions(optionalContribution.get());
            loanProducts.setGuarantor(loanproductWrapper.getIsguarantor());
            loanProducts.setPenalty(loanproductWrapper.getHasPenalty());
            loanProducts.setPenaltyValue(loanproductWrapper.getPenaltyvalue());
            loanProducts.setIsPercentagePercentage(loanproductWrapper.getIspenaltypercentage());
            loanProducts.setUserSavingValue(loanproductWrapper.getUsersavingvalue());
            loanProducts.setDebitAccountId(accountsOptional.get());
            loanProducts.setGracePeriod(loanproductWrapper.getGracePeriod());
            loanProducts.setIsActive(true);
            loanProducts.setPenaltyPeriod(loanproductWrapper.getPenaltyPeriod());
            LoanProducts savedLoanProduct = loanproductsRepository.save(loanProducts);

            sendLoanProductCreatedMessage(createdBy, savedLoanProduct, groupWrapper);
            return new UniversalResponse("success", getResponseMessage("loanProductAdded"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendLoanProductCreatedMessage(String createdBy, LoanProducts savedLoanProduct, GroupWrapper groupWrapper) {
        MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(createdBy);
        String memberName = String.format("%s %s", member.getFirstname(), member.getLastname());

        chamaKycService.getFluxGroupMembers(savedLoanProduct.getGroupId())
                .subscribe(memberWrapper -> notificationService.sendLoanProductCreated(memberName, savedLoanProduct.getProductname(), groupWrapper.getName(), memberWrapper.getPhonenumber(), memberWrapper.getLanguage()));
    }

    @Override
    public Mono<UniversalResponse> editLoanProduct(LoanproductWrapper loanproductWrapper, String approvedBy) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProducts = loanproductsRepository.findById(loanproductWrapper.getProductid());
            if (optionalLoanProducts.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"), Collections.emptyList());

            if (loanproductWrapper.getPaymentperiodtype() == null || loanproductWrapper.getPaymentperiodtype().isBlank())
                return new UniversalResponse("fail", getResponseMessage("loanProductPaymentPeriodNotFound"), Collections.emptyList());

            LoanProducts loanProduct = optionalLoanProducts.get();
            loanProduct.setDescription(loanproductWrapper.getDescription());
            loanProduct.setInteresttype(loanproductWrapper.getInteresttype());
            loanProduct.setInterestvalue(loanproductWrapper.getInterestvalue());
            loanProduct.setMax_principal(loanproductWrapper.getMax_principal());
            loanProduct.setMin_principal(loanproductWrapper.getMin_principal());
            loanProduct.setProductname(loanproductWrapper.getProductname());
            loanProduct.setPaymentperiod(loanproductWrapper.getPaymentperiod());
            loanProduct.setGracePeriod(loanproductWrapper.getGracePeriod());
            loanProduct.setPaymentperiodtype(loanproductWrapper.getPaymentperiodtype());
            loanProduct.setGuarantor(loanproductWrapper.getIsguarantor());
            loanProduct.setPenalty(loanproductWrapper.getHasPenalty());
            loanProduct.setIsPercentagePercentage(loanproductWrapper.getIspenaltypercentage());
            loanProduct.setUserSavingValue(loanproductWrapper.getUsersavingvalue());
            loanProduct.setPenaltyPeriod(loanproductWrapper.getPenaltyPeriod());
            loanProduct.setIsActive(loanproductWrapper.getIsActive());
            loanProduct.setPenaltyValue(loanproductWrapper.getPenaltyvalue());
            LoanProducts savedLoanProduct = loanproductsRepository.save(loanProduct);

            sendLoanProductEditedMessage(approvedBy, savedLoanProduct);
            return new UniversalResponse("success", getResponseMessage("loanProductEditedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendLoanProductEditedMessage(String approvedBy, LoanProducts savedLoanProduct) {
        MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(approvedBy);
        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(savedLoanProduct.getGroupId());
        String memberName = String.format("%s %s", member.getFirstname(), member.getLastname());

        double penaltyValue = savedLoanProduct.getIsPercentagePercentage() ?
                savedLoanProduct.getPenaltyValue() / (double) 100 : savedLoanProduct.getPenaltyValue();

        chamaKycService.getFluxGroupMembers()
                .filter(gm -> !gm.getPhoneNumber().equals(approvedBy))
                .map(gm -> chamaKycService.getMemberDetailsById(gm.getMemberId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .subscribe(memberWrapper -> notificationService.sendLoanProductEdited(memberName,
                        savedLoanProduct.getProductname(), groupWrapper.getName(), savedLoanProduct.getMax_principal(),
                        savedLoanProduct.getMin_principal(), savedLoanProduct.getUserSavingValue(),
                        (int) penaltyValue, memberWrapper.getPhonenumber(), memberWrapper.getLanguage()));
    }

    @Override
    public Mono<UniversalResponse> activateDeactivateLoanProduct(LoanproductWrapper loanproductWrapper, String currentUser, boolean activate) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProducts = loanproductsRepository.findById(loanproductWrapper.getProductid());
            if (optionalLoanProducts.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));

            LoanProducts loanProduct = optionalLoanProducts.get();

            if (!activate && !loanProduct.getIsActive())
                return new UniversalResponse("success", getResponseMessage("loanProductIsDeactivated"));

            if (activate && loanProduct.getIsActive())
                return new UniversalResponse("success", getResponseMessage("loanProductIsDeactivated"));

            loanProduct.setIsActive(activate);
            LoanProducts savedLoanProduct = loanproductsRepository.save(loanProduct);
            sendLoanActivatedOrDeactivatedMessage(savedLoanProduct, activate, currentUser);
            return new UniversalResponse("success", activate ? getResponseMessage("loanProductActivatedSuccessfully") : getResponseMessage("loanProductDeactivatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendLoanActivatedOrDeactivatedMessage(LoanProducts savedLoanProduct, boolean activate, String currentUser) {
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(currentUser);
        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(savedLoanProduct.getGroupId());

        if (memberWrapper == null) {
            log.info("Member not found on send loan product activation...");
            return;
        }

        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        chamaKycService.getFluxGroupMembers()
                .filter(gm -> !gm.getPhoneNumber().equals(currentUser))
                .map(gm -> chamaKycService.getMemberDetailsById(gm.getMemberId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .subscribe(member -> {
                    if (activate) {
                        notificationService.sendLoanProductActivatedMessage(memberName, savedLoanProduct.getProductname(), groupWrapper.getName(), member.getPhonenumber(), member.getLanguage());
                        return;
                    }
                    notificationService.sendLoanProductDeactivatedMessage(memberName, savedLoanProduct.getProductname(), groupWrapper.getName(), member.getPhonenumber(), member.getLanguage());
                });
    }

    @Override
    public Mono<UniversalResponse> getLoanProductsbyGroup(long groupid) {
        return Mono.fromCallable(() -> {
            GroupWrapper optionalGroup = chamaKycService.getMonoGroupById(groupid);

            if (optionalGroup == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            List<LoanproductWrapper> loanProductsList = loanproductsRepository.findAllByGroupId(groupid)
                    .stream()
                    .map(p -> {
                        return mapToLoanProductWrapper(optionalGroup, p);
                    })
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanProductsPerGroup"),
                    loanProductsList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanProductsList.size());
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private double loanLimit(LoanProducts loanProducts) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        try {
            int userSavingValue = loanProducts.getUserSavingValue();

            int limit = checkLoanLimit(auth.getName(), loanProducts.getContributions().getId());

            limit = (userSavingValue * limit) / 100;

            return limit;
        } catch (Exception exception) {
            return 0.0;
        }
    }

    private double loanLimit(LoanProducts loanProducts, String appliedBy) {
        try {
            int userSavingValue = loanProducts.getUserSavingValue();

            int limit = checkLoanLimit(appliedBy, loanProducts.getContributions().getId());

            limit = (userSavingValue * limit) / 100;

            return limit;
        } catch (Exception exception) {
            return 0.0;
        }
    }

    @Override
    public Mono<UniversalResponse> applyLoan(ApplyLoanWrapper applyLoanWrapper, String appliedBy) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProduct = loanproductsRepository.findById(applyLoanWrapper.getLoanproduct());
            if (optionalLoanProduct.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));

            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(appliedBy);
            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            LoanProducts loanProducts = optionalLoanProduct.get();
            if (!loanProducts.getIsActive())
                return new UniversalResponse("fail", getResponseMessage("loanProductIsNotActive"), Collections.emptyList());

            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(loanProducts.getGroupId());
            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            double amount = applyLoanWrapper.getAmount();
            if (amount > loanProducts.getMax_principal() || amount < loanProducts.getMin_principal())
                return new UniversalResponse("fail", getResponseMessage("loanAmountIsBeyondLimit"));

            double loanLimit = loanLimit(loanProducts, appliedBy);
            if (amount > loanLimit)
                return new UniversalResponse("fail", getResponseMessage("loanIsAboveLoanLimit"), Collections.emptyList());

            List<LoansDisbursed> loansDisbursedList = loansdisbursedRepo.findUserPendingLoansInLoanProduct(memberWrapper.getId(), loanProducts.getId());
            if (!loansDisbursedList.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("youHaveAnExistingLoanApplication"), Collections.emptyList());

            String accountToDeposit = applyLoanWrapper.getCoreAccount().isBlank() ? appliedBy : applyLoanWrapper.getCoreAccount();
            return handleLoanApplication(amount, applyLoanWrapper.getReminder(), applyLoanWrapper.getGuarantors(), loanProducts, memberWrapper, groupWrapper, accountToDeposit);
        }).publishOn(Schedulers.boundedElastic());

    }

    private UniversalResponse handleLoanApplication(double loanAmount, Map<String, Object> reminder,
                                                    List<Guarantors> guarantorsList, LoanProducts loanProducts,
                                                    MemberWrapper memberWrapper, GroupWrapper groupWrapper, String accountToDeposit) {
        LoanApplications loanApplications = new LoanApplications();
        loanApplications.setAmount(loanAmount);
        loanApplications.setMemberId(memberWrapper.getId());
        loanApplications.setPending(true);
        loanApplications.setAccountToDeposit(accountToDeposit);
        loanApplications.setUsingWallet(accountToDeposit.length() <= 12);
        loanApplications.setApprovalCount(0);
        loanApplications.setLoanProducts(loanProducts);
        loanApplications.setApprovedby(new JsonObject().toString());
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        String member_reminder = gson.toJson(reminder, type);
        loanApplications.setReminder(member_reminder);
        loanApplications.setUnpaidloans(0);
        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        if (guarantorsList != null) {
            try {
                StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
                checkIfGuarantorQualify(loanApplications, loanProducts.getContributions().getId(), guarantorsList, stringJoiner);
                guarantorsRepository.saveAll(guarantorsList);
                // send guarantor's messages
                List<String> phoneNumbers = guarantorsList.stream().map(Guarantors::getPhoneNumber).collect(Collectors.toList());
                notificationService.sendGuarantorsInviteMessage(phoneNumbers, loanAmount, memberName, memberWrapper.getLanguage());
            } catch (Exception ex) {
                return new UniversalResponse("fail", ex.getMessage());
            }
        }
        LoanApplications savedLoanApplication = loanapplicationsRepo.save(loanApplications);
        sendLoanApplicationSmsToOfficials(savedLoanApplication, memberName);
        return UniversalResponse.builder()
                .status("success")
                .message(String.format(getResponseMessage("successfulLoanApplication"), loanAmount, groupWrapper.getName().toUpperCase()))
                .build();
    }

    @Async
    public void sendLoanApplicationSmsToOfficials(LoanApplications loanApplication, String memberName) {
        GroupWrapper group = chamaKycService.getMonoGroupById(loanApplication.getLoanProducts().getGroupId());

        if (group != null && group.isActive()) {
            log.info("Sending loan application notification to officials...");
            chamaKycService.getGroupOfficials(group.getId())
                    .subscribe(member -> notificationService.sendLoanApplicationSms(member.getFirstname(), memberName, group.getName(), loanApplication.getAmount(), member.getPhonenumber(), member.getLanguage()));
        } else {
            log.error("Could not send Loan Application SMS. Group not found.");
        }
    }

    public boolean checkAllGuarantorsApproved(Long loanId) {
        Optional<LoanApplications> contributionLoanOptional = loanapplicationsRepo.findById(loanId);
        List<Guarantors> guarantorsList = guarantorsRepository.findGuarantorsByLoanId(loanId);
        if (contributionLoanOptional.isEmpty()) {
            return false;
        }
        if (guarantorsList.isEmpty()) {
            return false;
        }
        LoanApplications contributionLoan = contributionLoanOptional.get();
        double totalLoan = contributionLoan.getAmount();
        double amountGuaranteed = guarantorsList
                .stream()
                .filter(guarantor -> guarantor.getLoanStatus().equalsIgnoreCase(LoanStatusEnum.APPROVED.name()))
                .mapToDouble(Guarantors::getAmount)
                .sum();
        return amountGuaranteed >= totalLoan;
    }

    @Override
    public Mono<UniversalResponse> getLoansPendingApprovalbyGroup(long groupid, int page, int size) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupid);
            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            Pageable pageable = PageRequest.of(page, size);
            List<LoanApplications> loanApplicationsList = loanapplicationsRepo.getApplicationsbygroup(groupid, pageable);

            List<LoansPendingApprovalWrapper> loansPendingApproval = loanApplicationsList
                    .stream()
                    .map(loan -> {
                        MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                        if (member == null) return null;

                        LoansPendingApprovalWrapper loansPendingApprovalWrapper = new LoansPendingApprovalWrapper();
                        loansPendingApprovalWrapper.setAmount(loan.getAmount());
                        loansPendingApprovalWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                        loansPendingApprovalWrapper.setAppliedon(loan.getCreatedOn());
                        loansPendingApprovalWrapper.setLoanproductid(loan.getLoanProducts().getId());
                        loansPendingApprovalWrapper.setLoanproductname(loan.getLoanProducts().getProductname());
                        loansPendingApprovalWrapper.setMemberphonenumber(member.getPhonenumber());
                        loansPendingApprovalWrapper.setLoanapplicationid(loan.getId());
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> member_reminder = gson.fromJson(loan.getReminder(), type);
                        loansPendingApprovalWrapper.setReminder(member_reminder);
                        loansPendingApprovalWrapper.setUnpaidloans(loan.getUnpaidloans());
                        return loansPendingApprovalWrapper;
                    }).collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansPendingApprovalPerGroup"), loansPendingApproval);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanapplicationsRepo.countApplicationsbyGroup(groupid));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoansPendingApprovalbyUser(String phonenumber, int page, int size) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phonenumber);

            if (member == null) {
                return Mono.just(new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND)));
            }

            Pageable pageable = PageRequest.of(page, size);
            List<LoanApplications> loanApplicationsList = loanapplicationsRepo.findByMemberIdAndPendingTrueOrderByCreatedOnDesc(member.getId(), pageable);
            List<LoansPendingApprovalWrapper> loansPendingApprovalWrapperList = loanApplicationsList
                    .stream()
                    .map(loans -> {
                        LoansPendingApprovalWrapper loansPendingApprovalWrapper = new LoansPendingApprovalWrapper();
                        loansPendingApprovalWrapper.setAmount(loans.getAmount());
                        loansPendingApprovalWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                        loansPendingApprovalWrapper.setAppliedon(loans.getCreatedOn());
                        loansPendingApprovalWrapper.setLoanproductid(loans.getLoanProducts().getId());
                        loansPendingApprovalWrapper.setLoanproductname(loans.getLoanProducts().getProductname());
                        loansPendingApprovalWrapper.setMemberphonenumber(member.getPhonenumber());
                        loansPendingApprovalWrapper.setLoanapplicationid(member.getId());
                        loansPendingApprovalWrapper.setGuarantor(loans.getLoanProducts().isGuarantor());
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> member_reminder = gson.fromJson(loans.getReminder(), type);
                        loansPendingApprovalWrapper.setReminder(member_reminder);
                        loansPendingApprovalWrapper.setUnpaidloans(loans.getUnpaidloans());
                        return loansPendingApprovalWrapper;
                    })
                    .collect(Collectors.toList());
            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansPendingApprovalPerUser"),
                    loansPendingApprovalWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanapplicationsRepo.countByMemberIdAndPendingTrue(member.getId()));
            response.setMetadata(metadata);
            return Mono.just(response);
        }).publishOn(Schedulers.boundedElastic()).flatMap(res -> res);
    }

    @Override
    public Mono<UniversalResponse> getLoansPendingApprovalbyLoanProduct(long loanproductid, String currentUser, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProduct = loanproductsRepository.findById(loanproductid);
            if (optionalLoanProduct.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));
            }
            Pageable pageable = PageRequest.of(page, size);
            List<LoanApplications> loanApplicationsList = loanapplicationsRepo.findByLoanProductsAndPendingTrueOrderByCreatedOnDesc(optionalLoanProduct.get(), pageable);
            List<LoansPendingApprovalWrapper> loansPendingApprovalWrapperList = loanApplicationsList
                    .stream()
                    .filter(loan -> checkAllGuarantorsApproved(loan.getId()))
                    .map(loan -> {
                        MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                        if (member == null) return null;
                        LoansPendingApprovalWrapper loansPendingApprovalWrapper = new LoansPendingApprovalWrapper();
                        loansPendingApprovalWrapper.setAmount(loan.getAmount());
                        loansPendingApprovalWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                        loansPendingApprovalWrapper.setAppliedon(loan.getCreatedOn());
                        loansPendingApprovalWrapper.setLoanproductid(loan.getLoanProducts().getId());
                        loansPendingApprovalWrapper.setLoanproductname(loan.getLoanProducts().getProductname());
                        loansPendingApprovalWrapper.setMemberphonenumber(member.getPhonenumber());
                        loansPendingApprovalWrapper.setLoanapplicationid(loan.getId());
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> member_reminder = gson.fromJson(loan.getReminder(), type);
                        loansPendingApprovalWrapper.setReminder(member_reminder);
                        loansPendingApprovalWrapper.setUnpaidloans(loan.getUnpaidloans());
                        return loansPendingApprovalWrapper;
                    }).collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansPendingApprovalPerProduct"),
                    loansPendingApprovalWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanapplicationsRepo.countAllByLoanProductsAndPendingTrue(optionalLoanProduct.get()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> approvedeclineLoanApplication(boolean approve, long loanapplicationid, long debitaccountid, String approvedby) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(approvedby);
            if (member == null) return new UniversalResponse("fail", "Member not found");
            Optional<LoanApplications> optionalLoanApplication = loanapplicationsRepo.findById(loanapplicationid);
            if (optionalLoanApplication.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotFound"));
            }
            LoanApplications loanApplications = optionalLoanApplication.get();
            if (!loanApplications.isPending()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotPending"));
            }
            if (member.getId() == loanApplications.getMemberId()) {
                return new UniversalResponse("fail", getResponseMessage("cannotApproveSelfLoanApplication"), Collections.emptyList());
            }
            MemberWrapper loanedMember = chamaKycService.getMonoMemberDetailsById(loanApplications.getMemberId());
            if (loanedMember == null)
                return new UniversalResponse("fail", getResponseMessage("loanedMemberNotFound"));
            if (approve) {
                Accounts accounts = loanApplications.getLoanProducts().getDebitAccountId();
                if (accounts.getAccountbalance() < loanApplications.getAmount()) {
                    return new UniversalResponse("fail", getResponseMessage("accountBalanceIsInsufficient"));
                }
                WithdrawalLogs withdrawalLogs = new WithdrawalLogs();
                withdrawalLogs.setCapturedby(loanedMember.getPhonenumber());
                withdrawalLogs.setContribution_narration(String.format("loan disbursement for member %s",
                        loanedMember.getFirstname().concat(" ").concat(loanedMember.getLastname())));
                withdrawalLogs.setContributions(loanApplications.getLoanProducts().getContributions());
                withdrawalLogs.setCreditphonenumber(loanedMember.getPhonenumber());
                withdrawalLogs.setDebitAccounts(accounts);
                withdrawalLogs.setTransamount(loanApplications.getAmount());
                withdrawalLogs.setUniqueTransactionId("LA_" + accounts.getAccountType().getAccountPrefix().concat(String.valueOf(new Date().getTime())));
                withdrawalLogs.setWithdrawalreason("loan application");
                withdrawalLogs.setLoanApplications(loanApplications);
                if (loanApplications.getLoanProducts().getDebitAccountId().getAccountType().getId() == 1) {
                    routerService.makeB2CwithdrawalRequest(loanedMember.getPhonenumber(),
                            loanApplications.getAmount(), withdrawalLogs.getUniqueTransactionId());
                    withdrawallogsRepo.save(withdrawalLogs);
                } else {
                    logLoanApplication(withdrawalLogs, accounts, loanApplications);
                }
            }
            if (approve) {
                notificationService.sendLoanApprovedMessage(loanedMember.getPhonenumber(), loanedMember.getFirstname(), loanApplications.getAmount(), loanedMember.getLanguage());
            } else {
                notificationService.sendLoanDeclinedMessage(loanedMember.getPhonenumber(), loanedMember.getFirstname(), loanApplications.getAmount(), loanedMember.getLanguage());
            }
            loanApplications.setPending(false);
            loanApplications.setApprovedby(approvedby);
            loanApplications.setStatus("Approved");
            loanapplicationsRepo.save(loanApplications);
            return new UniversalResponse("success", approve ? getResponseMessage("loanHasBeenApproved") : getResponseMessage("loanHasBeenDeclined"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> approveLoanApplication(boolean approve, long loanApplicationId, String approvedBy) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(approvedBy);
            if (member == null) return new UniversalResponse("fail", "Member not found");
            Optional<LoanApplications> optionalLoanApplication = loanapplicationsRepo.findById(loanApplicationId);
            if (optionalLoanApplication.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotFound"));
            }
            LoanApplications loanApplications = optionalLoanApplication.get();
            if (!loanApplications.isPending()) {
                return new UniversalResponse("fail", getResponseMessage("loanApplicationNotPending"));
            }
            long memberId = loanApplications.getMemberId();
            if (member.getId() == memberId) {
                return new UniversalResponse("fail", getResponseMessage("cannotApproveSelfLoanApplication"), Collections.emptyList());
            }
            MemberWrapper loanedMember = chamaKycService.getMonoMemberDetailsById(memberId);
            if (loanedMember == null)
                return new UniversalResponse("fail", getResponseMessage("loanedMemberNotFound"));

            long groupId = loanApplications.getLoanProducts().getGroupId();
            GroupMemberWrapper groupMemberWrapper = chamaKycService.getMonoGroupMembershipByGroupIdAndMemberId(groupId, member.getId());
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupId);

            if (groupMemberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("loanApproverNotInGroup"));

            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            List<Accounts> accounts = accountsRepository.findByGroupIdAndActive(groupMemberWrapper.getGroupId(), true);

            Accounts account = accounts.get(0);

            if (account.getAccountbalance() < loanApplications.getAmount())
                return new UniversalResponse("fail", getResponseMessage("accountBalanceIsInsufficient"));

            if (!approve) {
                notificationService.sendLoanDeclinedMessage(loanedMember.getPhonenumber(),
                        loanedMember.getFirstname(), loanApplications.getAmount(), loanedMember.getLanguage());
                loanApplications.setPending(false);
                loanapplicationsRepo.save(loanApplications);
                return new UniversalResponse("success", getResponseMessage("loanRequestDeclinedSuccessfully"));
            }

            JsonObject approvals = gson.fromJson(loanApplications.getApprovedby(), JsonObject.class);

            if (approvals.has(groupMemberWrapper.getTitle()))
                return new UniversalResponse("fail", getResponseMessage("youCannotAuthorizeMoreThanOnce"));

            approvals.addProperty(groupMemberWrapper.getTitle(), member.getId());
            loanApplications.setApprovalCount(loanApplications.getApprovalCount() + 1);
            loanApplications.setApprovedby(approvals.toString());

            if (loanApplications.getApprovalCount() > 1) {
                // disburse the money
                disburseLoan(groupWrapper.getCsbAccount(), loanApplications.getAmount(), loanApplications.getAccountToDeposit(), loanApplications.getId());

                // send sms
                notificationService.sendLoanApprovedMessage(loanedMember.getPhonenumber(), loanedMember.getFirstname(),
                        loanApplications.getAmount(), loanedMember.getLanguage());

                loanApplications.setStatus("Approved");
                loanApplications.setPending(false);
                loanApplications.setStatus(PaymentEnum.PAYMENT_PENDING.name());
                loanApplications.setApproved(true);
                loanapplicationsRepo.save(loanApplications);
                saveWithdrawalLog(loanApplications, account, loanedMember);
                return new UniversalResponse("success", getResponseMessage("approvalDone"));
            }

            loanApplications.setStatus(PaymentEnum.PAYMENT_PENDING.name());
            loanApplications.setPending(true);
            loanapplicationsRepo.save(loanApplications);
            return new UniversalResponse("success", getResponseMessage("loanApprovalSuccessful"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void disburseLoan(String debitAccountId, Double amount, String phoneNumber, long loanApplicationId) {
        String transactionId = TransactionIdGenerator.generateTransactionId("LDM");
        Map<String, String> esbRequest = constructBody(debitAccountId, phoneNumber, amount.intValue(), loanApplicationId, Optional.empty(), transactionId, "LD");
        String body = gson.toJson(esbRequest);

        webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(t -> log.error("Error occurred while disbursing a loan... Reason => {}", t.getLocalizedMessage()))
                .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                .subscribe(res -> log.info("Loan disbursement response... {}", res));
    }

    public void saveWithdrawalLog(LoanApplications loanApplications, Accounts accounts, MemberWrapper loanedMember) {
        WithdrawalLogs withdrawalLogs = new WithdrawalLogs();
        withdrawalLogs.setCapturedby(loanedMember.getPhonenumber());
        withdrawalLogs.setContribution_narration(String.format("loan disbursement for member %s",
                loanedMember.getFirstname().concat(" ").concat(loanedMember.getLastname())));
        withdrawalLogs.setContributions(loanApplications.getLoanProducts().getContributions());
        withdrawalLogs.setCreditphonenumber(loanedMember.getPhonenumber());
        withdrawalLogs.setDebitAccounts(accounts);
        withdrawalLogs.setTransamount(loanApplications.getAmount());
        withdrawalLogs.setUniqueTransactionId("LA_" + accounts.getAccountType().getAccountPrefix().concat(String.valueOf(new Date().getTime())));
        withdrawalLogs.setWithdrawalreason("loan application");
        withdrawalLogs.setLoanApplications(loanApplications);
        logLoanApplication(withdrawalLogs, accounts, loanApplications);
    }

    public void logLoanApplication(WithdrawalLogs withdrawalLogs, Accounts accounts, LoanApplications loanApplications) {
        withdrawalLogs.setNewbalance(accounts.getAccountbalance() - loanApplications.getAmount());
        withdrawalLogs.setOldbalance(accounts.getAccountbalance());
        LoansDisbursed loansDisbursed = new LoansDisbursed();
        LoanProducts loanProducts = loanApplications.getLoanProducts();
        String interesttype = loanProducts.getInteresttype();
        double interestvalue = loanProducts.getInterestvalue();
        int period = loanProducts.getPaymentperiod();
        if (loanProducts.getPaymentperiodtype().equalsIgnoreCase("year")) {
            period = period * 12;
        }
        double interest = calculateLoanInterest(interesttype, interestvalue, loanApplications.getAmount(), period);
        withdrawallogsRepo.save(withdrawalLogs);
        accountsRepository.save(accounts);
        //update disbursed loan
        loansDisbursed.setInterest(interest);
        loansDisbursed.setPrincipal(loanApplications.getAmount());
        loansDisbursed.setDueamount(interest + loanApplications.getAmount());
        loansDisbursed.setLoanApplications(loanApplications);
        loansDisbursed.setPaymentPeriodType(loanProducts.getPaymentperiodtype());
        loansDisbursed.setWithdrawalLogs(withdrawalLogs);
        loansDisbursed.setGroupId(accounts.getGroupId());
        loansDisbursed.setMemberId(loanApplications.getMemberId());
        loansDisbursed.setStatus(PaymentEnum.YET_TO_PAY.name());
        loansdisbursedRepo.save(loansDisbursed);
    }

    private double calculateLoanInterest(String interesttype, double interestvalue, double amount, int period) {
        double interest = 0;
        if (interesttype.toLowerCase().contains("simple")) {
            interest = Math.ceil((amount * interestvalue * period / 12) / 100);
        } else if (interesttype.toLowerCase().contains("compound")) {
            interestvalue = Math.ceil((1 + interestvalue / 100));

            interestvalue = Math.ceil(Math.pow(interestvalue, period / 12));

            double totalamount = amount * interestvalue;
            interest = totalamount - amount;
        } else if (interesttype.toLowerCase().contains("flat")) {
            interest = interestvalue;
        }
        return interest;
    }

    @Override
    public Mono<UniversalResponse> getDisbursedLoansperGroup(long groupid, int page, int size) {
        return Mono.fromCallable(() -> {
            GroupWrapper group = chamaKycService.getMonoGroupById(groupid);
            if (group == null) return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));
            Pageable pageable = PageRequest.of(page, size);
            List<DisbursedloansWrapper> disbursedloansWrapperList =
                    loansdisbursedRepo.findByGroupId(group.getId(), pageable).stream()
                            .map(p -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (member == null) return null;
                                DisbursedloansWrapper disbursedloansWrapper = new DisbursedloansWrapper();
                                disbursedloansWrapper.setContributionid(p.getLoanApplications().getLoanProducts().getContributions().getId());
                                disbursedloansWrapper.setContributionname(p.getLoanApplications().getLoanProducts().getContributions().getName());
                                disbursedloansWrapper.setDueamount(Math.ceil(p.getDueamount()));
                                disbursedloansWrapper.setGroupid(group.getId());
                                disbursedloansWrapper.setGroupname(group.getName());
                                disbursedloansWrapper.setPaymentperiodtype(p.getPaymentPeriodType());
                                disbursedloansWrapper.setApprovedon(p.getCreatedOn());
                                disbursedloansWrapper.setDuedate(p.getDuedate());
                                disbursedloansWrapper.setInterest(Math.ceil(p.getInterest()));
                                disbursedloansWrapper.setLoanid(p.getId());
                                disbursedloansWrapper.setPrincipal(p.getPrincipal());
                                disbursedloansWrapper.setRecipient(String.format("%s %s", member.getFirstname(), member.getLastname()));
                                disbursedloansWrapper.setRecipientsnumber(member.getPhonenumber());
                                disbursedloansWrapper.setLoanproductname(p.getLoanApplications().getLoanProducts().getProductname());
                                Accounts accounts = p.getLoanApplications().getLoanProducts().getDebitAccountId();
                                AccountWrapper accountWrapper = AccountWrapper.builder()
                                        .accountbalance(accounts.getAccountbalance())
                                        .accountname(accounts.getName())
                                        .accounttypeid(accounts.getAccountType().getId())
                                        .build();
                                disbursedloansWrapper.setDebitAccount(accountWrapper);
                                disbursedloansWrapper.setAccountTypeId(accountWrapper.getAccounttypeid());
                                disbursedloansWrapper.setAppliedon(p.getCreatedOn());
                                disbursedloansWrapper.setGuarantor(p.getLoanApplications().getLoanProducts().isGuarantor());
                                return disbursedloansWrapper;
                            })
                            .collect(Collectors.toList());
            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansDisbursedPerGroup"),
                    disbursedloansWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansdisbursedRepo.countByGroupId(group.getId()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getDisbursedLoansperLoanproduct(long loanproductid, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProducts = loanproductsRepository.findById(loanproductid);
            if (optionalLoanProducts.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"), Collections.emptyList());
            }

            GroupWrapper group = chamaKycService.getMonoGroupById(optionalLoanProducts.get().getGroupId());
            if (group == null)
                return UniversalResponse.builder().status("fail").message(getResponseMessage(GROUP_NOT_FOUND)).build();

            Pageable pageable = PageRequest.of(page, size);
            List<DisbursedloansWrapper> disbursedloansWrapperList =
                    loansdisbursedRepo.findByLoanproduct(loanproductid, pageable).stream()
                            .map(p -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (member == null) return null;
                                DisbursedloansWrapper disbursedloansWrapper = new DisbursedloansWrapper();
                                disbursedloansWrapper.setContributionid(p.getLoanApplications().getLoanProducts().getContributions().getId());
                                disbursedloansWrapper.setContributionname(p.getLoanApplications().getLoanProducts().getContributions().getName());
                                disbursedloansWrapper.setDueamount(Math.ceil(p.getDueamount()));
                                disbursedloansWrapper.setGroupid(group.getId());
                                disbursedloansWrapper.setGroupname(group.getName());
                                disbursedloansWrapper.setDuedate(p.getDuedate());
                                disbursedloansWrapper.setApprovedon(p.getCreatedOn());
                                disbursedloansWrapper.setInterest(Math.ceil(p.getInterest()));
                                disbursedloansWrapper.setLoanid(p.getId());
                                disbursedloansWrapper.setPrincipal(p.getPrincipal());
                                disbursedloansWrapper.setRecipient(String.format("%s %s", member.getFirstname(), member.getLastname()));
                                disbursedloansWrapper.setRecipientsnumber(member.getPhonenumber());
                                disbursedloansWrapper.setLoanproductname(p.getLoanApplications().getLoanProducts().getProductname());
                                Accounts accounts = p.getLoanApplications().getLoanProducts().getDebitAccountId();
                                AccountWrapper accountWrapper = AccountWrapper.builder()
                                        .accountbalance(accounts.getAccountbalance())
                                        .accountname(accounts.getName())
                                        .accounttypeid(accounts.getAccountType().getId())
                                        .build();
                                disbursedloansWrapper.setDebitAccount(accountWrapper);
                                disbursedloansWrapper.setAccountTypeId(accountWrapper.getAccounttypeid());
                                disbursedloansWrapper.setAppliedon(p.getCreatedOn());
                                disbursedloansWrapper.setGuarantor(p.getLoanApplications().getLoanProducts().isGuarantor());
                                return disbursedloansWrapper;
                            })
                            .collect(Collectors.toList());
            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansDisbursedPerProduct"),
                    disbursedloansWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansdisbursedRepo.countLoansDisbursedbyLoanproduct(loanproductid));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getDisbursedLoansperUser(String phonenumber, int page, int size) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phonenumber);
            if (member == null)
                return new UniversalResponse("fail", "Member not found");

            Pageable pageable = PageRequest.of(page, size);
            List<DisbursedloansWrapper> disbursedloansWrapperList =
                    loansdisbursedRepo.findByMemberIdOrderByCreatedOnDesc(member.getId(), pageable).stream()
                            .map(p -> {
                                GroupWrapper group = chamaKycService.getMonoGroupById(p.getGroupId());
                                if (group == null) return null;
                                Optional<WithdrawalLogs> optionalWithdrawalLog = withdrawallogsRepo.findFirstByLoanApplications(p.getLoanApplications());
                                if (optionalWithdrawalLog.isEmpty()) return null;
                                DisbursedloansWrapper disbursedloansWrapper = new DisbursedloansWrapper();
                                disbursedloansWrapper.setTransactionid(optionalWithdrawalLog.get().getUniqueTransactionId());
                                disbursedloansWrapper.setContributionid(p.getLoanApplications().getLoanProducts().getContributions().getId());
                                disbursedloansWrapper.setContributionname(p.getLoanApplications().getLoanProducts().getContributions().getName());
                                disbursedloansWrapper.setDueamount(Math.ceil(p.getDueamount()));
                                disbursedloansWrapper.setGroupid(group.getId());
                                disbursedloansWrapper.setGroupname(group.getName());
                                disbursedloansWrapper.setDuedate(p.getDuedate());
                                disbursedloansWrapper.setApprovedon(p.getCreatedOn());
                                disbursedloansWrapper.setInterest(Math.ceil(p.getInterest()));
                                disbursedloansWrapper.setLoanid(p.getId());
                                disbursedloansWrapper.setPaymentperiodtype(p.getLoanApplications().getLoanProducts().getPaymentperiodtype());
                                disbursedloansWrapper.setPrincipal(p.getPrincipal());
                                disbursedloansWrapper.setRecipient(member.getFirstname().concat(" ").concat(member.getLastname()));
                                disbursedloansWrapper.setRecipientsnumber(member.getPhonenumber());
                                disbursedloansWrapper.setGuarantor(p.getLoanApplications().getLoanProducts().isGuarantor());
                                disbursedloansWrapper.setLoanproductname(p.getLoanApplications().getLoanProducts().getProductname());
                                Accounts accounts = p.getLoanApplications().getLoanProducts().getDebitAccountId();
                                AccountWrapper accountWrapper = new AccountWrapper();
                                accountWrapper.setAccountname(accounts.getName());
                                accountWrapper.setAccounttypeid(accounts.getAccountType().getId());
                                accountWrapper.setAccountbalance(accounts.getAccountbalance());
                                disbursedloansWrapper.setDebitAccount(accountWrapper);
                                disbursedloansWrapper.setAccountTypeId(accountWrapper.getAccounttypeid());
                                disbursedloansWrapper.setGuarantor(p.getLoanApplications().getLoanProducts().isGuarantor());
                                List<LoanPenalty> loanPenaltyList = loanPenaltyRepository.findAllByLoansDisbursedAndDueAmountGreaterThan(p, 0.0);
                                List<LoanPenaltyWrapper> loanPenaltyWrapperList = new ArrayList<>();
                                loanPenaltyList.forEach(loanPenalty -> {
                                    LoanPenaltyWrapper loanPenaltyWrapper = new LoanPenaltyWrapper();
                                    loanPenaltyWrapper.setDueAmount(loanPenalty.getDueAmount());
                                    loanPenaltyWrapper.setLoanPenaltyId(loanPenalty.getId());
                                    loanPenaltyWrapper.setPenaltyAmount(loanPenalty.getPenaltyAmount());
                                    loanPenaltyWrapper.setPaidAmount(loanPenalty.getPaidAmount());
                                    loanPenaltyWrapperList.add(loanPenaltyWrapper);
                                });
                                disbursedloansWrapper.setLoanPenaltyWrapperList(loanPenaltyWrapperList);
                                disbursedloansWrapper.setAppliedon(p.getCreatedOn());
                                return disbursedloansWrapper;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansDisbursedPerUser"),
                    disbursedloansWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansdisbursedRepo.countByMemberId(member.getId()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> recordLoanRepayment(long disbursedloanid, double amount, String receiptnumber, FilePart file, String paidby) {
        return Mono.fromCallable(() -> {
            Optional<LoansDisbursed> optionalLoansDisbursed = loansdisbursedRepo.findById(disbursedloanid);
            if (optionalLoansDisbursed.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanDisbursedNotFound"));
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(paidby);
            if (member == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            LoansDisbursed loansDisbursed = optionalLoansDisbursed.get();
            if (loansDisbursed.getDueamount() < amount) {
                return new UniversalResponse("fail", getResponseMessage("overPaymentNotAllowed"));
            }
            String imageUrl = fileHandlerService.uploadFile(file);
            if (imageUrl.trim().equalsIgnoreCase("")) {
                return new UniversalResponse("fail", "Receipt upload fail");
            }
            LoanRepaymentPendingApproval loanRepaymentPendingApproval = new LoanRepaymentPendingApproval();
            loanRepaymentPendingApproval.setAmount(amount);
            loanRepaymentPendingApproval.setLoansDisbursed(loansDisbursed);
            loanRepaymentPendingApproval.setMemberId(member.getId());
            loanRepaymentPendingApproval.setPending(true);
            loanRepaymentPendingApproval.setReceiptnumber(receiptnumber);
            loanRepaymentPendingApproval.setReceiptImageUrl(imageUrl);
            loanRepaymentPendingApproval.setPaymentType("receipt");
            loanrepaymentpendingapprovalRepo.save(loanRepaymentPendingApproval);

            return new UniversalResponse("success", getResponseMessage("loanPaymentRecordedForApproval"));
        }).publishOn(Schedulers.boundedElastic());
    }


    @Override
    @Transactional
    public Mono<UniversalResponse> approveLoanRepayment(long loanpaymentid, boolean approve, String approvedby) {
        return Mono.fromCallable(() -> {
            Optional<LoanRepaymentPendingApproval> optionalLoanRepaymentPendingApproval = loanrepaymentpendingapprovalRepo.findById(loanpaymentid);
            if (optionalLoanRepaymentPendingApproval.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanRepaymentNotFound"));
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(approvedby);
            if (member == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));
            LoanRepaymentPendingApproval loanRepaymentPendingApproval = optionalLoanRepaymentPendingApproval.get();
            if (member.getId() == loanRepaymentPendingApproval.getMemberId()) {
                return new UniversalResponse("fail", getResponseMessage("cannotApproveOwnLoanRepayment"));
            }
            loanRepaymentPendingApproval.setPending(false);
            loanRepaymentPendingApproval.setApprovedby(approvedby);
            loanRepaymentPendingApproval.setApproved(approve);
            loanrepaymentpendingapprovalRepo.save(loanRepaymentPendingApproval);

            if (approve) {
                LoansRepayment loansRepayment = new LoansRepayment();
                LoansDisbursed loansDisbursed = loanRepaymentPendingApproval.getLoansDisbursed();
                double newdueamount = loansDisbursed.getDueamount() - loanRepaymentPendingApproval.getAmount();
                loansRepayment.setAmount(loanRepaymentPendingApproval.getAmount());
                loansRepayment.setLoansDisbursed(loansDisbursed);
                loansRepayment.setMemberId(loanRepaymentPendingApproval.getMemberId());
                loansRepayment.setNewamount(newdueamount);
                loansRepayment.setOldamount(loansDisbursed.getDueamount());
                loansRepayment.setReceiptnumber(loanRepaymentPendingApproval.getReceiptnumber());
                loansRepayment.setPaymentType(loanRepaymentPendingApproval.getPaymentType());
                loansDisbursed.setDueamount(newdueamount);
                loansdisbursedRepo.save(loansDisbursed);
                loansrepaymentRepo.save(loansRepayment);

                Contributions contributions = loansDisbursed.getLoanApplications().getLoanProducts().getContributions();
                Accounts accounts = loansDisbursed.getLoanApplications().getLoanProducts().getDebitAccountId();
                MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loansDisbursed.getMemberId());
                String phoneNumber = memberWrapper.getPhonenumber();
                TransactionsLog transactionsLog = new TransactionsLog();
                transactionsLog.setContributionNarration("Loan repayment. Member with phone number " + phoneNumber + " paid loan of  " + loanRepaymentPendingApproval.getAmount() + " to contribution  " + contributions.getName());

                transactionsLog.setCreditaccounts(accounts);
                transactionsLog.setDebitphonenumber(String.valueOf(phoneNumber));
                String transid = accounts.getAccountType().getAccountPrefix().concat(String.valueOf(new Date().getTime()));
                transactionsLog.setUniqueTransactionId(transid);
                transactionsLog.setOldbalance(accounts.getAvailableBal());
                transactionsLog.setNewbalance(accounts.getAvailableBal() + loanRepaymentPendingApproval.getAmount());
                transactionsLog.setTransamount(loanRepaymentPendingApproval.getAmount());
                transactionsLog.setCapturedby(approvedby);
                transactionsLog.setApprovedby(approvedby);
                transactionsLog.setContributions(contributions);
                accounts.setAvailableBal(transactionsLog.getNewbalance());

                accountsRepository.save(accounts);
                transactionlogsRepo.save(transactionsLog);
                return new UniversalResponse("success", getResponseMessage("loanRepaymentApproved"));
            }
            return new UniversalResponse("success", getResponseMessage("loanRepaymentDeclined"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentPendingApprovalByUser(String phonenumber, int page, int size) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phonenumber);
            if (member == null)
                return new UniversalResponse("fail", "Member not found");
            Pageable pageable = PageRequest.of(page, size);
            List<LoanpaymentPendingApprovalWrapper> loanpaymentPendingApprovalWrappersList =
                    loanrepaymentpendingapprovalRepo.findByMemberIdAndPendingTrueOrderByCreatedOnDesc(member.getId(), pageable)
                            .stream()
                            .filter(payment -> payment.getMemberId() != member.getId())
                            .map(p -> {
                                MemberWrapper loanedMember = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (loanedMember == null) return null;
                                return LoanpaymentPendingApprovalWrapper.builder()
                                        .dueamount(p.getLoansDisbursed().getDueamount())
                                        .duedate(p.getLoansDisbursed().getDuedate())
                                        .loanid(p.getLoansDisbursed().getId())
                                        .loanproductname(p.getLoansDisbursed().getLoanApplications().getLoanProducts().getProductname())
                                        .membername(String.format("%s %s", loanedMember.getFirstname(), loanedMember.getLastname()))
                                        .memberphone(loanedMember.getPhonenumber())
                                        .receiptnumber(p.getReceiptnumber())
                                        .receiptImageUrl(fileHandlerService.getFileUrl(p.getReceiptImageUrl()))
                                        .loanpaymentid(p.getId())
                                        .transamount(p.getAmount())
                                        .paymentDate(p.getCreatedOn())
                                        .appliedon(p.getLoansDisbursed().getCreatedOn())
                                        .build();
                            }).collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanPaymentsPendingApprovalByUser"),
                    loanpaymentPendingApprovalWrappersList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanrepaymentpendingapprovalRepo.countByMemberIdAndPendingTrue(member.getId()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentPendingApprovalByGroup(long groupid, String currentUser, int page, int size) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupid);
            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));
            Pageable pageable = PageRequest.of(page, size);

            List<LoanpaymentPendingApprovalWrapper> loanpaymentPendingApprovalWrapperList =
                    loanrepaymentpendingapprovalRepo.findpendingbyGroupid(groupid, pageable)
                            .stream()
                            .filter(p -> !p.getPaymentType().equalsIgnoreCase("mpesa"))
                            .map(p -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (member == null) return null;
                                LoanpaymentPendingApprovalWrapper loanpaymentPendingApprovalWrapper = new LoanpaymentPendingApprovalWrapper();
                                loanpaymentPendingApprovalWrapper.setDueamount(p.getLoansDisbursed().getDueamount());
                                loanpaymentPendingApprovalWrapper.setDuedate(p.getLoansDisbursed().getDuedate());
                                loanpaymentPendingApprovalWrapper.setLoanid(p.getLoansDisbursed().getId());
                                loanpaymentPendingApprovalWrapper.setLoanproductname(p.getLoansDisbursed().getLoanApplications().getLoanProducts().getProductname());
                                loanpaymentPendingApprovalWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                                loanpaymentPendingApprovalWrapper.setMemberphone(member.getPhonenumber());
                                loanpaymentPendingApprovalWrapper.setReceiptnumber(p.getReceiptnumber());
                                loanpaymentPendingApprovalWrapper.setTransamount(p.getAmount());
                                loanpaymentPendingApprovalWrapper.setLoanpaymentid(p.getId());
                                loanpaymentPendingApprovalWrapper.setReceiptImageUrl(p.getReceiptImageUrl());
                                loanpaymentPendingApprovalWrapper.setPaymentDate(p.getCreatedOn());
                                loanpaymentPendingApprovalWrapper.setAppliedon(p.getCreatedOn());
                                return loanpaymentPendingApprovalWrapper;
                            })
                            .collect(Collectors.toList());
            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanPaymentsPendingApprovalByGroup"),
                    loanpaymentPendingApprovalWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanrepaymentpendingapprovalRepo.countpendingbyGroupid(groupid));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentsbyUser(String phonenumber, int page, int size) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phonenumber);
            if (member == null)
                return new UniversalResponse("fail", "Member not found");
            Pageable pageable = PageRequest.of(page, size);
            List<LoanpaymentsWrapper> loanpaymentsWrapperList = loansrepaymentRepo.findByMemberIdOrderByCreatedOnDesc(member.getId(), pageable)
                    .stream()
                    .map(p -> {
                        LoanpaymentsWrapper loanpaymentsWrapper = new LoanpaymentsWrapper();
                        loanpaymentsWrapper.setAmount(p.getAmount());
                        loanpaymentsWrapper.setLoandisbursedid(p.getLoansDisbursed().getId());
                        loanpaymentsWrapper.setLoanproductname(p.getLoansDisbursed().getLoanApplications().getLoanProducts().getProductname());
                        loanpaymentsWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                        loanpaymentsWrapper.setMemberphonenumber(member.getPhonenumber());
                        loanpaymentsWrapper.setNewamount(p.getNewamount());
                        loanpaymentsWrapper.setOldamount(p.getOldamount());
                        loanpaymentsWrapper.setReceiptnumber(p.getReceiptnumber());
                        loanpaymentsWrapper.setAppliedon(p.getCreatedOn());
                        return loanpaymentsWrapper;
                    })
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanPaymentsByUser"),
                    loanpaymentsWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansrepaymentRepo.countByMemberId(member.getId()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentsbyGroupid(long groupid, int page, int size) {
        return Mono.fromCallable(() -> {
            GroupWrapper optionalGroup = chamaKycService.getMonoGroupById(groupid);
            if (optionalGroup == null) return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));
            Pageable pageable = PageRequest.of(page, size);
            List<LoanpaymentsWrapper> loanpaymentsWrapperList = loansrepaymentRepo.getloanpaymentsbyGroupid(groupid, pageable)
                    .stream()
                    .map(p -> {
                        MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                        if (member == null) return null;
                        LoanpaymentsWrapper loanpaymentsWrapper = new LoanpaymentsWrapper();
                        loanpaymentsWrapper.setAmount(p.getAmount());
                        loanpaymentsWrapper.setLoandisbursedid(p.getLoansDisbursed().getId());
                        loanpaymentsWrapper.setLoanproductname(p.getLoansDisbursed().getLoanApplications().getLoanProducts().getProductname());
                        loanpaymentsWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                        loanpaymentsWrapper.setMemberphonenumber(member.getPhonenumber());
                        loanpaymentsWrapper.setNewamount(p.getNewamount());
                        loanpaymentsWrapper.setOldamount(p.getOldamount());
                        loanpaymentsWrapper.setReceiptnumber(p.getReceiptnumber());
                        loanpaymentsWrapper.setAppliedon(p.getCreatedOn());
                        return loanpaymentsWrapper;
                    })
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanRepaymentsByGroup"),
                    loanpaymentsWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansrepaymentRepo.countloanpaymentsbyGroupid(groupid));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentsbyDisbursedloan(long disbursedloanid, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<LoansDisbursed> optionalLoansDisbursed = loansdisbursedRepo.findById(disbursedloanid);
            if (optionalLoansDisbursed.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanDisbursedNotFound"));
            LoansDisbursed loansDisbursed = optionalLoansDisbursed.get();
            Pageable pageable = PageRequest.of(page, size);
            List<LoanpaymentsWrapper> loanpaymentsWrapperList =
                    loansrepaymentRepo.findByLoansDisbursedOrderByCreatedOnDesc(loansDisbursed, pageable)
                            .stream()
                            .map(p -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (member == null) return null;
                                LoanpaymentsWrapper loanpaymentsWrapper = new LoanpaymentsWrapper();
                                loanpaymentsWrapper.setAmount(p.getAmount());
                                loanpaymentsWrapper.setLoandisbursedid(p.getLoansDisbursed().getId());
                                loanpaymentsWrapper.setLoanproductname(p.getLoansDisbursed().getLoanApplications().getLoanProducts().getProductname());
                                loanpaymentsWrapper.setMembername(member.getFirstname().concat(" ").concat(member.getLastname()));
                                loanpaymentsWrapper.setMemberphonenumber(member.getPhonenumber());
                                loanpaymentsWrapper.setNewamount(p.getNewamount());
                                loanpaymentsWrapper.setOldamount(p.getOldamount());
                                loanpaymentsWrapper.setReceiptnumber(p.getReceiptnumber());
                                loanpaymentsWrapper.setAppliedon(p.getCreatedOn());
                                loanpaymentsWrapper.setTrxdate(p.getCreatedOn());
                                return loanpaymentsWrapper;
                            })
                            .collect(Collectors.toList());
            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanRepaymentsByLoanDisbursed"),
                    loanpaymentsWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansrepaymentRepo.countByLoansDisbursed(loansDisbursed));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getOverdueLoans(long groupid, int page, int size) {
        return Mono.fromCallable(() -> {
            GroupWrapper group = chamaKycService.getMonoGroupById(groupid);
            if (group == null)
                return new UniversalResponse("fail", "Group not found");
            Pageable pageable = PageRequest.of(page, size);
            List<DisbursedloansWrapper> disbursedloansWrapperList =
                    loansdisbursedRepo
                            .findByGroupIdAndDueamountGreaterThanAndDuedateLessThanOrderByCreatedOnDesc(groupid, 0.0, new Date(), pageable)
                            .stream()
                            .map(p -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(p.getMemberId());
                                if (member == null) return null;
                                DisbursedloansWrapper disbursedloansWrapper = new DisbursedloansWrapper();
                                disbursedloansWrapper.setContributionid(p.getLoanApplications().getLoanProducts().getContributions().getId());
                                disbursedloansWrapper.setContributionname(p.getLoanApplications().getLoanProducts().getContributions().getName());
                                disbursedloansWrapper.setDueamount(p.getDueamount());
                                disbursedloansWrapper.setGroupid(group.getId());
                                disbursedloansWrapper.setGroupname(group.getName());
                                disbursedloansWrapper.setDuedate(p.getDuedate());
                                long diff = new Date().getTime() - p.getDuedate().getTime();
                                long daysdue = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                                disbursedloansWrapper.setDaysoverdue(daysdue);
                                disbursedloansWrapper.setInterest(p.getInterest());
                                disbursedloansWrapper.setLoanid(p.getId());
                                disbursedloansWrapper.setPrincipal(p.getPrincipal());
                                disbursedloansWrapper.setRecipient(member.getFirstname().concat(" ").concat(member.getLastname()));
                                disbursedloansWrapper.setRecipientsnumber(member.getPhonenumber());
                                return disbursedloansWrapper;
                            })
                            .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loansOverdue"),
                    disbursedloansWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loansdisbursedRepo.countByGroupIdAndDueamountGreaterThanAndDuedateLessThan(groupid, 0.0, new Date()));
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> payLoanByMpesa(LoanRepaymentsWrapper loanRepaymentWrapper, String paidBy) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(paidBy);
            if (member == null)
                return new UniversalResponse("fail", "Member not found");

            Optional<LoansDisbursed> optionalLoansDisbursed = loansdisbursedRepo.findById(loanRepaymentWrapper.getLoandisbursedid());
            if (optionalLoansDisbursed.isEmpty())
                return new UniversalResponse("fail", "Loan record not found");
            LoansDisbursed loansDisbursed = optionalLoansDisbursed.get();
            if (loanRepaymentWrapper.getAmount() > loansDisbursed.getDueamount())
                return new UniversalResponse("fail", "over payment is not allowed");
            String transactionId = "LP-" + UUID.randomUUID();
            LoanRepaymentPendingApproval loanRepaymentPendingApproval = new LoanRepaymentPendingApproval();
            loanRepaymentPendingApproval.setAmount(loanRepaymentWrapper.getAmount());
            loanRepaymentPendingApproval.setLoansDisbursed(loansDisbursed);
            loanRepaymentPendingApproval.setMemberId(member.getId());
            loanRepaymentPendingApproval.setPending(true);
            loanRepaymentPendingApproval.setReceiptnumber("");
            loanRepaymentPendingApproval.setReceiptImageUrl("");
            loanRepaymentPendingApproval.setPaymentType("mpesa");
            String response = makeLoanPayment(loanRepaymentWrapper.getPaymentPhoneNumber(), loanRepaymentWrapper.getAmount(), transactionId, "Loan Payment");
            if (response == null)
                return new UniversalResponse(getResponseMessage("paymentInitFailed"), getResponseMessage("couldNotGetResponseFromServer"));
            JsonObject jsonObject1 = new JsonParser().parse(response).getAsJsonObject();
            if (!jsonObject1.get("STATUS").getAsString().equals("00"))
                return new UniversalResponse("Payment Initialization fail", jsonObject1.get("STATUSDESCRIPTION").getAsString());

            String mpesaCheckoutId = jsonObject1.get("ORIGINAL_RESPONSE").getAsJsonObject().get("CheckoutRequestID").getAsString();
            loanRepaymentPendingApproval.setPaymentId(transactionId);
            if (mpesaCheckoutId != null) {
                loanRepaymentPendingApproval.setMpesaCheckoutId(mpesaCheckoutId);
            }
            loanrepaymentpendingapprovalRepo.save(loanRepaymentPendingApproval);
            return new UniversalResponse("success", "Payment initialized successfully");
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> payLoan(LoanpaymentsWrapper loanpaymentsWrapper) {

        return null;
    }

    private String makeLoanPayment(String phoneNumber, double amount, String transactionId, String cName) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", pgUsername);
        jsonObject.addProperty("password", pgClientPassword);
        jsonObject.addProperty("amount", (int) amount);
        jsonObject.addProperty("clientid", pgClientId);
        jsonObject.addProperty("accountno", phoneNumber);
        jsonObject.addProperty("narration", "Loan Payment");
        jsonObject.addProperty("serviceid", pgServiceId);
        jsonObject.addProperty("msisdn", phoneNumber);
        jsonObject.addProperty("transactionid", transactionId);
        jsonObject.addProperty("accountreference", cName);
        //anti- pattern using block
        return routerService.makeStkPushRequest(jsonObject);
    }

    @Override
    public Mono<UniversalResponse> getGroupLoansPenalties(Long groupId) {
        return Mono.fromCallable(() -> {
            GroupWrapper optionalGroup = chamaKycService.getMonoGroupById(groupId);
            if (optionalGroup == null)
                return new UniversalResponse("fail", "Group not found");
            List<LoansDisbursed> loansDisbursedList = loansdisbursedRepo.findAllByGroupIdOrderByCreatedOnDesc(groupId);
            List<LoanPenalty> loanPenalties = new ArrayList<>();
            loansDisbursedList.forEach(loansDisbursed -> {
                List<LoanPenalty> loanPenaltyList = loanPenaltyRepository.findAllByLoansDisbursed(loansDisbursed);
                loanPenalties.addAll(loanPenaltyList);
            });
            return getLoanPenalties(loanPenalties);
        }).publishOn(Schedulers.boundedElastic());
    }

    private UniversalResponse getLoanPenalties(List<LoanPenalty> loanPenaltyList) {
        List<LoanPenaltyWrapper> loanPenaltyWrappers = loanPenaltyList
                .parallelStream()
                .map(lp -> mapToLoanPenaltyWrapper().apply(lp))
                .collect(Collectors.toList());
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanPenalties"),
                loanPenaltyWrappers);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("numofrecords", loanPenaltyWrappers.size());
        response.setMetadata(metadata);
        return response;
    }

    Function<LoanPenalty, LoanPenaltyWrapper> mapToLoanPenaltyWrapper() {
        return lp -> {
            MemberWrapper member = chamaKycService.getMemberDetailsById(lp.getMemberId()).orElse(null);
            if (member == null) return null;
            LoanPenaltyWrapper loanPenaltyWrapper = new LoanPenaltyWrapper();
            loanPenaltyWrapper.setLoanPenaltyId(lp.getId());
            loanPenaltyWrapper.setDueAmount(lp.getDueAmount());
            loanPenaltyWrapper.setLoanDueDate(lp.getLoanDueDate());
            loanPenaltyWrapper.setMemberName(member.getFirstname().concat(" ").concat(member.getLastname()));
            loanPenaltyWrapper.setMemberPhoneNumber(member.getPhonenumber());
            return loanPenaltyWrapper;
        };
    }

    @Override
    public Mono<UniversalResponse> getMemberLoansPenalties(String phoneNumber) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);
            if (member == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            List<LoanPenalty> loanPenaltyList = loanPenaltyRepository.findAllByMemberId(member.getId());
            return getLoanPenalties(loanPenaltyList);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getMemberLoansPenalties(String phoneNumber, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);
            if (member == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            Page<LoanPenalty> pageData = loanPenaltyRepository.findAllByMemberId(member.getId(), pageable);

            List<LoanPenaltyWrapper> loanPenaltyWrappers = pageData.getContent()
                    .parallelStream()
                    .map(lp -> mapToLoanPenaltyWrapper().apply(lp))
                    .collect(Collectors.toList());

            return UniversalResponse.builder()
                    .status("success")
                    .message(getResponseMessage("loanPenaltiesList"))
                    .data(loanPenaltyWrappers)
                    .metadata(Map.of("currentPage", pageData.getNumber(), "numOfRecords", pageData.getNumberOfElements(), "totalPages", pageData.getTotalPages()))
                    .timestamp(new Date())
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> payLoanPenaltyByMpesa(PayPenaltyLoanWrapper payPenaltyLoanWrapper) {
        return Mono.fromCallable(() -> {
            Optional<LoanPenalty> optionalLoanPenalty = loanPenaltyRepository.findById(payPenaltyLoanWrapper.getPenaltyLoanId());
            if (payPenaltyLoanWrapper.getPaymentPhoneNumber() == null || payPenaltyLoanWrapper.getPaymentPhoneNumber().equals("")) {
                return new UniversalResponse("fail", getResponseMessage("paymentPhoneNumberIsNull"));
            }
            if (optionalLoanPenalty.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanPenaltyNotFound"));
            }
            LoanPenalty loanPenalty = optionalLoanPenalty.get();
            //make check transactionId
            //plp - penaltyloanpayment
            String transactionId = "PLP-" + UUID.randomUUID();

            String response = makeLoanPayment(payPenaltyLoanWrapper.getPaymentPhoneNumber(),
                    payPenaltyLoanWrapper.getAmount(), transactionId, getResponseMessage("loanPenaltyPayment"));

            if (response == null) {
                return new UniversalResponse("Payment Initialization fail", getResponseMessage("couldNotGetResponseFromServer"));
            }
            JsonObject jsonObject1 = new JsonParser().parse(response).getAsJsonObject();
            if (!jsonObject1.get("STATUS").getAsString().equals("00")) {
                return new UniversalResponse("Payment Initialization fail", jsonObject1.get("STATUSDESCRIPTION").getAsString());

            }
            String mpesaCheckoutId = jsonObject1.get("ORIGINAL_RESPONSE").getAsJsonObject().get("CheckoutRequestID").getAsString();
            LoanPenaltyPayment loanPenaltyPayment = new LoanPenaltyPayment();
            loanPenaltyPayment.setPaidAmount(payPenaltyLoanWrapper.getAmount());
            loanPenaltyPayment.setPaymentMethod("mpesa");
            loanPenaltyPayment.setTransactionId(transactionId);
            loanPenaltyPayment.setLoanPenalty(loanPenalty);
            loanPenaltyPayment.setPaymentStatus(PaymentEnum.PAYMENT_PENDING.name());
            loanPenaltyPayment.setMpesaCheckoutId(mpesaCheckoutId);
            loanPenaltyPaymentRepository.save(loanPenaltyPayment);
            return new UniversalResponse("success", getResponseMessage("paymentInitSuccess"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> payLoanPenaltyByReciept(PayPenaltyLoanWrapper payPenaltyLoanWrapper, FilePart file) {
        return Mono.fromCallable(() -> {
            Optional<LoanPenalty> optionalLoanPenalty = loanPenaltyRepository.findById(payPenaltyLoanWrapper.getPenaltyLoanId());
            if (payPenaltyLoanWrapper.getPaymentPhoneNumber() == null || payPenaltyLoanWrapper.getPaymentPhoneNumber().equals("")) {
                return new UniversalResponse("fail", getResponseMessage("paymentPhoneNumberIsNull"));
            }
            if (optionalLoanPenalty.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("loanPenaltyNotFound"));
            }
            String imageUrl = fileHandlerService.uploadFile(file);
            if (imageUrl.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("receiptUploadFailed"));
            }
            LoanPenalty loanPenalty = optionalLoanPenalty.get();
            LoanPenaltyPayment loanPenaltyPayment = new LoanPenaltyPayment();
            loanPenaltyPayment.setPaidAmount(payPenaltyLoanWrapper.getAmount());
            loanPenaltyPayment.setPaymentMethod("receipt");
            loanPenaltyPayment.setTransactionId(payPenaltyLoanWrapper.getReceiptNumber());
            loanPenaltyPayment.setLoanPenalty(loanPenalty);
            loanPenaltyPayment.setReceiptImageUrl(fileHandlerService.getFileUrl(imageUrl));
            loanPenaltyPayment.setPaymentStatus(PaymentEnum.PAYMENT_PENDING.name());
            loanPenaltyPaymentRepository.save(loanPenaltyPayment);
            return new UniversalResponse("success", getResponseMessage("paymentRecordedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Double getTotalLoansByGroup(long groupId) {
        return loansdisbursedRepo.findAllByGroupIdOrderByCreatedOnDesc(groupId)
                .stream()
                .mapToDouble(LoansDisbursed::getPrincipal)
                .sum();
    }

    @Override
    public Mono<UniversalResponse> getInactiveGroupLoanProducts(Long groupId) {
        return Mono.fromCallable(() -> {
            GroupWrapper group = chamaKycService.getMonoGroupById(groupId);

            if (group == null)
                return new UniversalResponse("fail", "Group not found");

            List<LoanproductWrapper> loanproductWrapperList = loanproductsRepository.findAllByGroupIdAndIsActive(groupId, false)
                    .stream()
                    .map(p -> mapToLoanProductWrapper(group, p))
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("inactiveLoanProductsPerGroup"),
                    loanproductWrapperList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanproductWrapperList.size());
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @NonNull
    private LoanproductWrapper mapToLoanProductWrapper(GroupWrapper group, LoanProducts p) {
        LoanproductWrapper loanproductWrapper = new LoanproductWrapper();
        loanproductWrapper.setGroupname(group.getName());
        loanproductWrapper.setInteresttype(p.getInteresttype());
        loanproductWrapper.setInterestvalue(p.getInterestvalue());
        loanproductWrapper.setMax_principal(p.getMax_principal());
        loanproductWrapper.setMin_principal(p.getMin_principal());
        loanproductWrapper.setProductid(p.getId());
        loanproductWrapper.setProductname(p.getProductname());
        loanproductWrapper.setPaymentperiod(p.getPaymentperiod());
        loanproductWrapper.setPaymentperiodtype(p.getPaymentperiodtype());
        loanproductWrapper.setGroupid(group.getId());
        loanproductWrapper.setDescription(p.getDescription());
        loanproductWrapper.setContributionid(p.getContributions().getId());
        loanproductWrapper.setContributionname(p.getContributions().getName());
        loanproductWrapper.setIsguarantor(p.isGuarantor());
        loanproductWrapper.setHasPenalty(p.isPenalty());
        loanproductWrapper.setPenaltyvalue(p.getPenaltyValue());
        loanproductWrapper.setIspenaltypercentage(p.getIsPercentagePercentage());
        loanproductWrapper.setUsersavingvalue(p.getUserSavingValue());
        loanproductWrapper.setUserLoanLimit(loanLimit(p));
        loanproductWrapper.setIsActive(p.getIsActive());
        loanproductWrapper.setDebitAccountId(p.getDebitAccountId().getId());
        loanproductWrapper.setPenaltyPeriod(p.getPenaltyPeriod());
        return loanproductWrapper;
    }

    @Override
    public Mono<UniversalResponse> getGroupsLoanSummaryPayment(String groupName, Date startDate, Date endDate, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<GroupsLoanSummaryWrapper> loansList;
            if (!groupName.equalsIgnoreCase("all")) {
                GroupWrapper group = chamaKycService.getMonoGroupByName(groupName);
                if (group == null)
                    return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

                loansList = loansrepaymentRepo.findAllByGroupIdAndCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(group.getId(), startDate, endDate, pageable)
                        .stream()
                        .map(loan -> {
                            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                            if (member == null) return null;
                            return new GroupsLoanSummaryWrapper(member.getFirstname()
                                    , member.getLastname(), group.getName(),
                                    loan.getPaymentType(), loan.getAmount(), loan.getCreatedOn());
                        })
                        .collect(Collectors.toList());
            } else {
                loansList = loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable)
                        .stream()
                        .map(loan -> {
                            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getLoansDisbursed().getMemberId());
                            if (member == null) return null;
                            GroupWrapper group = chamaKycService.getMonoGroupById(loan.getLoansDisbursed().getGroupId());
                            if (group == null) return null;
                            return GroupsLoanSummaryWrapper.builder()
                                    .firstName(member.getFirstname())
                                    .lastName(member.getLastname())
                                    .groupName(group.getName())
                                    .paymentType(loan.getPaymentType())
                                    .amount(loan.getAmount())
                                    .paymentDate(loan.getCreatedOn())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }
            return new UniversalResponse("success", String.format(getResponseMessage("loanRepaymentsFor"), groupName), loansList);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> initiateLoanRepayment(LoanRepaymentsWrapper loanRepaymentsWrapper, String username) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(username);
            if (member == null)
                return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            Optional<LoansDisbursed> loanDisbursedOptional = loansdisbursedRepo.findById(loanRepaymentsWrapper.getLoandisbursedid());

            if (loanDisbursedOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanDisbursedNotFound"));

            LoansDisbursed loansDisbursed = loanDisbursedOptional.get();
            GroupWrapper group = chamaKycService.getMonoGroupById(loansDisbursed.getGroupId());

            if (group == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            String transactionId = TransactionIdGenerator.generateTransactionId("LPN");
            Map<String, String> esbRequest;
            int amount = loanRepaymentsWrapper.getAmount().intValue();
            if (loanRepaymentsWrapper.getCoreAccount().isBlank() || loanRepaymentsWrapper.getCoreAccount().length() < 14) {
                esbRequest = constructBody(group.getCsbAccount(), username, amount, loansDisbursed.getId(),
                        Optional.empty(), transactionId, "LR");
            } else {
                if (member.getLinkedAccounts() == null)
                    return new UniversalResponse("fail", getResponseMessage("memberDoesNotHaveLinkedAccount"));

                if (Arrays.stream(member.getLinkedAccounts().split(",")).noneMatch(s -> s.equals(loanRepaymentsWrapper.getCoreAccount())))
                    return new UniversalResponse("fail", getResponseMessage("memberDoesNotOwnCoreAccount"));

                UniversalResponse coreAccountValidationRes = validateCoreAccount(loanRepaymentsWrapper.getCoreAccount(), member);
                if (coreAccountValidationRes != null) return coreAccountValidationRes;


                esbRequest = constructBody(group.getCsbAccount(), loanRepaymentsWrapper.getCoreAccount(),
                        amount, loansDisbursed.getId(), Optional.empty(), transactionId, "LRC");
            }

            LoansRepayment loansRepayment = new LoansRepayment();
            loansRepayment.setLoansDisbursed(loansDisbursed);
            loansRepayment.setAmount(amount);
            loansRepayment.setMemberId(loansDisbursed.getMemberId());
            loansRepayment.setPaymentType(loanRepaymentsWrapper.getCoreAccount().isBlank() ? "WALLET" : "CORE ACCOUNT");
            loansRepayment.setReceiptnumber(transactionId);
            loansRepayment.setNewamount(loansDisbursed.getDueamount() - amount);
            loansRepayment.setOldamount(loansDisbursed.getDueamount());
            loansRepayment.setStatus(PaymentEnum.PAYMENT_PENDING.name());

            loansrepaymentRepo.save(loansRepayment);
            String bodyRequest = gson.toJson(esbRequest);

            return webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(res -> log.info("Loan repayment initiation response... {}", res))
                    .map(res -> new UniversalResponse("success", getResponseMessage("requestReceived")))
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("serviceNotAvailable")))
                    .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    private UniversalResponse validateCoreAccount(String coreAccount, MemberWrapper member) {
        if (coreAccount.isBlank() && member.getLinkedAccounts() == null)
            return new UniversalResponse("fail", getResponseMessage("memeberHasNoLinkedAccounts"));

        if (coreAccount.isBlank() && member.getLinkedAccounts() != null && Arrays.stream(member.getLinkedAccounts().split(",")).noneMatch(s -> s.equals(coreAccount)))
            return new UniversalResponse("fail", getResponseMessage("coreAccountDoesNotBelongToMember"));
        return null;
    }

    private void checkBalance(LoanRepaymentsWrapper dto, MemberWrapper member) {
        String accountToCheckBalance = dto.getCoreAccount().isBlank() ? member.getEsbwalletaccount() : dto.getCoreAccount();
        Optional<UniversalResponse> optionalBalanceInquiryRes = esbService.balanceInquiry(accountToCheckBalance).blockOptional();
        optionalBalanceInquiryRes.ifPresent(balanceInquiryRes -> {
            if (balanceInquiryRes.getStatus().equals("fail"))
                throw new IllegalArgumentException(getResponseMessage("balanceInquiryFailed"));

            BalanceInquiry balanceInquiry = (BalanceInquiry) balanceInquiryRes.getData();
            if (Integer.parseInt(balanceInquiry.getAvailableBal()) <= dto.getAmount())
                throw new IllegalArgumentException(getResponseMessage("insufficientBalance"));
        });
    }

    @Override
    public Mono<UniversalResponse> getLoanApplications(Long loanProductId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return Mono.fromCallable(() -> {
            Page<LoanApplicationsProjection> loanApplications = loanapplicationsRepo.findLoanApplications(loanProductId, pageable);
            Map<String, Integer> metadata = Map.of(
                    "currentpage", loanApplications.getNumber(),
                    "totalcount", loanApplications.getTotalPages(),
                    "numofrecords", loanApplications.getNumberOfElements()
            );

            return new UniversalResponse("success", getResponseMessage("loanApplicationsForLoanProduct"), loanApplications.getContent(), new Date(), metadata);
        }).publishOn(Schedulers.boundedElastic());
    }

    Predicate<LoansRepayment> filterGroup(long groupId, String groupName) {
        return loansRepayment -> {
            GroupWrapper group = chamaKycService.getMonoGroupById(groupId);
            if (group == null) return false;
            return group.getName().equalsIgnoreCase(groupName);
        };
    }

    @Override
    public Mono<UniversalResponse> getUserLoanApplications(String phoneNumber, Integer page, Integer size) {
        return Mono.fromCallable(() -> {
            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);

            if (memberWrapper == null) return new UniversalResponse("fail", getResponseMessage(MEMBER_NOT_FOUND));

            List<LoanApplications> memberLoanApplications = loanapplicationsRepo.findAllByMemberId(memberWrapper.getId());

            return new UniversalResponse("success", getResponseMessage("memberLoanApplicationList"), memberLoanApplications);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getLoanPaymentsByLoanProductProduct(long loanProductId, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> optionalLoanProduct = loanproductsRepository.findById(loanProductId);

            if (optionalLoanProduct.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));

            Pageable pageable = PageRequest.of(page, size);
            Page<LoanRepaymentsProjection> repaymentsByLoanProduct = loansrepaymentRepo.findAllRepaymentsByLoanProduct(loanProductId, pageable);

            return UniversalResponse.builder()
                    .status("success")
                    .message("Loan repayments for loan product")
                    .data(repaymentsByLoanProduct.getContent())
                    .metadata(Map.of("numofrecords", repaymentsByLoanProduct.getTotalElements()))
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getUserLoanProducts(String username) {
        return chamaKycService.getFluxGroupsMemberBelongs(username)
                .filter(GroupWrapper::isActive)
                .map(GroupWrapper::getId)
                .publishOn(Schedulers.boundedElastic())
                .map(loanproductsRepository::findAllByGroupId)
                .flatMap(Flux::fromIterable)
                .mapNotNull(this::mapToLoanProductWrapper)
                .collectList()
                .map(lps -> UniversalResponse.builder()
                        .status("success")
                        .message("Loan Products for User")
                        .data(lps)
                        .build());
    }

    @Override
    public Mono<UniversalResponse> getActiveLoanProductsbyGroup(Long groupId, boolean isActive) {
        return Mono.fromCallable(() -> {
            GroupWrapper optionalGroup = chamaKycService.getMonoGroupById(groupId);

            if (optionalGroup == null)
                return new UniversalResponse("fail", getResponseMessage(GROUP_NOT_FOUND));

            List<LoanproductWrapper> loanProductsList = loanproductsRepository.findAllByGroupIdAndIsActive(groupId,true)
                    .stream()
                    .map(p -> {
                        return mapToLoanProductWrapper(optionalGroup, p);
                    })
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanProductsPerGroup"),
                    loanProductsList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", loanProductsList.size());
            response.setMetadata(metadata);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private LoanproductWrapper mapToLoanProductWrapper(LoanProducts p) {
        Optional<String> optionalGroupName = chamaKycService.getGroupNameByGroupId(p.getGroupId());

        if (optionalGroupName.isEmpty()) return null;

        LoanproductWrapper loanproductWrapper = new LoanproductWrapper();
        loanproductWrapper.setGroupname(optionalGroupName.get());
        loanproductWrapper.setInteresttype(p.getInteresttype());
        loanproductWrapper.setInterestvalue(p.getInterestvalue());
        loanproductWrapper.setMax_principal(p.getMax_principal());
        loanproductWrapper.setMin_principal(p.getMin_principal());
        loanproductWrapper.setProductid(p.getId());
        loanproductWrapper.setProductname(p.getProductname());
        loanproductWrapper.setPaymentperiod(p.getPaymentperiod());
        loanproductWrapper.setPaymentperiodtype(p.getPaymentperiodtype());
        loanproductWrapper.setGroupid(p.getGroupId());
        loanproductWrapper.setDescription(p.getDescription());
        loanproductWrapper.setContributionid(p.getContributions().getId());
        loanproductWrapper.setContributionname(p.getContributions().getName());
        loanproductWrapper.setIsguarantor(p.isGuarantor());
        loanproductWrapper.setHasPenalty(p.isPenalty());
        loanproductWrapper.setPenaltyvalue(p.getPenaltyValue());
        loanproductWrapper.setIspenaltypercentage(p.getIsPercentagePercentage());
        loanproductWrapper.setUsersavingvalue(p.getUserSavingValue());
        loanproductWrapper.setUserLoanLimit(loanLimit(p));
        loanproductWrapper.setIsActive(p.getIsActive());
        loanproductWrapper.setDebitAccountId(p.getDebitAccountId().getId());
        loanproductWrapper.setPenaltyPeriod(p.getPenaltyPeriod());
        return loanproductWrapper;
    }
}