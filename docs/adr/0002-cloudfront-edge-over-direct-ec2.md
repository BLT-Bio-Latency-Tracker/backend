# ADR 002: API 엣지 구조로 직접 노출(Route53→EC2) 대신 CloudFront(캐시 Off) 경유 채택

- status: Accepted
- date: 2026-06-25
- decision-makers: 이윤서 (Backend Engineer)

## Context and Problem Statement
BLT 백엔드는 `api.bryki.site` 단일 API 도메인만 공개한다(웹 프론트 없음, 클라이언트는 네이티브 iOS 앱). 오리진은 **EC2 t3.small 1대(nginx+Spring Boot, docker compose)** 이며, 운영 보안상 **SSH를 닫고 SSM Session Manager로만 접근**한다. 처리 데이터는 수면·PVT·Brain ROI 등 **민감한 건강 정보**다.
이 도메인에 HTTPS를 제공하려면 "어디서 TLS를 종료하고, 오리진을 어떻게 보호하며, 인증서를 누가 갱신할지"를 정해야 한다. 핵심 제약은 **SSH가 없는 단일 EC2에서 인증서 자동 갱신을 운영하는 부담**과 **오리진이 인터넷에 직접 노출되는 위험**이다.

## Decision Drivers
- 인증서 발급/갱신 운영 부담 최소화 (SSH 닫힌 SSM-only EC2)
- 오리진(EC2) IP 은닉 및 공격 표면 축소
- 민감 건강 데이터에 대한 DDoS/확장 보안(WAF) 대비
- MVP 비용 효율 (저트래픽, 추가 고정비 회피)

## Considered Options
- Route53 → EC2(nginx) 직접 노출 (nginx에서 Let's Encrypt/certbot으로 TLS 종료)
- Route53 → CloudFront(캐시 Off) → EC2(nginx) (CloudFront에서 ACM으로 TLS 종료)

## Decision Outcome
- **Route53 → CloudFront(캐시 Off) → EC2** 채택
- because ACM 무료 인증서로 **TLS 발급·갱신이 완전 자동화**되어 SSH 없는 EC2에서 certbot을 운영할 필요가 사라지고, EC2 보안그룹을 **CloudFront prefix list로만** 열고 **`X-Origin-Secret` 헤더**를 검증해 오리진 IP를 숨기고 공격 표면을 좁힐 수 있으며, **AWS Shield Standard(무료)** 로 DDoS를 흡수하고 추후 **WAF/레이트리밋을 앱 변경 없이** 부착할 수 있기 때문임. 캐시는 동적·인증 API라 이득이 없고 유저 간 응답 혼선 위험만 있으므로 **명시적으로 비활성화(CachingDisabled)** 하되, 캐시 외의 위 이점만 취한다. 비용도 **영구 무료 한도(1TB·요청 1천만/월)** 내라 MVP 구간에서 사실상 $0이다.

## Risk & Remediation — 오리진 구간 TLS (CloudFront → EC2)
- **알려진 위험**: CloudFront에서 TLS를 종료하므로 **CloudFront 엣지 → EC2(공인 IP) 구간은 평문 HTTP**다. 오리진이 EC2 공인 IP(EIP)라 이 구간은 VPC 사설 링크가 아니며, 민감 건강 데이터(HealthKit/PVT)가 이 홉에서 비암호화로 흐른다.
- **완화(현재)**: SG inbound를 **CloudFront origin-facing prefix list로만** 제한 + **`X-Origin-Secret` 헤더** 검증 + AWS 백본 경유(공개 인터넷 노출 최소화). 단, 이는 **접근 제어이지 전송 암호화가 아니다**(보안 감사 기준 미달 인지).
- **현재 결정(D안 — MVP 한시적 위험 수용)**: **출시 전이며 실 사용자 건강 PII가 흐르지 않는** 단계이므로, ALB/추가 인증서 운영의 비용·복잡도를 지금 떠안지 않고 HTTP 오리진을 **의식적·한시적으로** 유지한다.
- **리메디에이션 트리거(반드시)**: **프로덕션 GA 또는 실 사용자 건강 PII 유입 이전**에 **End-to-End TLS로 전환**한다.
- **목표 구성**: **ALB + ACM**(권장 — 인증서를 AWS가 관리해 ADR-0002의 "EC2 인증서 운영 회피" 철학 유지; CloudFront→ALB HTTPS, ALB→EC2는 VPC 사설). 대안: CloudFront **VPC origin**(공인 IP 제거) 또는 nginx+Let's Encrypt(차선 — certbot 운영이 EC2로 복귀).
- ⚠️ 검토 메모: CloudFront 커스텀 오리진 HTTPS는 **공인 CA 인증서만** 허용(self-signed 불가), **ACM 공개 인증서는 nginx에 설치 불가**(AWS 서비스 전용). 따라서 "nginx에 self-signed/ACM" 방식은 성립하지 않음 → 전환 시 ALB+ACM 또는 VPC origin이 정석.

> 이 섹션은 CodeRabbit 보안 리뷰(오리진 구간 평문 전송, Major)를 반영한 **의식적 위험 수용 + 리메디에이션 계획**이다. 위 트리거 충족 시 본 HTTP-오리진 결정은 폐기되고 E2E TLS로 대체된다.

## Pros and Cons of the Options
### Route53 → CloudFront(캐시 Off) → EC2
TLS 종료·오리진 은닉·DDoS 방어를 엣지에 위임하는 표준 구성.
- Good, because ACM 무료 인증서로 인증서 발급/갱신이 자동화되어 SSM-only EC2의 운영 부담이 사라짐.
- Good, because SG를 CloudFront prefix list로만 열고 `X-Origin-Secret`을 검증해 오리진 IP 은닉 + 네트워크/애플리케이션 이중 잠금이 됨.
- Good, because AWS Shield Standard(무료) DDoS 방어와 WAF/지오차단/레이트리밋 확장점을 앱 코드 변경 없이 확보.
- Good, because 가장 가까운 엣지에서 TLS 악수를 종료하고 오리진 구간은 AWS 백본을 타 지연이 줄며, 오리진→CloudFront 전송은 무과금.
- Bad, because 구성 요소(분배·오리진·prefix list·헤더)가 늘어 초기 설정과 디버깅 동선이 길어짐.
- Bad, because ACM 인증서를 us-east-1에 별도 발급해야 하고, 오리진 구간(CloudFront→EC2)은 HTTP라 비암호화(비밀 헤더+SG로 보완).

### Route53 → EC2(nginx) 직접 노출
가장 단순하고 홉이 적은 구성.
- Good, because 구조가 단순하고 홉이 적어 초기 셋업과 트러블슈팅이 직관적임.
- Good, because nginx↔클라이언트 구간까지 종단 HTTPS가 가능.
- Bad, because SSH 닫힌 SSM-only EC2에서 certbot 자동 갱신을 운영해야 해 깨지기 쉽고 관리 부담이 큼.
- Bad, because EC2 퍼블릭 IP가 인터넷에 직접 노출되어 스캐닝·DDoS 표면이 커지고, 단일 t3.small이 공격을 그대로 받음.
- Bad, because WAF/레이트리밋/지오차단 등 보안 확장을 붙일 표준 지점이 없어 앱이나 nginx에서 직접 떠안아야 함.
