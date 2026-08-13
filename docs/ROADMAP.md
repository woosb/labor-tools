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
| **연차 계산기** | **미착수 — 리포지토리 이름값을 아직 못 하고 있다** |
| 배포 (Docker / CI) | 미착수 |

---

## P1. 연차 계산기

이 저장소의 존재 이유인데 코드가 한 줄도 없다. 여기부터 하는 게 맞다.

### 설계 방향

`dev.sungbin.labortools.leave` 패키지를 새로 만들고
**Spring·JPA 의존성 없는 순수 도메인**으로 짠다.
README 가 "연차계산 라이브러리 데모"라고 말하는 이상,
나중에 별도 모듈로 떼어낼 수 있어야 한다.

```
leave/
  AnnualLeaveCalculator.java   순수 계산기. static 또는 무상태 빈
  LeaveInput.java              입사일, 기준일, 출근율, 산정방식
  LeaveResult.java             발생일수 + 산출 근거 목록
  AccrualBasis.java            enum: HIRE_DATE, FISCAL_YEAR
```

### 계산 규칙 (근로기준법 제60조)

> 구현 전에 조문 원문을 한 번 확인할 것. 아래는 기억 기반 요약이다.

- 계속근로 1년 미만: **1개월 개근마다 1일**, 최대 11일
- 1년간 출근율 80% 이상: **15일**
- 1년간 출근율 80% 미만(1년 이상 근속): 1개월 개근마다 1일
- 3년 이상 계속근로: 최초 1년을 초과하는 **매 2년마다 1일 가산**
- 가산 포함 **상한 25일**

### 결정해야 할 것

1. **입사일 기준만 지원할지, 회계연도 기준까지 지원할지.**
   회계연도 기준은 실무에서 더 많이 쓰이지만 비례 계산이 붙어 복잡해진다.
   1차는 입사일 기준만 하고 `AccrualBasis` enum 자리만 잡아두는 쪽을 권함.
2. **결과에 산출 근거를 담을지.** 담는 편이 데모로서 훨씬 설득력 있다.
   `LeaveResult` 에 `List<String> reasons` 정도.

### 라우트

`/tools/annual-leave` — GET 은 폼, POST 는 결과.
`PostController` 와 섞지 말고 `LeaveController` 를 따로 둔다.
`SecurityConfig` 는 `anyRequest().permitAll()` 이라 별도 설정 없이 공개된다.

### 테스트

계산기가 순수 함수라 테스트하기 가장 좋은 대상이다. 경계값 위주로:
입사 1개월/11개월/1년/3년/10년/25일 상한 도달, 출근율 79.9% vs 80%, 윤년.

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
