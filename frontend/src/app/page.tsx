"use client";
import { useState, type FormEvent } from "react";
import { ReflectionCard } from "../components/reflection-card";
import { toggleLike, type Reflection } from "../lib/experiment";
import { initialExperiment } from "../lib/examples";
import { useExperiment } from "../lib/use-experiment";
const sampleReflections = initialExperiment().reflections;

export default function ExperimentPage() {
  const { experiment, participant, ready, error, changeExperiment } = useExperiment();
  const [selectedId, setSelectedId] = useState("odyssey");
  const [sort, setSort] = useState("newest");
  const [notice, setNotice] = useState("");
  const [validation, setValidation] = useState("");
  const book = experiment.books.find((item) => item.id === selectedId) ?? experiment.books[0];
  const likesFor = (id: string) => experiment.likes.filter((like) => like.reflectionId === id).length;
  const notes = experiment.reflections.map((note) => note.isExample ? sampleReflections.find((sample) => sample.id === note.id) ?? note : note).filter((note) => note.bookId === book?.id).sort((a, b) =>
    sort === "popular" ? likesFor(b.id) - likesFor(a.id) || b.createdAt.localeCompare(a.createdAt) : b.createdAt.localeCompare(a.createdAt));
  function submitReflection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!participant || !book) return;
    const form = event.currentTarget;
    const fields = new FormData(form);
    const body = String(fields.get("body") ?? "").trim();
    if (!body) { setValidation("감상을 입력해 주세요."); return; }
    const note: Reflection = { id: crypto.randomUUID(), bookId: book.id, userId: participant.id,
      nickname: participant.nickname, title: "", quote: "", source: "",
      body, createdAt: new Date().toISOString(), isExample: false };
    if (changeExperiment((current) => ({ ...current, reflections: [note, ...current.reflections] }))) {
      form.reset(); setSort("newest"); setValidation(""); setNotice("감상을 남겼어요.");
    }
  }
  return <main className="experiment-shell">
    <section className="books-section" aria-labelledby="books-title">
      <div className="section-heading"><h1 id="books-title">함께 읽을 책</h1><span className="muted">등록된 책 {experiment.books.length}권</span></div>
      <div className="book-shelf" aria-label="책 선택">
        {experiment.books.map((item) => <button key={item.id} type="button" className={"book-choice " + (book?.id === item.id ? "selected" : "")}
          aria-pressed={book?.id === item.id} onClick={() => { setSelectedId(item.id); setNotice(""); }} aria-label={item.title + " 선택"}>
          <span className="book-genre">{item.genre}</span><strong className="book-title">{item.title}</strong>
        </button>)}
      </div>
    </section>
    {error && <p role="alert" className="error-banner">{error}</p>}
    {book && <div className="reading-layout">
      <section className="reflection-feed" aria-labelledby="feed-title">
        <div className="feed-heading"><h2 id="feed-title">함께 나눈 감상 <span>{notes.length}</span></h2>
          <label className="sort-label"><span className="sr-only">감상 정렬</span><select value={sort} onChange={(event) => setSort(event.target.value)}><option value="newest">최신순</option><option value="popular">좋아요순</option></select></label>
        </div>
        <form key={book.id} onSubmit={submitReflection} className="inline-composer">
          <label className="sr-only" htmlFor="reflection-body">감상</label>
          <textarea id="reflection-body" name="body" required maxLength={3000} rows={2} placeholder="이 책, 어떻게 읽었나요? 짧게 남겨도 좋아요."/>
          <button className="primary" disabled={!ready}>남기기</button>
          {validation && <p role="alert" className="field-error">{validation}</p>}
        </form>
        <p role="status" className="save-notice">{notice}</p>
        {notes.length === 0 && <p className="empty-state">아직 나눈 감상이 없어요.</p>}
        {notes.map((note) => <ReflectionCard key={note.id} note={note}
          replies={experiment.replies.filter((reply) => reply.reflectionId === note.id)}
          likeCount={likesFor(note.id)} liked={experiment.likes.some((like) => like.reflectionId === note.id && like.userId === participant?.id)}
          ready={ready}
          onLike={() => { if (participant) changeExperiment((current) => toggleLike(current, note.id, participant.id)); }}
          onReply={(body) => {
            if (!participant) return false;
            return changeExperiment((current) => ({ ...current, replies: [...current.replies, { id: crypto.randomUUID(), reflectionId: note.id,
              userId: participant.id, nickname: participant.nickname, body, createdAt: new Date().toISOString() }] }));
          }}/>)}
      </section>
    </div>}
    <p className="preview-note">미리보기 · 입력은 이 브라우저에만 저장됩니다.</p>
  </main>;
}
