package com.dayone.security;

import com.dayone.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1 hour
    private static final String KEY_ROLES = "roles";

    private final MemberService memberService;

    @Value("${spring.jwt.secret}")
    private String secretKey;

    /**
     * JWT 토큰 생성 메서드
     * @param username
     * @param roles
     * @return 생성된 JWT 토큰
     */
    public String generateToken(String username, List<String> roles) {
        // 토큰 클레임에 사용자명과 역할 정보를 추가
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        // 현재 시간과 만료 시간 설정
        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        // JWT 토큰 생성 및 반환
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)  // 생성시간
                .setExpiration(validity)  // 만료시간
                .signWith(SignatureAlgorithm.HS256, secretKey)  // 서명 알고리즘 및 시크릿 키 설정
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 정보 조회 메서드
     * @param jwt
     * @return 사용자 정보가 포함된 Authentication 객체
     */
    public Authentication getAuthentication(String jwt) {
        // JWT에서 사용자명을 추출하고, 해당 사용자로 Authentication 생성
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * JWT 토큰에서 사용자명 추출
     * @param token
     * @return 토큰에 포함된 사용자명
     */
    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token
     * @return 토큰이 유효한지 여부
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) return false;

        Claims claims = this.parseClaims(token);
        return !claims.getExpiration().before(new Date());  // 만료 시간 체크
    }

    /**
     * JWT 토큰의 클레임을 파싱
     * @param token
     * @return 파싱된 Claims 객체
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(this.secretKey)  // 시크릿 키를 사용해 토큰 서명 검증
                    .parseClaimsJws(token)  // JWT 파싱
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();  // 만료된 토큰의 경우에도 클레임 반환
        }
    }

}

