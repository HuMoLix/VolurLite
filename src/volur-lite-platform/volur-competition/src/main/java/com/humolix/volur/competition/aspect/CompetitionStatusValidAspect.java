package com.humolix.volur.competition.aspect;

import com.humolix.volur.competition.annotation.RequiredCompetitionStatus;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CompetitionStatusValidAspect {

    private final CompetitionRepository competitionRepository;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(com.humolix.volur.competition.annotation.RequiredCompetitionStatus) || " +
            "@within(com.humolix.volur.competition.annotation.RequiredCompetitionStatus)")
    public Object validateCompetitionAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        RequiredCompetitionStatus statusAnnotation = method.getAnnotation(RequiredCompetitionStatus.class);
        if (statusAnnotation == null) {
            statusAnnotation = targetClass.getAnnotation(RequiredCompetitionStatus.class);
        }

        if (statusAnnotation != null) {
            Long competitionId = extractCompetitionId(joinPoint, statusAnnotation.key());
            validateCompetitionStatus(competitionId, statusAnnotation.allowed(), statusAnnotation.types());
        }

        return joinPoint.proceed();
    }

    public Competition validateCompetitionStatus(Long competitionId, CompetitionStatus[] allowedStatuses, CompetitionType[] allowedTypes) {
        if (competitionId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛标识不能为空");
        }

        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        comp.refreshStatus();

        // 1. 状态匹配校验
        if (allowedStatuses != null && allowedStatuses.length > 0) {
            List<CompetitionStatus> allowedList = Arrays.asList(allowedStatuses);
            if (!allowedList.contains(comp.getStatus())) {
                // 特殊友好提示：如果要求处于“报名中”，但当前状态不符
                if (allowedList.contains(CompetitionStatus.REGISTRATION)) {
                    if (comp.getStatus() == CompetitionStatus.DRAFT) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛尚未发布，暂不开放报名");
                    } else if (comp.getStatus() == CompetitionStatus.ARCHIVED || comp.getStatus() == CompetitionStatus.ENDED) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛已结束或已归档，报名通道已关闭");
                    } else if (comp.getStatus() == CompetitionStatus.RUNNING) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛已开始，报名通道已关闭");
                    } else if (comp.getRegStartTime() != null && Instant.now().isBefore(comp.getRegStartTime())) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "报名尚未开启");
                    } else if (comp.getRegEndTime() != null && Instant.now().isAfter(comp.getRegEndTime())) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "报名已截止");
                    }
                }
                // 特殊友好提示：如果要求处于“进行中”，但当前状态不符
                if (allowedList.contains(CompetitionStatus.RUNNING)) {
                    if (comp.getStatus() == CompetitionStatus.DRAFT || comp.getStatus() == CompetitionStatus.ARCHIVED) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛未开放或已归档");
                    } else if (comp.getStartTime() != null && Instant.now().isBefore(comp.getStartTime())) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛尚未开始");
                    } else if (comp.getEndTime() != null && Instant.now().isAfter(comp.getEndTime())) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛已结束");
                    }
                }
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "当前竞赛状态不允许执行该操作 (当前状态: " + comp.getStatus().getDescription() + ")");
            }
        }

        // 2. 赛制模式匹配校验
        if (allowedTypes != null && allowedTypes.length > 0) {
            boolean typeMatched = false;
            for (CompetitionType t : allowedTypes) {
                if (comp.getType() == t) {
                    typeMatched = true;
                    break;
                }
            }
            if (!typeMatched) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "该竞赛模式不满足要求");
            }
        }

        return comp;
    }

    private Long extractCompetitionId(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        if (StringUtils.hasText(keyExpression)) {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            try {
                Object val = expressionParser.parseExpression(keyExpression).getValue(context);
                if (val instanceof Number) {
                    return ((Number) val).longValue();
                } else if (val instanceof String) {
                    return Long.parseLong((String) val);
                }
            } catch (Exception e) {
                log.warn("Failed to parse SpEL expression for competitionId: {}", keyExpression, e);
            }
        }

        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("competitionId".equalsIgnoreCase(paramNames[i])) {
                    if (args[i] instanceof Number) {
                        return ((Number) args[i]).longValue();
                    } else if (args[i] instanceof String) {
                        return Long.parseLong((String) args[i]);
                    }
                }
            }
        }

        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        for (Object arg : args) {
            if (arg != null) {
                try {
                    Method getter = arg.getClass().getMethod("getCompetitionId");
                    Object val = getter.invoke(arg);
                    if (val instanceof Number) {
                        return ((Number) val).longValue();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法提取竞赛标识 competitionId 进行状态核验");
    }

}
