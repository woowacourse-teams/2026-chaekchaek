# 모니터링 서버의 장애 알림

원본 알림은 Grafana에서 Slack으로 직접 전송한다. 분석기는 같은 알림의 식별 정보만 저장한다. 사용자가 원본 알림 스레드에서 봇을 멘션하면 발생 시점의 근거와 원인 후보를 답한다. 알림 수신만으로는 LLM을 호출하지 않는다. 분석기가 중단돼도 원본 알림 경로는 유지된다. 먼저 개발 채널에서 검증하고 운영 환경은 별도로 활성화한다.

## 1. Slack과 LLM 인증 정보 준비

1. Slack 개발 알림 채널을 정한다. [Slack 앱 관리](https://api.slack.com/apps)에서 앱을 생성한다.
2. `Incoming Webhooks`를 켜고 `Add New Webhook to Workspace`에서 개발 채널을 선택한다. 발급 URL은 채팅이나 Git에 남기지 않는다.
3. 서버의 `.env.incident.example`을 `.env.incident`로 복사하고 권한을 `600`으로 설정한다. `SLACK_WEBHOOK_URL_DEV`에 URL을 저장한다. 운영 채널은 별도 URL을 사용한다.
4. `GRAFANA_WEBHOOK_TOKEN`에는 무작위 32자 이상의 문자열을 저장한다. Grafana와 분석기가 같은 값을 사용한다. `INCIDENT_DB_PASSWORD`도 최초 실행 전에 정하고 이후 유지한다.
5. LLM을 연결할 때 OpenAI 프로젝트 API 키를 `LLM_API_KEY`, Responses API와 Structured Outputs를 지원하는 모델명을 `LLM_MODEL`에 저장한다. `LLM_ENABLED=false`, `ANALYSIS_ENVIRONMENTS=`는 기본값으로 유지한다.
6. `GRAFANA_URL=https://api.chaekchaek.com/grafana`로 설정한다. Slack 사용자가 접속할 기준 주소이며 대시보드의 `/d/...` 경로나 쿼리는 넣지 않는다. 답변 버튼은 이 주소의 `/explore`에 장애 발생 시각과 로그 조회 조건을 붙인다. SSH 터널로만 접속하는 환경에서는 `http://localhost:3000`을 사용할 수 있다.

비밀값 파일을 출력하는 `docker compose config` 대신 `docker compose config --quiet`로 검사한다. 원본 발송 URL은 Grafana가 사용하고, 스레드 답변은 별도의 봇 토큰을 사용한다. `.env.incident`는 저장소에 커밋하지 않는다.

`GRAFANA_URL`을 변경하면 같은 릴리스를 다시 배포해 환경 변수를 반영한다. 새 답변부터 변경된 주소를 사용하며, 이미 전송된 Slack 메시지의 링크는 자동으로 바뀌지 않는다.

## 2. 분석기 비활성 배포

저장소 루트에서 테스트와 패키징을 실행한다.

```sh
(cd incident-analyzer && ./gradlew test)
monitoring-server/scripts/package-incident.sh
```

출력된 `monitoring-server/releases/<commit>/` 디렉터리와 `scripts/deploy-incident.sh`를 EC2로 복사한다. 서버에는 Docker Compose, 기존 `chaekchaek-monitoring-server_default` 네트워크가 있어야 한다. 아래 명령은 EC2에서 실행한다.

```sh
chmod 600 ~/monitoring-server/.env.incident
MONITORING_DIR=~/monitoring-server bash ~/monitoring-server/scripts/deploy-incident.sh \
  ~/monitoring-server/releases/<commit>
```

이 명령은 분석기만 빌드하고 기동한다. JAR는 Java 바이트코드이므로 로컬과 EC2의 CPU 아키텍처가 달라도 사용하며, 런타임 이미지는 EC2에서 빌드한다. 외부 포트는 열지 않는다. 메모리 상한은 512 MiB, 힙은 192 MiB다. 2 GiB 서버에서 기존 프로세스와 합쳐 여유 메모리가 있는지 확인한다. 이 제한이 실제 부하 적합성을 보장하지는 않는다.

`prometheus/prometheus.yml`의 `incident-analyzer` scrape 설정을 실제 서버 파일에 반영한다. 기존 파일에 다른 설정이 있다면 해당 scrape 항목만 병합한다. `promtool check config` 후 `docker kill --signal=HUP chaekchaek-prometheus`로 설정을 다시 읽고 다음 쿼리가 `1`인지 확인한다. 설정 파일은 변경 전에 백업한다.

```promql
up{job="incident-analyzer"}
```

분석기 DB는 `chaekchaek-incident-data` 볼륨에 남는다. 롤백은 이전 릴리스 디렉터리를 같은 배포 스크립트에 전달한다. 자동 분석하던 이전 버전으로 롤백할 때는 먼저 `ANALYSIS_ENVIRONMENTS=`와 `LLM_ENABLED=false`로 바꾸고 원본 Slack 경로만 사용한다. `docker compose down -v`로 볼륨을 삭제하지 않는다.

## 3. 원본 Slack 알림부터 연결

아래 명령은 모니터링 서버 디렉터리에서 실행한다. 기존 `.env`에는 `GRAFANA_ADMIN_PASSWORD`가 있어야 한다. `.env.incident`는 이를 덮어쓰지 않는다.

```sh
python3 scripts/render-alerting.py --env dev --paused \
  --output grafana/provisioning/alerting/chaekchaek-dev.json

docker compose --env-file .env --env-file .env.incident \
  -f docker-compose.yml -f docker-compose.alerting.yml config --quiet

docker compose --env-file .env --env-file .env.incident \
  -f docker-compose.yml -f docker-compose.alerting.yml up -d --no-deps grafana
```

Grafana에서 `Alerting → Contact points → chaekchaek-dev-slack → Test`로 수신을 확인한다. 이 작업은 실제 채널에 테스트 알림을 보낸다. 아직 모든 규칙은 일시 정지 상태다.

수신 확인 후 `--paused`를 빼고 같은 파일을 다시 생성한다. `docker compose`의 같은 옵션으로 `restart grafana`를 실행하면 파일을 다시 읽는다. 생성 파일은 `policies`를 포함하지 않으므로 기존 정책 트리를 교체하지 않는다. 각 규칙이 직접 contact point를 지정한다.

| 규칙 | 초기 기준 |
| --- | --- |
| Backend5xx | 최근 5분 5xx 접근 로그 5건 이상, 1분 지속 |
| BackendScrapeUnavailable | 백엔드 scrape 실패 또는 시계열 부재, 2분 지속 |
| BackendMetricsStale | 마지막 실제 샘플이 90초 이상 지연, 1분 지속 |
| HostMemoryPressure | 호스트 가용 메모리 10% 미만, 3분 지속 |

트래픽에 맞춘 기준선은 실제 알림을 관찰한 뒤 조정한다. 5xx 건수가 빈 결과인 경우 이 규칙은 0으로 처리하며, 수집 경로 장애는 별도 scrape, freshness 규칙에서 감지한다. 로그 수집만 끊긴 경우를 독립적으로 감지하는 규칙은 아직 없다.

## 4. 멘션용 Slack 앱 연결

기존 Incoming Webhook이 사용자 지정 구형 연동이면 해당 연동에 Socket Mode를 추가할 수 없다. 원본 Webhook을 유지하고 [앱 manifest](slack-app-manifest.json)로 별도 앱을 만든다. 최신 Slack API 앱이면 필요한 설정을 기존 앱에 추가할 수 있다.

1. Slack 앱 관리의 `Create New App → From a manifest`에서 대상 워크스페이스를 선택하고 manifest를 붙여 넣는다. 앱 표시 이름은 `책췍 장애 도우미`, 멘션 이름은 `chaekchaek-bot`이다.
2. `OAuth & Permissions`에서 Bot Token Scopes `app_mentions:read`, `chat:write`, `channels:history`를 확인하고 워크스페이스에 설치한다. 발급된 `xoxb-` 토큰을 `SLACK_BOT_TOKEN`에 저장한다.
3. `Socket Mode`를 켜고 `Basic Information → App-Level Tokens`에서 `connections:write` 범위로 앱 토큰을 만든다. `xapp-` 토큰을 `SLACK_APP_TOKEN`에 저장한다. 공개 Request URL이나 새 EC2 인바운드 포트는 필요하지 않다.
4. `Event Subscriptions`를 켜고 Bot Events가 `app_mention` 하나인지 확인한다. manifest에 이미 포함되어 있으며 `message.channels` 구독은 필요하지 않다.
5. 개발 알림 채널의 `채널 세부정보 → 에이전트 및 앱`에서 봇을 추가한다. 채널 정보에 표시되는 ID를 `SLACK_CHANNEL_DEV`에 저장한다. 비공개 채널을 쓰려면 `groups:history` 권한을 추가하고 재설치한다.
6. 등록된 원본 Webhook 메시지의 `bot_id`를 `SLACK_ALERT_BOT_ID_DEV`에 저장한다. 이 값은 새 질문 봇의 ID가 아니다. 봇 토큰으로 `conversations.history`를 호출하여 원본 Grafana 메시지에서 확인한다. 인증 헤더는 보호된 파일에서 읽고 응답 전체나 토큰을 출력하지 않는다.
7. `ANALYSIS_ENVIRONMENTS=dev`, `SLACK_BOT_ENABLED=true`, `LLM_ENABLED=true`로 설정하고 새 버전 분석기를 배포한다. 이 설정에서도 사용자 질문이 없으면 모델을 호출하지 않는다. 운영용 토큰과 채널은 아직 활성화하지 않는다.

`incident_slack_connected` 지표가 `1`인지 확인한다. 연결이 끊기면 새 Socket URL을 발급받아 재접속한다. 연결 토큰이나 Socket URL에는 비밀값이 있으므로 로그에 남기지 않는다. 서버는 Slack의 HTTPS와 WSS에 아웃바운드로 접근할 수 있어야 한다.

## 5. 원본 알림과 질문을 함께 검증

새 분석기가 준비된 뒤 다음과 같이 Grafana 설정을 생성한다. 이미 동작 중인 개발 규칙이면 `--paused`를 생략하여 상태를 유지한다.

```sh
python3 scripts/render-alerting.py --env dev --with-questions --paused \
  --output grafana/provisioning/alerting/chaekchaek-dev.json
```

`chaekchaek-dev-enriched`라는 기존 contact point 이름과 UID를 유지한다. Slack 원본과 장애 정보 저장용 Webhook을 각각 전송하며 자동 분석은 하지 않는다. Slack 원본에는 기본 본문과 `CC-INCIDENT|환경|fingerprint|startsAt` 참조가 함께 들어간다. 이미 받은 과거 알림에는 참조가 없어 질문할 수 없으므로 설정 변경 후 새 알림으로 확인한다.

Grafana를 같은 compose 옵션으로 재시작하거나, 인증한 `POST /api/admin/provisioning/alerting/reload`로 파일을 다시 읽는다. 기존 정책 트리와 다른 알림 규칙이 유지됐는지 비교한다. Grafana 임의 테스트는 `env`와 `analysis_profile`이 없어 무시될 수 있으므로 해당 라벨이 포함된 별도 개발 테스트 규칙을 사용한다.

1. `env=dev`, `analysis_profile=api-errors`인 테스트 알림을 발생시킨다. 원본 Slack 수신, Webhook 접수, `incident_llm_tokens_total` 변화 없음을 확인한다.
2. 원본 알림의 스레드에서 `@chaekchaek-bot 이 상황에서 어디를 먼저 확인해야 하나요?`라고 묻는다. 답변이 같은 스레드에 한 번 오는지, 근거와 토큰 사용량이 기록되는지 확인한다.
3. 스레드 밖의 일반 멘션이나 봇 메시지에는 분석이 시작되지 않아야 한다. 복구 알림 이후 같은 원본 스레드에서 다시 질문해도 발생 시점의 자료를 기준으로 답해야 한다.
4. 테스트 규칙을 해제하고 개발 규칙의 평가 상태와 연결 상태를 확인한다. 검증이 끝나면 `--paused` 없이 생성해 활성화한다.

Webhook 계약은 [분석기 OpenAPI](../incident-analyzer/openapi.yaml)를 참고한다. `status=resolved`는 장애의 복구 상태를 저장하며 사용자 질문이나 준비된 답변을 취소하지 않는다. 동일 `event_id`를 다시 수신해도 질문은 한 건이다. 발송이 불확실한 네트워크 오류에서는 답변이 중복될 수 있으므로 정확히 한 번 전달을 보장하지 않는다.

실제 LLM 품질 검증은 외부 `429`, timeout, 내부 예외, 메모리 압박과 Broken pipe 동시 발생, 전체 자료 누락을 구분해 수행한다. 모의 서버 테스트와 연동 성공은 모델 정확도 평가를 대신하지 않는다. 실제 장애를 재현하지 않은 설정 테스트는 설정 검증으로 기록한다.

봇은 질문에 따라 DB 연결 대기 최댓값, HTTP p95, 오류 유형별 건수, 외부 API 실패별 건수 중 필요한 조회를 선택할 수 있다. 원본 장애의 환경과 발생 직전 15분을 기준으로 서버가 고정 쿼리를 실행한다. 추가 조회는 최대 4개, 모델 요청은 최대 2회다. Slack 권한과 인바운드 포트 변경은 필요하지 않으며 자유 쿼리, SQL, 자동 조치는 지원하지 않는다.

새 멘션으로 확인할 때는 답변의 추가 조회 상태와 Grafana 재현 버튼을 함께 확인한다. datasource UID는 `prometheus`, `loki`여야 한다. 버튼은 `GRAFANA_URL`의 `/explore`에서 같은 식과 시각의 instant query를 열며, 기본 오류 로그 버튼은 range query를 유지한다. `NO_DATA`는 정상 `0`과 다르다. HTTP 요청이 부족하면 p95가 `NaN`일 수 있고, 외부 API 상세 로그가 배포되지 않았으면 집계가 비어 있을 수 있다. 시간이나 두 번째 호출 한도가 부족하면 조회 결과와 제한 사유를 정적으로 표시한다.

## 6. 운영 적용과 중단

개발 환경에서 확인한 후에만 `SLACK_WEBHOOK_URL_PROD`를 설정하고 `--env prod`로 **별도 파일**을 생성한다. 운영 봇 채널과 원본 발신자도 별도로 등록한다. 분석 허용 환경은 `ANALYSIS_ENVIRONMENTS=dev,prod`로 명시한다. 개발 파일을 운영 파일로 덮어쓰지 않는다.

봇 응답을 중단하려면 `SLACK_BOT_ENABLED=false`로 바꾸고 분석기만 재배포한다. `ANALYSIS_ENVIRONMENTS=dev`를 유지하면 장애 정보는 계속 저장한다. 원본 Slack은 계속 동작한다. 모델 호출 한도는 하루 20회, 요청당 입력 20,000자와 출력 2,000토큰이며, 두 단계 답변은 호출 예약 2회를 쓴다. 실패한 요청의 예약도 재시작 후 남는다. `incident_llm_tokens_total`은 모델 응답 수신 시 집계하므로 결과 검증에 실패한 사용량도 포함하고 발송 재시도는 중복 집계하지 않는다. `LLM_ENABLED=false`만 설정하면 사람의 멘션에 비활성 안내를 답하지만 자동 메시지는 보내지 않는다.

분석기의 scrape 실패, Socket 연결 끊김, Slack 발송 실패, 모델 검증 실패는 `chaekchaek-<env>-slack`으로만 전달하여 재귀 분석을 방지한다. 분석기 자체의 과도한 알림은 규칙 일시 정지로 먼저 중단한다.

같은 EC2의 Grafana까지 멈추면 이 경로 전체가 중단된다. 모니터링 서버 자체 장애는 다른 위치의 가용성 검사로 보완해야 한다.

공식 형식은 [Grafana 파일 provisioning](https://grafana.com/docs/grafana/v13.2/alerting/set-up/provision-alerting-resources/file-provisioning/), [Explore URL](https://grafana.com/docs/grafana/v13.2/visualizations/explore/get-started-with-explore/#generate-explore-urls-from-external-tools), [Slack Incoming Webhooks](https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/), [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)에서 확인할 수 있다.

Slack 연결 방식은 [Socket Mode](https://docs.slack.dev/apis/events-api/using-socket-mode/), 원본 조회는 [conversations.history](https://docs.slack.dev/reference/methods/conversations.history/), 스레드 답변은 [chat.postMessage](https://docs.slack.dev/reference/methods/chat.postMessage/) 계약을 따른다.
