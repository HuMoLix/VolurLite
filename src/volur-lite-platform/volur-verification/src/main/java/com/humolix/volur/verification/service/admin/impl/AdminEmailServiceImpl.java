package com.humolix.volur.verification.service.admin.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.domain.dto.AdminEmailQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminSendEmailDTO;
import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.domain.vo.AdminEmailStatisticsVO;
import com.humolix.volur.verification.domain.vo.EmailLogVO;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.executor.MailTaskExecutor;
import com.humolix.volur.verification.mapper.EmailLogMapper;
import com.humolix.volur.verification.repository.EmailLogRepository;
import com.humolix.volur.verification.service.admin.AdminEmailService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEmailServiceImpl implements AdminEmailService {

    private final EmailLogRepository emailLogRepository;
    private final EmailLogMapper emailLogMapper;
    private final MailTaskExecutor mailTaskExecutor;
    private final SystemSettingService systemSettingService;

    @Override
    public PageResponse<EmailLogVO> listEmails(AdminEmailQueryDTO queryDTO) {
        Pageable pageable = SortUtils.createPageable(
                queryDTO.getPage(),
                queryDTO.getSize(),
                EmailLog.class,
                queryDTO.getSortBy(),
                queryDTO.getSortOrder(),
                "createdAt",
                Sort.Direction.DESC
        );

        Specification<EmailLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(queryDTO.getKeyword())) {
                String keywordLike = "%" + queryDTO.getKeyword() + "%";
                Predicate recipientLike = cb.like(root.get("recipient"), keywordLike);
                Predicate subjectLike = cb.like(root.get("subject"), keywordLike);
                Predicate senderLike = cb.like(root.get("sender"), keywordLike);
                predicates.add(cb.or(recipientLike, subjectLike, senderLike));
            }

            if (StringUtils.hasText(queryDTO.getType())) {
                predicates.add(cb.equal(root.get("type"), queryDTO.getType().toUpperCase()));
            }

            if (StringUtils.hasText(queryDTO.getStatus())) {
                predicates.add(cb.equal(root.get("status"), queryDTO.getStatus().toUpperCase()));
            }

            if (queryDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), queryDTO.getStartTime()));
            }

            if (queryDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), queryDTO.getEndTime()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<EmailLog> entityPage = emailLogRepository.findAll(spec, pageable);
        return PageResponse.of(entityPage.map(emailLogMapper::toVO));
    }

    @Override
    public AdminEmailStatisticsVO getStatistics() {
        long total = emailLogRepository.count();
        long success = emailLogRepository.countByStatusIn(List.of("DELIVERED", "SENT", "SUCCESS"));
        long failed = emailLogRepository.countByStatusIn(List.of("FAILED", "BOUNCED", "COMPLAINED"));

        Instant startOfToday = LocalDate.now(ZoneId.of("Asia/Shanghai")).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        long today = emailLogRepository.countByCreatedAtAfter(startOfToday);

        return AdminEmailStatisticsVO.builder()
                .totalEmails(total)
                .successEmails(success)
                .failedEmails(failed)
                .todayEmails(today)
                .build();
    }

    @Override
    public EmailLogVO getEmailDetail(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "邮件记录 ID 不能为空");
        }
        EmailLog emailLog = emailLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "邮件记录不存在"));
        return emailLogMapper.toVO(emailLog);
    }

    @Override
    public void sendManualEmail(AdminSendEmailDTO sendDTO, Long operatorId) {
        String from = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_FROM_ADDRESS, "");
        if (!StringUtils.hasText(from)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "发件人邮箱地址 (system.mail.from_address) 未配置或为空，无法发送邮件");
        }
        from = from.trim();
        String senderName = systemSettingService.getSettingValue(SystemSettingKeys.MAIL_SENDER_NAME, "");

        boolean isHtml = sendDTO.getHtml() == null || Boolean.TRUE.equals(sendDTO.getHtml());
        String ip = RequestUtil.getRemoteIp();

        MailMessageBO message = MailMessageBO.builder()
                .from(from)
                .senderName(senderName)
                .to(sendDTO.getRecipient())
                .subject(sendDTO.getSubject())
                .content(sendDTO.getContent())
                .fallbackContent(sendDTO.getContent())
                .html(isHtml)
                .type("MANUAL")
                .operatorId(operatorId)
                .operatorIp(ip)
                .build();

        mailTaskExecutor.executeAsyncMail(message);
    }

    @Override
    public void resendEmail(Long id, Long operatorId) {
        EmailLog oldLog = emailLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到指定的邮件记录"));

        AdminSendEmailDTO sendDTO = new AdminSendEmailDTO(
                oldLog.getRecipient(),
                oldLog.getSubject(),
                oldLog.getContent(),
                true
        );
        sendManualEmail(sendDTO, operatorId);
    }

}
