package com.humolix.volur.competition.websocket.interceptor;

import com.humolix.volur.auth.security.jwt.JwtUtil;
import com.humolix.volur.auth.service.TokenService;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.enums.MemberStatus;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.entity.UserProfile;
import com.humolix.volur.user.domain.enums.UserRole;
import com.humolix.volur.user.repository.UserProfileRepository;
import com.humolix.volur.user.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.humolix.volur.framework.common.util.RequestUtil;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "CHAT_USER_ID";
    public static final String ATTR_USERNAME = "CHAT_USERNAME";
    public static final String ATTR_COMPETITION_ID = "CHAT_COMPETITION_ID";
    public static final String ATTR_TEAM_ID = "CHAT_TEAM_ID";
    public static final String ATTR_TEAM_NAME = "CHAT_TEAM_NAME";
    public static final String ATTR_IS_REFEREE = "CHAT_IS_REFEREE";
    public static final String ATTR_ROLE = "CHAT_ROLE";
    public static final String ATTR_AVATAR = "CHAT_AVATAR";

    private static final Pattern PATH_PATTERN = Pattern.compile("^/ws/competitions/(\\d+)/chat/?$");

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CompetitionMemberRepository memberRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String path = uri.getPath();
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            log.warn("[WebSocket-Chat] 拒绝连接: 请求路径不匹配: {}", path);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        Long competitionId;
        try {
            competitionId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = RequestUtil.getCookieValue(servletRequest.getServletRequest(), "access_token");
        }
        if (!StringUtils.hasText(token)) {
            List<String> cookieHeaders = request.getHeaders().get(HttpHeaders.COOKIE);
            if (cookieHeaders != null) {
                for (String cookieHeader : cookieHeaders) {
                    for (String piece : cookieHeader.split(";")) {
                        String[] pair = piece.trim().split("=", 2);
                        if (pair.length == 2 && "access_token".equalsIgnoreCase(pair[0].trim())) {
                            token = pair[1].trim();
                            break;
                        }
                    }
                    if (StringUtils.hasText(token)) {
                        break;
                    }
                }
            }
        }

        if (!StringUtils.hasText(token)) {
            log.warn("[WebSocket-Chat] 拒绝连接: 缺少 Token 鉴权凭证, path={}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Jwt claims = jwtUtil.getClaimsFromToken(token);

            if (tokenService.isTokenBlacklisted(claims.getId())) {
                log.warn("[WebSocket-Chat] 拒绝连接: 令牌已被列入黑名单");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtUtil.getUserIdFromTokenAsLong(token);
            long iat = claims.getIssuedAt() != null ? claims.getIssuedAt().toEpochMilli() : 0L;
            if (tokenService.isUserForceLoggedOut(userId, iat)) {
                log.warn("[WebSocket-Chat] 拒绝连接: 账号已被强制下线");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || Boolean.FALSE.equals(user.getStatus())) {
                log.warn("[WebSocket-Chat] 拒绝连接: 用户不存在或已被禁用: userId={}", userId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            boolean isReferee = (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN);

            // 查询队伍从属
            List<CompetitionMember> memberships = memberRepository.findMembersWithTeam(competitionId, userId);
            CompetitionMember acceptedMember = memberships.stream()
                    .filter(m -> m.getStatus() == MemberStatus.INVITED_ACCEPTED)
                    .findFirst()
                    .orElse(null);

            // 若非裁判且未加入任何已确认战队，禁止连接
            if (acceptedMember == null && !isReferee) {
                log.warn("[WebSocket-Chat] 拒绝连接: 用户未加入该比赛战队且无裁判权限: userId={}, compId={}", userId, competitionId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            String avatarUrl = userProfileRepository.findById(userId)
                    .map(UserProfile::getAvatarUrl)
                    .orElse(null);

            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_USERNAME, user.getUsername());
            attributes.put(ATTR_COMPETITION_ID, competitionId);
            attributes.put(ATTR_IS_REFEREE, isReferee);
            attributes.put(ATTR_AVATAR, avatarUrl);

            if (acceptedMember != null) {
                attributes.put(ATTR_TEAM_ID, acceptedMember.getTeam().getId());
                attributes.put(ATTR_TEAM_NAME, acceptedMember.getTeam().getTeamName());
                attributes.put(ATTR_ROLE, acceptedMember.getRole().name());
            } else {
                attributes.put(ATTR_ROLE, "REFEREE");
            }

            return true;

        } catch (Exception e) {
            log.warn("[WebSocket-Chat] 握手鉴权解析异常: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Handshake 完成后无需额外处理
    }

}
