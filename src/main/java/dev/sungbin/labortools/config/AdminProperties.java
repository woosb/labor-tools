package dev.sungbin.labortools.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 계정 1개. DB에 사용자 테이블을 두지 않는다.
 * passwordHash 는 BCrypt 해시이며 환경변수로 주입한다.
 */
@ConfigurationProperties(prefix = "site.admin")
public record AdminProperties(String username, String passwordHash) {
}
