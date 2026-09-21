"use client";
import { useState, type FormEvent } from "react";
import { Dialog } from "../../components/dialog";
import { GENRES, metricsCsv, summarizeExperiment, type Genre } from "../../lib/experiment";
import { useExperiment } from "../../lib/use-experiment";
export default function AdminPreview() {
  const { experiment, ready, error, cloud, changeExperiment } = useExperiment();
  const [addingBook, setAddingBook] = useState(false);
  const [validation, setValidation] = useState("");
  const summary = summarizeExperiment(experiment);
  function downloadCsv() {
    const url = URL.createObjectURL(new Blob([metricsCsv(experiment)], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url; link.download = "chaekchaek-preview-metrics.csv"; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  async function registerBook(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    const title = String(fields.get("title") ?? "").trim();
    const author = String(fields.get("author") ?? "").trim();
    const genre = String(fields.get("genre") ?? "") as Genre;
    if (!title || !author || !GENRES.includes(genre)) { setValidation("책 제목과 저자, 장르를 확인해 주세요."); return; }
    const book = { id: crypto.randomUUID(), title, author, genre };
    if (await changeExperiment((current) => ({ ...current, books: [...current.books, book] }), { type: "book", book })) {
      setAddingBook(false); setValidation("");
    }
  }
  return <main className="experiment-shell admin-shell">
    <h1>실험 집계 미리보기</h1>
    <p className="admin-intro">{cloud ? "모든 참여자의 감상, 답글과 좋아요를 합산한 실험 데이터입니다." : "이 브라우저에 저장된 동작 확인용 데이터입니다. 다른 참여자의 데이터는 합산되지 않습니다."}</p>
    <div className="admin-toolbar"><a href="/">참여 화면으로 돌아가기</a><button className="secondary" disabled={!ready} onClick={downloadCsv}>CSV 내려받기</button>{!cloud && <button className="primary" disabled={!ready} onClick={() => setAddingBook(true)}>책 등록하기</button>}</div>
    {error && <p className="error-banner" role="alert">{error}</p>}
    <div className="metrics-summary">
      <div><span>총 참여</span><strong data-testid="total-participation">{summary.total.total}</strong></div>
      <div><span>참여자</span><strong>{summary.total.participants}</strong></div>
      <div><span>소설 참여 비중</span><strong>{summary.novelShare === null ? "집계 전" : (summary.novelShare * 100).toFixed(1) + "%"}</strong></div>
    </div>
    <p className="muted">예시 감상 5개는 참여 수에서 제외합니다. 예시에 남긴 답글과 좋아요는 포함하며, 취소한 좋아요는 제외합니다. 참여자는 닉네임이 아닌 브라우저 식별자로 중복 제거합니다.</p>
    <section className="metrics-section"><h2>장르별 참여</h2><div className="table-scroll"><table>
      <thead><tr><th scope="col">장르</th><th scope="col">감상</th><th scope="col">답글</th><th scope="col">좋아요</th><th scope="col">총 참여</th><th scope="col">참여자</th><th scope="col">비중</th></tr></thead>
      <tbody>{summary.genres.map((row) => <tr key={row.genre}><td>{row.genre}</td><td>{row.reflections}</td><td>{row.replies}</td><td>{row.likes}</td><td>{row.total}</td><td>{row.participants}</td><td>{summary.total.total ? (row.total / summary.total.total * 100).toFixed(1) + "%" : "-"}</td></tr>)}</tbody>
    </table></div></section>
    <section className="metrics-section"><h2>책별 참여</h2><div className="table-scroll"><table>
      <thead><tr><th scope="col">책</th><th scope="col">장르</th><th scope="col">감상</th><th scope="col">답글</th><th scope="col">좋아요</th><th scope="col">참여자</th></tr></thead>
      <tbody>{summary.books.map((row) => <tr key={row.book.id}><td>{row.book.title}</td><td>{row.book.genre}</td><td>{row.reflections}</td><td>{row.replies}</td><td>{row.likes}</td><td>{row.participants}</td></tr>)}</tbody>
    </table></div></section>
    <p className="muted">소설 참여 비중 = (고전소설 참여 + 현대소설 참여) ÷ 전체 참여. 현재는 책 목록과 실험 조건이 확정되지 않았으므로 가설의 검증·기각을 판정하지 않습니다.</p>
    {addingBook && <Dialog title="책 등록하기" onClose={() => setAddingBook(false)}><form onSubmit={registerBook}>
      <label>책 제목<input name="title" required maxLength={120}/></label>
      <label>저자<input name="author" required maxLength={80}/></label>
      <label>장르<select className="genre-select" name="genre">{GENRES.map((genre) => <option key={genre}>{genre}</option>)}</select></label>
      <p className="muted">제목으로 만든 임시 표지를 사용합니다. 등록한 책은 참여 화면에서 확인할 수 있습니다.</p>
      {validation && <p className="field-error" role="alert">{validation}</p>}
      {error && <p className="error-banner" role="alert">{error}</p>}
      <div className="form-actions"><button type="button" className="secondary" onClick={() => setAddingBook(false)}>취소</button><button className="primary" disabled={!ready}>등록하기</button></div>
    </form></Dialog>}
  </main>;
}
