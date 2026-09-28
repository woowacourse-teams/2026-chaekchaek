"use client";
import { useState } from "react";
import type { TrafficBucket } from "../lib/reading-traffic";

type Period = "hourly" | "daily";

function TrafficPlot({ buckets, field, label }: { buckets: TrafficBucket[]; field: "arrivals" | "cumulative"; label: string }) {
  const width = 760;
  const top = 18;
  const bottom = 142;
  const left = 32;
  const right = width - 20;
  const step = (right - left) / buckets.length;
  const maximum = buckets.reduce((highest, bucket) => Math.max(highest, bucket[field]), 1);
  const height = bottom - top;
  const points = buckets.map((bucket, index) => ({
    x: left + step * (index + 0.5),
    y: bottom - (bucket[field] / maximum) * height,
    bucket,
  }));
  const ticks = [...new Set([0, Math.floor((buckets.length - 1) / 4), Math.floor((buckets.length - 1) / 2),
    Math.floor((buckets.length - 1) * 3 / 4), buckets.length - 1])];
  return <div className="traffic-plot">
    <h3>{label}</h3>
    <div className="traffic-plot-scroll"><svg viewBox={`0 0 ${width} 180`} role="img" aria-label={`${label}: 최대 ${maximum}명, 마지막 구간 ${buckets.at(-1)?.[field] ?? 0}명`}>
      <line className="traffic-axis" x1={left} y1={bottom} x2={right} y2={bottom}/>
      <text className="traffic-max-label" x={left} y={top - 4}>{maximum}명</text>
      {field === "arrivals" ? points.map(({ x, y, bucket }) => <rect key={bucket.key} className="traffic-bar"
        x={x - Math.max(2, step - 2) / 2} y={y} width={Math.max(2, step - 2)} height={Math.max(0, bottom - y)}>
        <title>{bucket.label}: {bucket.arrivals}명 유입</title>
      </rect>) : <>
        <path className="traffic-line" d={points.map(({ x, y }, index) => `${index ? "L" : "M"}${x},${y}`).join(" ")}/>
        {points.filter((_, index) => index === 0 || index === points.length - 1 || buckets[index].arrivals > 0)
          .map(({ x, y, bucket }) => <circle key={bucket.key} className="traffic-dot" cx={x} cy={y} r="3">
            <title>{bucket.label}: 누적 {bucket.cumulative}명</title>
          </circle>)}
      </>}
      {ticks.map((index) => <text key={index} className="traffic-tick" x={points[index].x} y="165"
        textAnchor={index === 0 ? "start" : index === buckets.length - 1 ? "end" : "middle"}>{buckets[index].label}</text>)}
    </svg></div>
  </div>;
}

export function ReadingTrafficChart({ hourly, daily, visitors, timedVisitors, asOf }: {
  hourly: TrafficBucket[]; daily: TrafficBucket[]; visitors: number; timedVisitors: number; asOf: number;
}) {
  const [period, setPeriod] = useState<Period>("hourly");
  const buckets = period === "hourly" ? hourly : daily;
  const peak = buckets.reduce<TrafficBucket | null>((best, bucket) => bucket.arrivals > (best?.arrivals ?? 0) ? bucket : best, null);
  return <section className="metrics-section traffic-section" aria-labelledby="traffic-title">
    <div className="traffic-heading"><div><h2 id="traffic-title">방문 흐름</h2><p className="muted">책을 선택한 사람의 첫 기록 시각을 한국 시간으로 표시합니다.<br/>조회 기준: {new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", dateStyle: "short", timeStyle: "short" }).format(asOf)}</p></div>
      <div className="traffic-period" role="group" aria-label="집계 간격">
        <button type="button" className={period === "hourly" ? "primary" : "secondary"} aria-pressed={period === "hourly"} onClick={() => setPeriod("hourly")}>최근 72시간</button>
        <button type="button" className={period === "daily" ? "primary" : "secondary"} aria-pressed={period === "daily"} onClick={() => setPeriod("daily")}>전체 일별</button>
      </div>
    </div>
    <div className="traffic-highlights"><div><span>현재까지 누적 방문자</span><strong>{visitors}명</strong></div>
      <div><span>가장 많이 들어온 {period === "hourly" ? "시간" : "날짜"}</span><strong>{peak ? `${peak.label} · ${peak.arrivals}명` : "기록 없음"}</strong></div></div>
    {timedVisitors > 0 ? <div className="traffic-plots">
      <TrafficPlot buckets={buckets} field="arrivals" label="구간별 새 방문자"/>
      <TrafficPlot buckets={buckets} field="cumulative" label="누적 방문자"/>
    </div> : <p className="muted">시각이 기록된 방문자가 아직 없습니다.</p>}
    {timedVisitors !== visitors && <p className="muted">시각이 없는 기존 기록 {visitors - timedVisitors}명은 누적 방문자 수에 포함하고 시간 그래프에서는 제외했습니다.</p>}
    <details className="traffic-details"><summary>구간별 정확한 수치 보기</summary><div className="table-scroll"><table>
      <thead><tr><th scope="col">한국 시간</th><th scope="col">새 방문자</th><th scope="col">누적 방문자</th></tr></thead>
      <tbody>{buckets.map((bucket) => <tr key={bucket.key}><td>{bucket.label}</td><td>{bucket.arrivals}</td><td>{bucket.cumulative}</td></tr>)}</tbody>
    </table></div></details>
    <p className="muted">이전 수집 방식의 일부 기록은 책을 선택하기 전 첫 방문 시각일 수 있습니다. 그래프는 현재 저장된 시각을 그대로 사용합니다.</p>
  </section>;
}
