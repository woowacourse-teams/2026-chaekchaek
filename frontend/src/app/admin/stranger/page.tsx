"use client";
import { strangerMetricsCsv, summarizeStrangerExperiment, summarizeStrangerPlatforms } from "../../../lib/stranger-experiment";
import { deviceLabel, sourceLabel } from "../../../lib/stranger-platform";
import { useStrangerExperiment } from "../../../lib/use-stranger-experiment";

export default function StrangerAdminPage() {
  const { data, ready, cloud, error } = useStrangerExperiment();
  const summary = summarizeStrangerExperiment(data);
  const platforms = summarizeStrangerPlatforms(data);
  function downloadCsv() {
    const url = URL.createObjectURL(new Blob([strangerMetricsCsv(data)], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url; link.download = "chaekchaek-stranger-metrics.csv"; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  return <main className="experiment-shell admin-shell">
    <h1>실험 집계 미리보기</h1>
    <nav className="admin-tabs" aria-label="실험 통계"><a href="/admin">기존 실험</a><a href="/admin/stranger" aria-current="page">이방인 실험</a></nav>
    <p className="admin-intro">{cloud ? "『이방인』 실험에 참여한 사람들의 결과입니다." : "이 브라우저에 저장된 동작 확인용 데이터입니다. 다른 참여자의 데이터는 합산되지 않습니다."}</p>
    <div className="admin-toolbar"><a href="/experiments/stranger">참여 화면으로 돌아가기</a><button className="secondary" disabled={!ready} onClick={downloadCsv}>CSV 내려받기</button></div>
    {error && <p className="error-banner" role="alert">{error}</p>}
    <div className="metrics-summary"><div><span>미선택 방문자</span><strong>{summary.unselected}</strong></div>
      {summary.groups.map((group) => <div key={group.status}><span>{group.status === "read" ? "읽었어요" : "안 읽었어요"}</span><strong>{group.participants}</strong></div>)}
    </div>
    <section className="metrics-section"><h2>읽음 여부별 참여</h2><div className="table-scroll"><table>
      <thead><tr><th scope="col">읽음 여부</th><th scope="col">선택자</th>{[1,2,3,4,5,6].map((page) => <th scope="col" key={page}>{page}쪽 열람</th>)}
        <th scope="col">6쪽 모두 열람</th><th scope="col">작성 시작</th><th scope="col">감상 제출자</th><th scope="col">제출률</th><th scope="col">답글</th><th scope="col">좋아요</th></tr></thead>
      <tbody>{summary.groups.map((group) => <tr key={group.status}><td>{group.status === "read" ? "읽었어요" : "안 읽었어요"}</td><td>{group.participants}</td>
        {group.pageViews.map((count, index) => <td key={index}>{count}</td>)}<td>{group.completedPages}</td><td>{group.composerStarted}</td><td>{group.submitted}</td>
        <td>{group.submissionRate === null ? "집계 전" : (group.submissionRate * 100).toFixed(1) + "%"}</td><td>{group.replies}</td><td>{group.likes}</td></tr>)}</tbody>
    </table></div></section>
    <section className="metrics-section"><h2>유입 경로별 참여</h2><div className="table-scroll"><table>
      <thead><tr><th scope="col">유입 경로</th><th scope="col">방문자</th><th scope="col">읽었어요</th><th scope="col">안 읽었어요</th><th scope="col">감상 제출자</th></tr></thead>
      <tbody>{platforms.sources.map((row) => <tr key={row.value}><td>{sourceLabel(row.value)}</td><td>{row.visitors}</td><td>{row.read}</td><td>{row.unread}</td><td>{row.submitted}</td></tr>)}</tbody>
    </table></div></section>
    <section className="metrics-section"><h2>접속 기기별 참여</h2><div className="table-scroll"><table>
      <thead><tr><th scope="col">접속 기기</th><th scope="col">방문자</th><th scope="col">읽었어요</th><th scope="col">안 읽었어요</th><th scope="col">감상 제출자</th></tr></thead>
      <tbody>{platforms.devices.map((row) => <tr key={row.value}><td>{deviceLabel(row.value)}</td><td>{row.visitors}</td><td>{row.read}</td><td>{row.unread}</td><td>{row.submitted}</td></tr>)}</tbody>
    </table></div></section>
    <p className="muted">열람은 페이지를 표시한 기록이며 독해 완료를 뜻하지 않습니다. 제출률은 해당 읽음 여부 선택자 중 감상을 제출한 사람의 비율입니다.</p>
    <p className="muted">유입 경로는 첫 방문의 utm_source 또는 확인 가능한 이전 사이트 도메인입니다. 기기 종류도 첫 방문 기준입니다. 출처를 확인할 수 없으면 직접/확인 불가로 표시합니다.</p>
  </main>;
}
