# labor-tools

개인 사이트 + 연차계산 라이브러리 데모.
Spring Boot 4.1 / Java 21 / PostgreSQL 17 / Thymeleaf.

## 실행

```bash
docker compose up -d          # Postgres 기동
cp .env.example .env          # 값 채우기
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 공개: http://localhost:8080/posts
- 관리: http://localhost:8080/admin/posts

## 관리자 비밀번호 해시 만들기

`ADMIN_PASSWORD_HASH` 에는 평문이 아니라 BCrypt 해시를 넣는다.

```bash
# htpasswd 사용 (apache2-utils)
htpasswd -bnBC 12 "" '내비밀번호' | tr -d ':\n'
```

또는 임시 테스트 코드에서:

```java
System.out.println(new BCryptPasswordEncoder().encode("내비밀번호"));
```

해시에 `$` 가 포함되므로 셸/compose 에서 다룰 때 이스케이프에 주의.

## 스키마 변경

Hibernate `ddl-auto` 는 `validate`. 스키마의 소유자는 Flyway다.
변경이 필요하면 `src/main/resources/db/migration/V2__xxx.sql` 을 추가한다.
이미 적용된 마이그레이션 파일은 절대 수정하지 않는다.

## 다음 단계

- [ ] Phase 2: Dockerfile, 운영 compose, GitHub Actions self-hosted runner
- [ ] 연차계산기 데모 라우트 (`/tools/annual-leave`)
- [ ] 외부 공개 시: Cloudflare Tunnel, 도메인, sitemap/robots/JSON-LD
