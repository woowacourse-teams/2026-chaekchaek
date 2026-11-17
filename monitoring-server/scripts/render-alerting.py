#!/usr/bin/env python3
"""기존 정책 트리를 변경하지 않는 Grafana alerting provisioning 파일을 생성한다."""
import argparse
import json
from pathlib import Path


ALERT_TEXT = """{{ template "default.message" . }}
{{ range .Alerts }}
`CC-INCIDENT|{{ .Labels.env }}|{{ .Fingerprint }}|{{ .StartsAt.Format "2006-01-02T15:04:05.999999999Z07:00" }}`
{{ end }}
질문은 이 알림 스레드에서 @chaekchaek-bot을 멘션해 주세요."""


def contact_point(environment, context_enabled):
    receivers = [{
        "uid": f"cc-{environment}-slack-{'ai' if context_enabled else 'base'}",
        "type": "slack",
        "disableResolveMessage": False,
        "settings": {"url": f"$SLACK_WEBHOOK_URL_{environment.upper()}"},
    }]
    if context_enabled:
        receivers[0]["settings"]["text"] = ALERT_TEXT
        receivers.append({
            "uid": f"cc-{environment}-analyzer",
            "type": "webhook",
            "disableResolveMessage": False,
            "settings": {
                "url": "http://incident-analyzer:8080/internal/alerts/grafana",
                "httpMethod": "POST",
                "authorization_scheme": "Bearer",
                "authorization_credentials": "$GRAFANA_WEBHOOK_TOKEN",
                "maxAlerts": "100",
            },
        })
    suffix = "enriched" if context_enabled else "slack"
    return {"orgId": 1, "name": f"chaekchaek-{environment}-{suffix}", "receivers": receivers}


def rule(environment, key, title, expression, datasource, threshold, pending, profile, receiver, paused):
    return {
        "uid": f"cc-{environment}-{key}",
        "title": f"{title}-{environment}",
        "condition": "B",
        "data": [
            {
                "refId": "A",
                "relativeTimeRange": {"from": 600, "to": 0},
                "datasourceUid": datasource,
                "model": {
                    "refId": "A", "expr": expression, "queryType": "instant", "instant": True,
                    "range": False, "editorMode": "code", "intervalMs": 1000, "maxDataPoints": 43200,
                    "datasource": {"uid": datasource, "type": datasource},
                },
            },
            {
                "refId": "B",
                "relativeTimeRange": {"from": 0, "to": 0},
                "datasourceUid": "__expr__",
                "model": {
                    "refId": "B", "type": "threshold", "expression": "A",
                    "conditions": [{
                        "evaluator": {"type": "gt", "params": [threshold]},
                        "operator": {"type": "and"}, "query": {"params": ["B"]},
                        "reducer": {"type": "last", "params": []}, "type": "query",
                    }],
                },
            },
        ],
        "noDataState": "NoData", "execErrState": "Error", "for": pending,
        "annotations": {"summary": title, "description": "Grafana Explore에서 해당 환경과 시간대의 근거를 확인하세요"},
        "labels": {"env": environment, "service": "chaekchaek", "analysis_profile": profile},
        "isPaused": paused,
        "notification_settings": {
            "receiver": receiver, "group_by": ["alertname", "env"],
            "group_wait": "10s", "group_interval": "5m", "repeat_interval": "4h",
        },
    }


def render(environment, with_questions, paused):
    base = contact_point(environment, False)
    contacts = [base]
    destination = base["name"]
    if with_questions:
        enriched = contact_point(environment, True)
        contacts.append(enriched)
        destination = enriched["name"]
    app = f'{{env="{environment}",job="chaekchaek-backend"}}'
    host = f'{{env="{environment}",job="integrations/unix"}}'
    errors = (
        f'sum(count_over_time({{app="chaekchaek",env="{environment}"}} '
        '| json kind="type", status="status" | kind="access" | status >= 500 '
        '| status < 600 | __error__="" [5m])) or vector(0)'
    )
    definitions = [
        ("backend-5xx", "Backend5xx", errors, "loki", 4, "1m", "api-errors", destination),
        ("backend-unavailable", "BackendScrapeUnavailable", f'(1 - min(up{app})) or vector(1)',
         "prometheus", 0.5, "2m", "availability", destination),
        ("backend-stale", "BackendMetricsStale", f'(time() - max(timestamp(up{app}))) or vector(1000000000)',
         "prometheus", 90, "1m", "availability", destination),
        ("host-memory", "HostMemoryPressure", f'1 - min(node_memory_MemAvailable_bytes{host} / node_memory_MemTotal_bytes{host})',
         "prometheus", 0.9, "3m", "availability", destination),
    ]
    if with_questions:
        definitions.extend([
            ("bot-disconnected", "IncidentBotDisconnected",
             '(max(incident_slack_enabled) or vector(0)) * (1 - (min(incident_slack_connected) or vector(0)))',
             "prometheus", 0.5, "2m", "disabled", base["name"]),
            ("analyzer-down", "IncidentAnalyzerUnavailable", '(1 - min(up{job="incident-analyzer"})) or vector(1)',
             "prometheus", 0.5, "2m", "disabled", base["name"]),
            ("analyzer-delivery", "IncidentAnalyzerDeliveryFailure", 'sum(increase(incident_delivery_total{outcome="failed"}[5m])) or vector(0)',
             "prometheus", 0, "0s", "disabled", base["name"]),
            ("analyzer-model", "IncidentAnalyzerModelFailure", 'sum(increase(incident_analysis_total{outcome="failed"}[5m])) or vector(0)',
             "prometheus", 0, "0s", "disabled", base["name"]),
        ])
    rules = [rule(environment, *definition, paused) for definition in definitions]
    return {
        "apiVersion": 1,
        "contactPoints": contacts,
        "groups": [{"orgId": 1, "name": f"chaekchaek-{environment}", "folder": "ChaekChaek incidents", "interval": "30s", "rules": rules}],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env", choices=["dev", "prod"], required=True)
    parser.add_argument("--with-questions", action="store_true")
    parser.add_argument("--paused", action="store_true")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    config = render(args.env, args.with_questions, args.paused)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n")
    print(f"Wrote {args.output}: {len(config['groups'][0]['rules'])} rules, paused={args.paused}")


if __name__ == "__main__":
    main()
