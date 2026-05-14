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