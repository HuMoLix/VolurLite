package com.humolix.volur.competition.aspect;

import com.humolix.volur.competition.annotation.CompetitionLog;
import com.humolix.volur.competition.domain.dto.CompetitionLogContextDTO;
import com.humolix.volur.competition.service.CompetitionLogPersistenceService;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CompetitionLogAspect {

    private static final Pattern COMPETITION_URI_PATTERN = Pattern.compile("/api/v1/(?:admin/)?competitions/(\\d+)");

    private final CompetitionLogPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(competitionLog)")
    public Object around(ProceedingJoinPoint joinPoint, CompetitionLog competitionLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 同步捕获 Web 请求现场快照（防止 Tomcat 回收 Request 导致虚拟线程读取空指针）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String operatorIp = IpUtils.getClientIp(request);
        String requestUri = request != null ? request.getRequestURI() : null;
        String requestMethod = request != null ? request.getMethod() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // 2. 同步提取调用签名与参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Long competitionId = extractLongParam(paramNames, args, "competitionId");
        if (competitionId == null && requestUri != null) {
            competitionId = parseCompetitionIdFromUri(requestUri);
        }

        Long challengeId = extractLongParam(paramNames, args, "challengeId", "competitionChallengeId");
        Long userId = extractLongParam(paramNames, args, "userId");

        // 3. 序列化请求参数快照
        String paramsJson = null;
        if (competitionLog.recordParams()) {
            paramsJson = serializeArguments(paramNames, args);
        }

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // 4. 计算业务状态判定（区分业务判定错误与异常）
            boolean status = true;
            String errorMessage = null;

            if (exception != null) {
                status = false;
                errorMessage = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
                if (errorMessage.length() > 2000) {
                    errorMessage = errorMessage.substring(0, 2000);
                }
            } else if (result instanceof ApiResponseEntity<?> entity) {
                ApiResponse<?> apiResponse = entity.getBody();
                if (apiResponse != null) {
                    if (apiResponse.data() instanceof Boolean b) {
                        status = b;
                        if (!b && StringUtils.hasText(apiResponse.message())) {
                            errorMessage = apiResponse.message();
                        }
                    } else if (!apiResponse.success() || apiResponse.status() != 200) {
                        status = false;
                        errorMessage = apiResponse.message();
                    }
                }
            } else if (result instanceof ApiResponse<?> apiResponse) {
                if (apiResponse.data() instanceof Boolean b) {
                    status = b;
                    if (!b && StringUtils.hasText(apiResponse.message())) {
                        errorMessage = apiResponse.message();
                    }
                } else if (!apiResponse.success() || apiResponse.status() != 200) {
                    status = false;
                    errorMessage = apiResponse.message();
                }
            }

            String resultJson = null;
            if (competitionLog.recordResponse() && result != null && exception == null) {
                try {
                    Object responseToSerialize = result instanceof ApiResponseEntity<?> entity ? entity.getBody() : result;
                    resultJson = objectMapper.writeValueAsString(responseToSerialize);
                    if (resultJson.length() > 4000) {
                        resultJson = resultJson.substring(0, 4000) + "... (truncated)";
                    }
                } catch (Exception ignored) {
                }
            }

            // 6. 构造上下文并交由虚拟线程异步落库
            String action = StringUtils.hasText(competitionLog.action())
                    ? competitionLog.action()
                    : signature.getMethod().getName();

            CompetitionLogContextDTO context = CompetitionLogContextDTO.builder()
                    .competitionId(competitionId)
                    .challengeId(challengeId)
                    .userId(userId)
                    .action(action)
                    .logType(competitionLog.logType())
                    .status(status)
                    .operatorIp(operatorIp)
                    .requestMethod(requestMethod)
                    .requestUri(requestUri)
                    .durationMs(durationMs)
                    .params(paramsJson)
                    .result(resultJson)
                    .errorMessage(errorMessage)
                    .userAgent(userAgent)
                    .build();

            persistenceService.saveLogAsync(context);
        }
    }

    private Long extractLongParam(String[] paramNames, Object[] args, String... targetNames) {
        if (paramNames == null || args == null) {
            return null;
        }
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            String name = paramNames[i];
            for (String target : targetNames) {
                if (target.equalsIgnoreCase(name) && args[i] instanceof Long l) {
                    return l;
                } else if (target.equalsIgnoreCase(name) && args[i] instanceof Number n) {
                    return n.longValue();
                }
            }
        }
        return null;
    }

    private Long parseCompetitionIdFromUri(String uri) {
        try {
            Matcher matcher = COMPETITION_URI_PATTERN.matcher(uri);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String serializeArguments(String[] paramNames, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        List<Object> filterArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof InputStream
                    || arg instanceof OutputStream) {
                continue;
            }
            filterArgs.add(arg);
        }

        if (filterArgs.isEmpty()) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(filterArgs.size() == 1 ? filterArgs.get(0) : filterArgs);
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "... (truncated)";
            }
            return json;
        } catch (Exception e) {
            return "[Serialization Error: " + e.getMessage() + "]";
        }
    }

}
