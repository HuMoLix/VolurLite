package com.humolix.volur.competition.aspect;

import com.humolix.volur.competition.annotation.RequiredCompetitionRunning;
import com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionStageConfig;
import com.humolix.volur.competition.domain.enums.CompetitionStage;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.StageStatus;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionStageConfigRepository;
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
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CompetitionValidationAspect {

    private final CompetitionRepository competitionRepository;
    private final CompetitionStageConfigRepository stageConfigRepository;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(com.humolix.volur.competition.annotation.RequiredCompetitionRunning) || " +
            "@annotation(com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning) || " +
            "@within(com.humolix.volur.competition.annotation.RequiredCompetitionRunning) || " +
            "@within(com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning)")
    public Object validateCompetitionAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        RequiredCompetitionStageRunning stageAnnotation = method.getAnnotation(RequiredCompetitionStageRunning.class);
        if (stageAnnotation == null) {
            stageAnnotation = targetClass.getAnnotation(RequiredCompetitionStageRunning.class);
        }

        RequiredCompetitionRunning runningAnnotation = method.getAnnotation(RequiredCompetitionRunning.class);
        if (runningAnnotation == null) {
            runningAnnotation = targetClass.getAnnotation(RequiredCompetitionRunning.class);
        }

        if (stageAnnotation != null) {
            Long competitionId = extractCompetitionId(joinPoint, stageAnnotation.key());
            validateCompetitionStage(competitionId, stageAnnotation.stage());
        } else if (runningAnnotation != null) {
            Long competitionId = extractCompetitionId(joinPoint, runningAnnotation.key());
            validateCompetition(competitionId);
        }

        return joinPoint.proceed();
    }

    public Competition validateCompetition(Long competitionId) {
        if (competitionId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛标识不能为空");
        }

        Competition comp = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        if (comp.getStatus() == CompetitionStatus.DRAFT || comp.getStatus() == CompetitionStatus.ARCHIVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛未开放或已归档");
        }

        Instant now = Instant.now();
        if (comp.getStartTime() != null && now.isBefore(comp.getStartTime())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛尚未开始");
        }
        if (comp.getEndTime() != null && now.isAfter(comp.getEndTime())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "竞赛已结束");
        }

        return comp;
    }

    public void validateCompetitionStage(Long competitionId, CompetitionStage stage) {
        validateCompetition(competitionId);

        if (stage == null) {
            return;
        }

        List<CompetitionStageConfig> stageConfigs = stageConfigRepository.findByCompetitionIdAndStageOrderByStageOrderAsc(competitionId, stage);
        if (!stageConfigs.isEmpty()) {
            Instant now = Instant.now();
            boolean hasRunningStage = stageConfigs.stream().anyMatch(s ->
                    s.getStatus() != StageStatus.PENDING &&
                    s.getStatus() != StageStatus.ENDED &&
                    (s.getStartTime() == null || !now.isBefore(s.getStartTime())) &&
                    (s.getEndTime() == null || !now.isAfter(s.getEndTime()))
            );
            if (!hasRunningStage) {
                boolean allPending = stageConfigs.stream().allMatch(s -> s.getStatus() == StageStatus.PENDING || (s.getStartTime() != null && now.isBefore(s.getStartTime())));
                if (allPending) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "该赛段尚未开放");
                }
                boolean allEnded = stageConfigs.stream().allMatch(s -> s.getStatus() == StageStatus.ENDED || (s.getEndTime() != null && now.isAfter(s.getEndTime())));
                if (allEnded) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "该赛段已结束");
                }
                throw new BusinessException(HttpStatus.FORBIDDEN, "当前没有正在运行的该类型赛段");
            }
        }
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
