# ADR 001: 주력 백엔드 개발 언어를 Java에서 Kotlin으로 변경

- status: Accepted
- date: 2026-05-13
- decision-makers: 이윤서 (Backend Engineer)

## Context and Problem Statement
BLT 프로젝트는 1-2개월의 타이트한 MVP 개발을 요구하며 빠른 가설 검증과 출시가 최우선 과제이다.
또한 백엔드는 Apple HealthKit의 수면 데이터와 PVT(인지반응속도) 측정 결과라는 복잡하고 변동성이 큰 JSON 데이터를 클라이언트로부터 전달받아 처리해야 한다.
따라서 위 상황 속에서 보일러플레이트 코드를 최소화하고, 잦은 기획 변경 속에서 런타임 에러를 방지하며 빠르게 비즈니스 로직(Brain ROI 계산)을 검증할 수 있어야 한다.

## Decision Drivers
- Time-to-Market (1-2개월 기한)
- Data Instability
- YAGNI & KISS 원칙

## Considered Options
- Java 17+ (with Spring Boot 3.x)
- Kotlin 1.9+ (with Spring Boot 3.x)

## Decision Outcome
- Kotlin 1.9+ 채택
- because 압도적으로 코드가 간결하여 개발 속도를 높일 수 있고, 컴파일 타임의 Null Safety를 통해 데이터 스펙이 불안정한 MVP 단계에서 서버의 런타임 크래시를 원천 차단할 수 있기 때문임. 또한, 확장 함수를 통해 외부 라이브러리 없이도 YAGNI/KISS 원칙을 지키며 DTO 매핑이 가능.

## Pros and Cons of the Options
### Kotlin 1.9+
모던 IT 기업 및 스타트업 생태계의 Spring Boot 차세대 표준 언어.
- Good, because 언어 차원에서 Null Safety를 지원하여 런타임 안정성이 높음.
- Good, because data class 하나로 불변 객체 및 기본 메서드를 완벽하게 지원.
- Good, because 확장 함수를 통해 클래스 내부에 직관적인 매핑 로직을 구현하여 응집도를 높일 수 있음.
- Bad, because Java에 비해 참고 레퍼런스가 상대적으로 적을 수 있음.
- Bad, because 유일한 백엔드 개발자인 본인이 Java가 주력 언어이므로 learning curve가 있을 수 있음.
### Java 17+
기존 엔터프라이즈 표준이자 가장 거대한 생태계를 가진 JVM 언어.
- Good, because 본인에게 가장 익숙하며, 트러블슈팅을 위한 레퍼런스를 찾기 가장 쉬움.
- Good, because Spring 생태계와의 호환성이 가장 깊음.
- Bad, because 코드가 장황하여 Lombok이나 Record 등을 반드시 혼용해야 하며, 여전히 NPE의 위험이 존재함.
- Bad, because 객체 간의 변환 로직을 깔끔하게 유지하기 위해 MapStruct, ModelMapper 등의 추가 의존성이 강제되는 경향이 있음.

