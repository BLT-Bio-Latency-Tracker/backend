# 🧠 BLT (Bio-Latency Tracker) Backend

> *"우리는 왜 가장 피곤할 때 가장 중요한 일을 하고 있을까?"*

**BLT**는 애플 헬스 데이터(수면)와 인지반응속도(PVT)를 결합해 유저의 뇌 컨디션(Brain ROI)을 수치화하고, 현재 상태에 최적화된 작업을 제안하는 헬스케어 서비스입니다.

## 🎯 Core Features
- **뇌 컨디션 진단 (Brain Condition Check):** 30초 분량의 짧은 인지반응속도(PVT) 테스트를 진행하면, 즉각적으로 유저의 당일 수면 데이터(Apple Health)와 결합하여 현재 뇌가 발휘할 수 있는 퍼포먼스를 'Brain ROI' 점수로 직관적으로 보여줍니다.
- **상태 맞춤형 업무 추천 (Condition-Based Task Recommendation):** 메디컬 팀의 연구를 기반으로 산출된 ROI 점수에 맞춰, 지금 당장 효율을 극대화할 수 있는 업무 유형을 제안합니다.
- **안전하고 투명한 건강 데이터 관리 (Secure Healthcare Data Management):** 유저의 민감한 수면 데이터를 다루는 만큼, 철저한 권한 동의 플로우를 제공합니다. 회원가입 전 '게스트 모드'를 통해 기능을 미리 체험할 수 있으며, 탈퇴 시 유저의 건강 데이터는 즉각 파기(논리적 삭제)되어 프라이버시를 완벽하게 보호합니다.

## 🛠 Tech Stack & Architecture

- **Language:** Kotlin 1.9+
- **Framework:** Spring Boot 3.3.x, Spring Data JPA, Spring Security
- **Database:** PostgreSQL 16
- **Infrastructure:** AWS
- **CI/CD:** GitHub Actions + Docker + EC2
- **API Spec:** OpenAPI (Swagger 3.0), RFC 7807 `ProblemDetail` 글로벌 에러 핸들링
- **Migration:** Flyway

## 📁 Project Structure
DDD Layered Architecture를 사용합니다.

```text
src/main/kotlin/com/medilux/blt
├── BltApplication.java
├── global
│   ├── config        // SecurityConfig, JpaConfig, WebConfig
│   ├── security      // JwtProvider, AppleOAuthClient, SecurityFilter
│   ├── exception     // GlobalExceptionHandler, BltException, ErrorCode
│   ├── common        // BaseEntity, ApiResponse, Pagination
│   └── util
├── auth              // 애플 로그인, 토큰 발급/갱신
├── user              // 유저 정보, 동의 관리
├── sleep             // HealthKit 수면 데이터 수집/저장
├── pvt               // PVT 테스트 결과 수집
└── brainroi          // 점수 계산 + 추천
    ├── controller
    ├── service
    ├── calculator    // ⭐ 계산식 분리
    ├── recommender
    ├── dto
    └── domain        // BrainRoiScore, Recommendation
```

---

## ⚙️ 사전 요구사항 (Prerequisites)

| 항목 | 버전 / 비고 |
|---|---|
| **JDK** | 21 (Gradle Toolchain 기준) |
| **Docker / Docker Compose** | DB(PostgreSQL 16) 및 테스트(Testcontainers) 실행에 필요 |
| **Gradle** | 설치 불필요 — 저장소의 `./gradlew`(Wrapper, 8.14.4) 사용 |

> 💡 모든 명령은 프로젝트 루트에서 실행합니다. Windows는 `./gradlew` 대신 `gradlew.bat` 사용.

---

## 🚀 빌드 & 실행 (Local)

### 1단계 — 환경변수 파일 생성
```bash
cp .env.example .env
```
- `.env` 의 `POSTGRES_*` 값은 Docker Compose(DB)가 사용합니다.
- 로컬 프로파일은 `application-local.yml` 에 기본값이 있어 별도 시크릿 없이 바로 실행됩니다.

### 2단계 — 데이터베이스 기동 (PostgreSQL 16)
```bash
docker compose up -d postgres        # v2 (구버전은 docker-compose up -d)
```
```bash
docker compose ps                    # 상태 확인 (healthy 인지)
```

### 3단계 — 애플리케이션 실행
```bash
./gradlew bootRun                    # 기본 프로파일: local
```
- 접속: http://localhost:8080
- API 문서(Swagger): http://localhost:8080/swagger-ui.html
- 로컬은 `ddl-auto=update` 라 스키마가 엔티티 기준으로 자동 생성됩니다.

### (선택) 프로파일 지정 실행
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
# 또는
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## 📦 빌드 (Jar 패키징)

```bash
./gradlew clean build                # 컴파일 + 린트 + 테스트 + Jar
```
```bash
./gradlew bootJar                    # 실행 가능한 Jar만 생성
```
```bash
java -jar build/libs/blt-0.0.1-SNAPSHOT.jar
# 프로파일 지정:
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/blt-0.0.1-SNAPSHOT.jar
```
> ⚠️ `build`/`test` 는 **Testcontainers** 로 실제 PostgreSQL을 띄우므로 **Docker 데몬이 실행 중**이어야 합니다.
> 테스트 없이 빌드만: `./gradlew build -x test`

---

## 🧪 테스트 & 코드 스타일

```bash
./gradlew test                       # 전체 테스트 (Testcontainers → Docker 필요)
./gradlew test --tests "com.medilux.blt.domain.notification.*"   # 특정 테스트만
```
```bash
./gradlew ktlintCheck                # 코드 스타일 검사
./gradlew ktlintFormat               # 자동 포맷 적용
```
```bash
./gradlew build                      # 컴파일 + ktlint + 전체 테스트 (CI 동등)
```

---

## 🌱 프로파일 & 환경변수

| 프로파일 | `ddl-auto` | Swagger | 용도 |
|---|---|---|---|
| `local` (기본) | `update` | ON | 로컬 개발 (기본값 내장) |
| `dev` | `validate` | ON | 개발 서버 |
| `prod` | `validate` | OFF | 운영 |

주요 환경변수 (`.env.example` 참고):
- **DB**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `POSTGRES_*`
- **JWT**: `JWT_SECRET`(64byte 권장), `JWT_*_TTL_*`
- **Apple**: `APPLE_ISSUER`, `APPLE_CLIENT_ID`(Bundle ID)
- **FCM(푸시)**: `FCM_ENABLED`, `FCM_CREDENTIALS_PATH`, `FCM_PROJECT_ID`

dev/prod DB는 전용 compose 파일 사용:
```bash
docker compose -f docker-compose.dev.yml up -d      # 개발
docker compose -f docker-compose.prod.yml up -d     # 운영
```

---

## 🔔 FCM (푸시 알림)

| 환경 | 설정 |
|---|---|
| **local / test** | `FCM_ENABLED=false` — 실제 발송 없이 로그만(`LoggingPushSender`), 크레덴셜 불필요 |
| **prod** | `FCM_ENABLED=true` + `FCM_CREDENTIALS_PATH`(서비스계정 JSON 경로). Firebase 콘솔에 APNs 인증키(.p8) 업로드 필요 |

---

## 🛑 종료 / 정리

```bash
docker compose down                  # DB 컨테이너 중지
docker compose down -v               # DB 컨테이너 + 데이터 볼륨까지 삭제
./gradlew --stop                     # Gradle 데몬 종료
```