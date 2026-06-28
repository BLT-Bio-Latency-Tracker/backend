#!/usr/bin/env bash
# BLT prod 배포 스크립트 — EC2에서 SSM Run Command가 실행한다(SSH 없음).
#
# 전제: CWD = 레포 루트(예: /opt/blt)이며 배포할 git sha로 checkout되어
#       docker-compose.prod.yml / infra/nginx 가 그 sha와 일치한다.
# 권장 SendCommand: cd /opt/blt && git fetch --all && git checkout <sha> \
#                     && IMAGE_TAG=<sha> ECR_IMAGE=<repo-uri> bash infra/deploy.sh
#
# 동작: SSM(/blt/prod/*) → .env(보간/nginx) + app.env(런타임) 생성 → FCM json 파일화 → ECR 로그인
#       → compose pull & up → /actuator/health 게이트 → 실패 시 이전 이미지로 자동 롤백.
set -euo pipefail

### 입력 (SendCommand가 env로 주입) -------------------------------------------
: "${IMAGE_TAG:?IMAGE_TAG(git sha) 가 필요합니다}"
: "${ECR_IMAGE:?ECR_IMAGE(repo URI, 예: 1234.dkr.ecr.ap-northeast-2.amazonaws.com/blt) 가 필요합니다}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
SSM_PREFIX="${SSM_PREFIX:-/blt/prod}"
COMPOSE_FILE="docker-compose.prod.yml"
HEALTH_URL="http://localhost/actuator/health"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"   # 30 × 5s = 최대 150s 대기
ECR_REGISTRY="${ECR_IMAGE%%/*}"

log() { echo "[deploy $(date -u +%H:%M:%S)] $*"; }

### 0. 이전 태그 보존(롤백 대상) ---------------------------------------------
PREV_TAG=""
[[ -f .env ]] && PREV_TAG="$(grep -E '^IMAGE_TAG=' .env | cut -d= -f2- || true)"
log "deploy ${ECR_IMAGE}:${IMAGE_TAG} (prev=${PREV_TAG:-none})"

### 1. SSM → .env 생성 (시크릿 회전 반영 위해 매 배포 재생성) -----------------
log "fetch secrets: SSM ${SSM_PREFIX}"
TMP_INFRA="$(mktemp)"   # .env    → compose 보간 + nginx 전용
TMP_APP="$(mktemp)"     # app.env → app 컨테이너 런타임 주입
# /blt/prod/db-url → DB_URL 형태로 변환. 라우팅:
#   ORIGIN_SECRET → .env(nginx/보간) / 그 외 → app.env(런타임) / FCM json → 파일(아래)
aws ssm get-parameters-by-path \
    --path "$SSM_PREFIX" --recursive --with-decryption \
    --region "$AWS_REGION" \
    --query 'Parameters[].[Name,Value]' --output text \
| while IFS=$'\t' read -r name value; do
    key="$(basename "$name" | tr '[:lower:]-' '[:upper:]_')"
    case "$key" in
      FCM_CREDENTIALS_JSON) continue ;;
      ORIGIN_SECRET) printf '%s=%s\n' "$key" "$value" >> "$TMP_INFRA" ;;
      *)             printf '%s=%s\n' "$key" "$value" >> "$TMP_APP" ;;
    esac
  done
# 배포 메타(compose 보간용)는 .env 로
{ printf 'IMAGE_TAG=%s\n' "$IMAGE_TAG"; printf 'ECR_IMAGE=%s\n' "$ECR_IMAGE"; } >> "$TMP_INFRA"
mv "$TMP_INFRA" .env     && chmod 600 .env
mv "$TMP_APP"   app.env  && chmod 600 app.env

### 2. FCM 자격증명 파일화 (compose가 컨테이너에 마운트) ----------------------
mkdir -p secrets
if aws ssm get-parameter --name "${SSM_PREFIX}/fcm-credentials-json" \
      --with-decryption --region "$AWS_REGION" \
      --query 'Parameter.Value' --output text > secrets/fcm.json 2>/dev/null; then
  log "fcm credentials written"
else
  echo '{}' > secrets/fcm.json
  log "no fcm credentials in SSM → placeholder {}"
fi
chmod 600 secrets/fcm.json

### 3. ECR 로그인 & pull -----------------------------------------------------
log "ecr login: ${ECR_REGISTRY}"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"
log "pull images"
docker compose -f "$COMPOSE_FILE" pull

### 4. 기동 -----------------------------------------------------------------
log "compose up"
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

### 5. 헬스 게이트 (nginx 경유 /actuator/health) -----------------------------
log "health gate: ${HEALTH_URL}"
healthy=false
for _ in $(seq 1 "$HEALTH_RETRIES"); do
  if curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
    healthy=true; break
  fi
  sleep 5
done

if [[ "$healthy" == true ]]; then
  log "✅ healthy — deploy ok (${IMAGE_TAG})"
  docker image prune -f >/dev/null 2>&1 || true
  exit 0
fi

### 6. 자동 롤백 -------------------------------------------------------------
log "❌ health check failed after $((HEALTH_RETRIES*5))s"
if [[ -n "$PREV_TAG" && "$PREV_TAG" != "$IMAGE_TAG" ]]; then
  log "rollback → ${PREV_TAG}"
  sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${PREV_TAG}/" .env
  docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
  log "rolled back to ${PREV_TAG}"
else
  log "no previous tag → rollback 불가(첫 배포)"
fi
exit 1
