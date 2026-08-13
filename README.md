# labor-tools

개인 사이트 + 연차계산 라이브러리 데모.
Spring Boot 4.1 / Java 21 / PostgreSQL 17 / Thymeleaf.

> 지금까지 된 것과 다음에 할 것은 [docs/ROADMAP.md](docs/ROADMAP.md) 에 정리해두었다.

## 처음 세팅 (클론 직후)

### 1. JDK 21

`build.gradle` 에 toolchain 21 이 걸려 있다.
자동 다운로드(foojay resolver)는 붙여두지 않았으므로 JDK 21 이 없으면
`No matching toolchains found` 로 빌드가 실패한다.

```bash
# macOS
brew install --cask temurin@21

# 설치 확인
/usr/libexec/java_home -V
```

### 2. 검증 — DB 없이 여기까지 통과해야 정상

```bash
./gradlew build
```

테스트는 전부 Spring 컨텍스트를 띄우지 않는 단위 테스트라
Docker 도 `.env` 도 없이 통과한다. 여기서 깨지면 JDK 문제다.

### 3. Postgres

```bash
docker compose up -d
```

### 4. `.env`

**이 파일이 없으면 앱이 기동조차 하지 않는다.**
`ADMIN_PASSWORD_HASH` 에 기본값을 두지 않았기 때문에
`Could not resolve placeholder 'ADMIN_PASSWORD_HASH'` 로 죽는다. 의도된 동작이다.

```bash
cp .env.example .env
```

해시 생성은 아래 [관리자 비밀번호 해시](#관리자-비밀번호-해시) 참고.

### 5. 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 공개: http://localhost:8080/posts
- 관리: http://localhost:8080/admin/posts

DB 는 머신에 종속적이다. 새 머신에서 처음 띄우면 Flyway 가 스키마만 만들고
글이 0개인 상태로 시작한다.

## 관리자 비밀번호 해시

`ADMIN_PASSWORD_HASH` 에는 평문이 아니라 BCrypt 해시를 넣는다.

```bash
htpasswd -bnBC 12 "" '내비밀번호' | tr -d ':\n'
```

`htpasswd` 는 macOS 에 기본 내장이고, Linux 는 `apache2-utils` 가 필요하다.
또는 임시 테스트 코드에서:

```java
System.out.println(new BCryptPasswordEncoder().encode("내비밀번호"));
```

해시에 `$` 가 포함되므로 셸/compose 에서 다룰 때 이스케이프에 주의.

## 스키마 변경

Hibernate `ddl-auto` 는 `validate`. 스키마의 소유자는 Flyway다.
변경이 필요하면 `src/main/resources/db/migration/V2__xxx.sql` 을 추가한다.
이미 적용된 마이그레이션 파일은 절대 수정하지 않는다.

## 프로젝트 구조

```
src/main/java/dev/sungbin/labortools/
  LaborToolsApplication.java
  config/     AdminProperties, SecurityConfig   — 관리자 1명, DB 사용자 테이블 없음
  post/       글 도메인 전체 (엔티티/서비스/컨트롤러/폼/마크다운 렌더러)
src/main/resources/
  db/migration/   Flyway. V1 이 post/tag/post_tag 를 만든다
  templates/      Thymeleaf. fragments/common.html 이 head 와 header 를 준다
src/test/java/    Spring 컨텍스트 없는 순수 단위 테스트 25개
```
