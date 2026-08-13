# CLAUDE.md

Claude Code 로 이 저장소를 이어서 작업할 때의 컨텍스트.
세팅은 [README](README.md), 할 일은 [docs/ROADMAP.md](docs/ROADMAP.md).

## 빌드

```bash
./gradlew build        # 테스트 포함. DB 없이 통과해야 정상
```

JDK 21 필수. toolchain 자동 다운로드는 설정하지 않았다.

## 반드시 지킬 것

- **스키마는 Flyway 가 소유한다.** Hibernate `ddl-auto` 는 `validate` 다.
  스키마를 바꾸려면 `db/migration/V{n}__xxx.sql` 을 **새로** 추가한다.
  이미 적용된 마이그레이션 파일은 수정하지 않는다.
- **`spring.jpa.open-in-view` 는 `false` 다.** 되돌리지 않는다.
  뷰에서 지연 로딩이 필요하면 서비스 계층에서 미리 채워 넘긴다.
- **비밀값은 환경변수로만.** `.env` 는 커밋 금지, `.env.example` 만 갱신한다.
  `ADMIN_PASSWORD_HASH` 는 평문이 아니라 BCrypt 해시다.
- **`MarkdownRenderer` 에 sanitize 가 없다.** 작성자가 본인 1명이라는 전제 위에서만 안전하다.
  외부 입력(댓글 등)을 받는 기능을 추가한다면 sanitizer 를 같이 넣어야 한다.

## 코드 관례

- 엔티티는 setter 를 두지 않는다. `edit()`, `publish()`, `replaceTags()` 처럼
  의도가 드러나는 메서드로 상태를 바꾼다. 기본 생성자는 `protected`.
- 수정은 더티 체킹에 맡기고 `repository.save()` 를 다시 부르지 않는다
  (`PostService.update` 참고).
- 조회수처럼 경합이 있는 카운터는 엔티티가 아니라 `@Modifying` 벌크 쿼리로 올린다.
  `updatedAt` 이 같이 밀리는 걸 막기 위함이다.
- 서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.
- 주석은 "무엇을" 이 아니라 **"왜"** 를 적는다. 기존 주석들의 톤을 따를 것.
- 테스트 메서드 이름은 한글 스네이크 케이스다 (`재발행해도_최초_발행일시는_유지된다`).

## 테스트 방침

현재 25개 전부 **Spring 컨텍스트를 띄우지 않는 순수 단위 테스트**다.
이 덕분에 Postgres 없이 CI 가 돈다. 이 성질을 깨지 않는 게 좋다.

`@SpringBootTest` 는 Postgres 와 `ADMIN_PASSWORD_HASH` 를 요구해서 그냥 붙이면 CI 가 깨진다.
리포지토리 쿼리를 테스트하려면 Testcontainers 를 도입하고,
그 시점에 CI 워크플로도 같이 손봐야 한다.

## 도메인 메모

연차 계산 로직(P1)은 `dev.sungbin.labortools.leave` 에 **Spring 의존성 없이** 짠다.
나중에 라이브러리로 떼어내는 게 목표라서다. 근거 조문은 근로기준법 제60조이고,
구현 전에 조문 원문을 확인할 것 — 로드맵의 요약은 기억 기반이다.
