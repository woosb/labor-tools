# 로드맵

작성 기준: 2026-08-13, 최초 커밋 직후.
세팅 방법은 [README](../README.md) 에 있다. 이 문서는 **무엇을 할 차례인가**만 다룬다.

## 지금까지 된 것

| 영역 | 상태 |
|---|---|
| 글 CRUD + 발행/발행취소 | 완료 (`post` 패키지) |
| 마크다운 → HTML (GFM 테이블 포함) | 완료 (`MarkdownRenderer`) |
| 태그 파싱·정규화·재사용 | 완료 (`PostService.resolveTags`) |
| 관리자 인증 (환경변수 기반 1인) | 완료 (`SecurityConfig`) |
| 공개 목록/상세 + 페이지네이션 | 완료 |
| Flyway V1 스키마 | 완료 |
| 단위 테스트 | 25개 통과 |
| **연차 계산기** | **별도 저장소로 분리됨. 이 저장소에는 아직 연동 코드가 없다** |
| 배포 (Docker / CI) | 미착수 |

> **연차 계산 로직은 여기서 구현하지 않는다.**
> [woosb/annual-leave-kr](https://github.com/woosb/annual-leave-kr) 로 떼어냈다.
> 이 저장소가 할 일은 그 라이브러리를 **가져다 쓰는 것**뿐이다.

---

## P1. 연차 계산기 데모 라우트

계산 로직은 [annual-leave-kr](https://github.com/woosb/annual-leave-kr) 에 있다.
여기서 할 일은 **의존성으로 가져와 화면을 붙이는 것**이다. 로직을 다시 짜지 않는다.

### 먼저: 아직 의존성으로 못 쓴다

annual-leave-kr 은 `0.1.0-SNAPSHOT` 이고 **아직 어디에도 배포되지 않았다.**
Maven Central 업로드는 그쪽 저장소의 0.1.0 릴리즈 시점 과제다.
그래서 P1 은 다음 중 하나를 먼저 정해야 시작할 수 있다.

| 방법 | 장점 | 대가 |
|---|---|---|
| **A. `publishToMavenLocal`** | 지금 당장 됨 | CI 가 깨진다. GitHub Actions 러너에는 `~/.m2` 가 비어 있다 |
| **B. 컴포지트 빌드 (`includeBuild`)** | 라이브러리 수정이 즉시 반영, 배포 불필요 | 두 저장소가 나란히 클론돼 있어야 함. CI 는 checkout 을 두 번 해야 한다 |
| **C. Maven Central 배포를 먼저** | 가장 깨끗하고 CI 도 그냥 됨 | 배포 파이프라인부터 끝내야 해서 P1 이 뒤로 밀린다 |

**권장: 개발 중에는 B, 라우트가 완성되면 C 로 갈아탄다.**
A 는 "내 노트북에서만 되는 빌드" 를 만들기 때문에 피하는 게 좋다.

컴포지트 빌드는 `settings.gradle` 에 이렇게 붙인다.

```groovy
// settings.gradle — 두 저장소가 같은 부모 디렉터리에 있을 때
includeBuild '../annual-leave-kr'
```

```groovy
// build.gradle
implementation 'io.github.woosb:annual-leave-kr:0.1.0-SNAPSHOT'
```

경로에 의존하므로, 실제로 붙일 때는 디렉터리가 없을 경우
빌드가 알아볼 수 있는 메시지로 실패하게 감싸는 편이 낫다.

### 라이브러리가 지금 할 수 있는 것

**제60조 제1항(1년간 80% 이상 출근 시 15일)만 구현되어 있다.**
제2항(1년 미만 월차)과 제4항(3년 이상 가산)은 테스트가 `@Disabled` 인 상태다.
즉 지금 화면을 붙이면 **1년 미만 근속자와 3년 이상 근속자에게 0일이 나온다.**

라우트를 먼저 만들 거라면 이 한계를 화면에 명시하거나,
annual-leave-kr 에서 두 규칙을 먼저 구현하고 오는 게 낫다.
어느 쪽이든 **모르고 배포하면 안 되는 사실**이다.

### 공개 API

```java
var record = new AttendanceRecord(
        LocalDate.of(2024, 3, 15),   // hireDate  입사일
        LocalDate.of(2025, 3, 15),   // baseDate  산정 기준일
        248,                          // prescribedWorkingDays  소정근로일수
        248);                         // actualAttendanceDays   실제 출근일수

AnnualLeave result = AnnualLeaveCalculator.calculate(record);
result.totalDays();   // double
result.grants();      // List<Grant> — 일수 + 근거 조문 + 사람이 읽을 설명
```

`grants()` 가 근거 조문을 함께 주므로, 화면에 총 일수만 찍지 말고
**산출 근거를 같이 보여주는 게 이 데모의 핵심**이다. 그래야 계산기가 신뢰를 얻는다.

### 라우트

`/tools/annual-leave` — GET 은 폼, POST 는 결과.
`PostController` 와 섞지 말고 `LeaveController` 를 따로 둔다.
`SecurityConfig` 는 `anyRequest().permitAll()` 이라 별도 설정 없이 공개된다.

폼 입력은 `AttendanceRecord` 를 그대로 바인딩하지 말고
별도 `LeaveForm` 을 두어 검증 메시지를 한글로 낸다.
(`AttendanceRecord` 의 생성자 검증은 `IllegalArgumentException` 이라 폼 에러로 쓰기엔 거칠다.)

### 테스트

계산 로직 자체의 테스트는 라이브러리 쪽 책임이다. 여기서 중복해서 짜지 않는다.
이 저장소에서는 **폼 바인딩과 검증 메시지**만 덮으면 충분하다.

---

## P2. 배포

### Dockerfile

`build.gradle` 에 layered jar 를 이미 켜뒀으므로 layertools 로 레이어를 쪼갠다.

```dockerfile
# 대략 이런 형태
FROM eclipse-temurin:21-jre AS builder
WORKDIR /app
COPY build/libs/labor-tools-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

FROM eclipse-temurin:21-jre
# 레이어별 COPY (dependencies → spring-boot-loader → snapshot-dependencies → application)
```

> Spring Boot 4 에서 `-Djarmode=layertools` 가 `-Djarmode=tools extract` 로 바뀌었다.
> 빌드해보고 실제 옵션을 확인할 것.

### 운영 compose

현재 `compose.yml` 은 **개발 전용**이다. 건드리지 말고 `compose.prod.yml` 을 새로 만든다.
개발용과 달라야 하는 부분:

- DB 비밀번호를 `site/site` 하드코딩이 아니라 환경변수로
- Postgres 포트를 호스트에 노출하지 않기 (개발용은 `127.0.0.1:5432` 로 묶여 있음)
- 앱 컨테이너 추가, `depends_on` + healthcheck 연동
- `SPRING_PROFILES_ACTIVE=prod`, `prod` 프로파일 yml 신설

### GitHub Actions

테스트가 DB 없이 돌기 때문에 CI 는 지금 당장 붙일 수 있다.

```yaml
# .github/workflows/ci.yml — setup-java@v4 (temurin 21) + ./gradlew build
```

배포용 self-hosted runner 는 그 다음. CI 와 CD 워크플로를 파일부터 분리해둘 것.

---

## P3. 사이트 마무리

- [ ] **태그별 글 목록** — `Tag` 에 `slug` 를 만들어 저장하고 `idx_post_tag_tag_id` 인덱스까지
      깔아놨는데 이걸 쓰는 라우트가 없다. `/tags/{slug}` 를 만들거나, 안 쓸 거면 정리하자.
      `TagRepository` 에 `findBySlug` 부터 필요하다.
- [ ] **에러 페이지** — `PostNotFoundException` 이 `@ResponseStatus(NOT_FOUND)` 로 404 를 내지만
      템플릿이 없어 기본 Whitelabel 이 뜬다. `templates/error/404.html`, `5xx.html`.
- [ ] **공개 목록에 태그 노출** — `post/list.html` 은 현재 태그를 렌더링하지 않는다.
      추가한다면 `tags` 가 `LAZY` 라 N+1 이 생기므로 `@EntityGraph` 나 fetch join 을 같이 넣을 것.
      (지금은 렌더링하지 않으므로 문제없다.)
- [ ] **`<html lang="ko">`** — `fragments/common.html` 에 `lang` 속성이 없다.
- [ ] **헤더의 "관리" 링크** — 비로그인 사용자에게도 보인다.
      `sec:authorize` 로 감싸거나 그냥 두거나, 결정만 하면 된다.

## P4. 외부 공개 시

- [ ] Cloudflare Tunnel + 도메인
- [ ] `sitemap.xml`, `robots.txt`
- [ ] OG 태그 / `meta description` / JSON-LD — `fragments/common.html :: head` 확장
- [ ] `format_sql: true` 는 운영에서 끌 것 (`application.yml`)
- [ ] `/actuator/health` 가 `permitAll` 로 공개된다. 상세 정보는 안 나가지만 인지는 하고 있을 것

---

## 알려진 기술 부채

우선순위는 낮지만 잊으면 나중에 물린다.

1. **슬러그 중복 검사 레이스**
   `PostService.isSlugTaken` 은 check-then-insert 라 동시 요청 시 통과할 수 있고,
   그때는 DB unique 제약이 `DataIntegrityViolationException` 을 던져 500 이 된다.
   작성자가 1명이라 실제로 터질 일은 없지만, 잡아서 필드 에러로 바꿔주는 게 정석.

2. **리포지토리 쿼리에 테스트가 없다**
   특히 `PostRepository.incrementViewCount` 의 `@Modifying` JPQL 은 검증된 적이 없다.
   `@DataJpaTest` + Testcontainers 로 덮는 게 맞다.
   (H2 로 대체하면 Postgres 방언 차이 때문에 검증 가치가 떨어진다.)

3. **컨트롤러/시큐리티 테스트가 없다**
   `@WebMvcTest` 를 쓰려면 `SecurityConfig` 가 `AdminProperties` 를 요구하므로
   `@TestConfiguration` 으로 더미 값을 주입해야 한다. 이 걸림돌 때문에 1차에서는 뺐다.

4. **조회수가 봇 트래픽까지 센다**
   `readPublished` 가 요청마다 무조건 증가시킨다. 외부 공개하면 의미가 흐려진다.

5. **마크다운 sanitize 없음**
   의도적이다 (`MarkdownRenderer` 주석 참고). 작성자가 본인 1명이라 성립하는 전제이므로,
   **댓글이든 뭐든 외부 입력을 받는 순간 OWASP Java HTML Sanitizer 를 반드시 끼울 것.**
