#!/usr/bin/env bash
set -euo pipefail

# SSM에서 전달받은 커밋 SHA로 배포할 JAR를 구분한다.
release=${1:-}
[[ "$release" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid commit SHA' >&2; exit 1; }

# 배포 대상 버킷은 root만 수정할 수 있는 서버 설정에서 읽는다.
source /etc/lottecinema/deploy.env
: "${DEPLOY_BUCKET:?DEPLOY_BUCKET must be set}"

# 같은 EC2에 두 배포가 동시에 파일을 교체하지 않도록 잠근다.
exec 9>/run/lock/lottecinema-deploy.lock
flock -n 9 || { echo 'Another deployment is running' >&2; exit 1; }

cd /opt/lottecinema
candidate=$(mktemp ./candidate.XXXXXX)
trap 'rm -f -- "$candidate"' EXIT

# 새 JAR를 먼저 내려받아 검증하고, 성공했을 때만 실행 파일을 교체한다.
aws s3 cp "s3://$DEPLOY_BUCKET/releases/$release/app.jar" "$candidate" \
  --region ap-northeast-2 --only-show-errors
test -s "$candidate"
chmod 0644 "$candidate"

# 최초 배포에는 이전 JAR가 없으므로 백업을 건너뛴다.
had_previous=false
if [ -f app.jar ]; then
  cp -p app.jar previous.jar
  had_previous=true
fi
mv -f "$candidate" app.jar

# 앱이 시작되어 OpenAPI 문서를 반환할 때까지 기다린다.
wait_for_swagger() {
  for attempt in $(seq 1 30); do
    if curl -fsS --max-time 2 -o /dev/null http://127.0.0.1:8080/v3/api-docs; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if systemctl restart lottecinema && wait_for_swagger; then
  echo "Deployed $release"
  exit 0
fi

# 실패한 배포는 GitHub Actions에 실패로 알리고, 이전 JAR가 있으면 복구한다.
echo 'Deployment failed.' >&2
if [ "$had_previous" = true ]; then
  echo 'Restoring the previous JAR.' >&2
  mv -f previous.jar app.jar
  systemctl reset-failed lottecinema
  systemctl restart lottecinema
  wait_for_swagger || echo 'Rollback health check failed' >&2
else
  echo 'There is no previous JAR to restore.' >&2
  systemctl stop lottecinema || true
  systemctl reset-failed lottecinema
  rm -f app.jar
fi
exit 1
