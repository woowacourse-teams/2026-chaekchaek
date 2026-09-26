"use client";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { ReflectionCard } from "./reflection-card";
import { initialStrangerReflections } from "../lib/stranger-initial-reflections";
import { initialLoveReflections } from "../lib/love-initial-reflections";
import { newReadingId, readingBooks, type ReadingBookId } from "../lib/reading-book-config";
import { useReadingExperiment } from "../lib/use-stranger-experiment";
import type { ReadingStatus, StrangerReflection } from "../lib/stranger-experiment";

const scanLoads = new Map<string, Promise<void>>();
const scanPath = (book: ReadingBookId, page: number) => `/experiments/${book}/page-${page}.jpg`;

function prepareScan(book: ReadingBookId, page: number): Promise<void> {
  const path = scanPath(book, page);
  const cached = scanLoads.get(path);
  if (cached) return cached;
  const image = new Image();
  image.src = path;
  const ready = image.decode().catch((error) => {
    scanLoads.delete(path);
    throw error;
  });
  scanLoads.set(path, ready);
  return ready;
}

type ExperimentState = ReturnType<typeof useReadingExperiment>;

export function ReadingExperimentPage({ initialBook = null }: { initialBook?: ReadingBookId | null }) {
  const stranger = useReadingExperiment("stranger", true);
  const love = useReadingExperiment("love-fragments", true);
  const [selectedBook, setSelectedBook] = useState<ReadingBookId | null>(initialBook);

  useEffect(() => { setSelectedBook(initialBook); }, [initialBook]);
  useEffect(() => {
    const restoreSelection = () => {
      const book = new URLSearchParams(window.location.search).get("book");
      setSelectedBook(book === "stranger" || book === "love-fragments" ? book : null);
    };
    window.addEventListener("popstate", restoreSelection);
    return () => window.removeEventListener("popstate", restoreSelection);
  }, []);

  async function chooseBook(book: ReadingBookId, status: ReadingStatus) {
    const experiment = book === "stranger" ? stranger : love;
    if (!experiment.identity) return;
    if (!await experiment.commit({ type: "reading", userId: experiment.identity.id, readingStatus: status })) return;
    setSelectedBook(book);
    const url = new URL(window.location.href);
    url.searchParams.set("book", book);
    window.history.replaceState(window.history.state, "", url);
  }

  const books = (["stranger", "love-fragments"] as const).map((book) => {
    const config = readingBooks[book];
    const experiment = book === "stranger" ? stranger : love;
    const person = experiment.data.participants.find((item) => item.id === experiment.identity?.id);
    const readingStatus = person?.readingStatus ?? null;
    return <section key={book} className={`reading-book-choice${selectedBook === book ? " selected" : ""}`} data-book={book}
      aria-label={`${config.title} 읽음 여부 선택`}>
      <img src={config.coverUrl} alt={`${config.title} 책 표지`} width={110} height={160}/>
      <div className="reading-book-copy"><h2>{config.title}</h2><p className="muted">읽어보셨나요?</p>
        <div className="stranger-choices">
          <button type="button" className={readingStatus === "read" ? "primary" : "secondary"} aria-pressed={readingStatus === "read"}
            disabled={!experiment.ready} onClick={() => void chooseBook(book, "read")}>읽었어요</button>
          <button type="button" className={readingStatus === "unread" ? "primary" : "secondary"} aria-pressed={readingStatus === "unread"}
            disabled={!experiment.ready} onClick={() => void chooseBook(book, "unread")}>안 읽었어요</button>
        </div>
        {experiment.error && <p role="alert" className="field-error">{experiment.error}</p>}
      </div>
    </section>;
  });
  const selected = selectedBook === "stranger" ? stranger : selectedBook === "love-fragments" ? love : null;
  const selectedStatus = selected?.data.participants.find((item) => item.id === selected.identity?.id)?.readingStatus;
  return <main className="experiment-shell stranger-shell">
    <div className="reading-book-grid" aria-label="실험할 책 선택">{books}</div>
    {selectedBook && selected && selectedStatus && <ReadingExperimentDetails key={selectedBook} book={selectedBook} experiment={selected}/>}
    {selected && <p className="preview-note">{selected.cloud ? "입력한 감상과 반응은 실험 데이터로 저장됩니다." : "미리보기 · 입력은 이 브라우저에만 저장됩니다."}</p>}
  </main>;
}

function ReadingExperimentDetails({ book, experiment }: { book: ReadingBookId; experiment: ExperimentState }) {
  const config = readingBooks[book];
  const initialReflections = book === "stranger" ? initialStrangerReflections : initialLoveReflections;
  const initialIds = new Set(initialReflections.map((item) => item.id));
  const { data, identity, ready, commit } = experiment;
  const [page, setPage] = useState(1);
  const [pageLoading, setPageLoading] = useState(false);
  const [pageError, setPageError] = useState("");
  const [notice, setNotice] = useState("");
  const [validation, setValidation] = useState("");
  const recordedPages = useRef(new Set<number>());
  const pendingReflectionId = useRef<string | null>(null);
  const pageTransitioning = useRef(false);
  const person = data.participants.find((item) => item.id === identity?.id);
  const readingStatus = person?.readingStatus ?? null;
  const reflections = [...initialReflections,
    ...data.reflections.filter((item) => !initialIds.has(item.id)).sort((a, b) => b.createdAt.localeCompare(a.createdAt))];

  useEffect(() => {
    if (!ready || !identity || !readingStatus || recordedPages.current.has(page)) return;
    recordedPages.current.add(page);
    void commit({ type: "page", userId: identity.id, page }).then((saved) => {
      if (!saved) recordedPages.current.delete(page);
    });
  }, [ready, identity, readingStatus, page, commit]);
  useEffect(() => {
    if (!readingStatus) return;
    for (const neighbor of [page - 1, page + 1]) {
      if (neighbor >= 1 && neighbor <= config.printedPages.length) void prepareScan(book, neighbor).catch(() => {});
    }
  }, [book, config.printedPages.length, page, readingStatus]);
  async function changePage(nextPage: number) {
    if (pageTransitioning.current || nextPage < 1 || nextPage > config.printedPages.length) return;
    pageTransitioning.current = true;
    setPageLoading(true);
    setPageError("");
    try {
      await prepareScan(book, nextPage);
      setPage(nextPage);
    } catch {
      setPageError("페이지 이미지를 불러오지 못했어요. 다시 눌러 주세요.");
    } finally {
      pageTransitioning.current = false;
      setPageLoading(false);
    }
  }
  function startComposer() {
    if (identity && !person?.composerStarted) void commit({ type: "composer", userId: identity.id });
  }
  async function submitReflection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!identity) return;
    const form = event.currentTarget;
    const body = String(new FormData(form).get("body") ?? "").trim();
    if (!body) { setValidation("감상을 입력해 주세요."); return; }
    const reflection: StrangerReflection = { id: pendingReflectionId.current ?? newReadingId(book), userId: identity.id,
      nickname: identity.nickname, body, createdAt: new Date().toISOString() };
    pendingReflectionId.current = reflection.id;
    if (await commit({ type: "reflection", reflection })) {
      pendingReflectionId.current = null;
      form.reset(); setValidation(""); setNotice("감상을 남겼어요.");
    }
  }
  return <>
      {config.context && <section className="stranger-context" aria-labelledby="stranger-context-title">
        <h2 id="stranger-context-title">앞선 줄거리</h2>
        <p>{config.context}</p>
      </section>}
      <section className="stranger-reading" aria-labelledby="stranger-reading-title">
        <h2 id="stranger-reading-title">{config.excerptTitle}</h2>
        <p className="muted">{config.excerptDescription}</p>
        <div className="stranger-reader">
          <div className="stranger-scan-stage">
            <button className="secondary stranger-side-nav" type="button" aria-label="이전" disabled={pageLoading || page === 1} onClick={() => void changePage(page - 1)}><span aria-hidden="true">‹</span></button>
            <div className="stranger-scan-frame">
              <img className={config.alignFacingPages ? `stranger-scan stranger-scan-${page % 2 ? "odd" : "even"}` : "stranger-scan"}
                src={scanPath(book, page)} alt={`${config.title} 발췌문 ${page} / ${config.printedPages.length}, 책 ${config.printedPages[page - 1]}쪽`}
                width={config.scanWidth} height={config.scanHeight}/>
            </div>
            <button className="secondary stranger-side-nav" type="button" aria-label="다음" disabled={pageLoading || page === config.printedPages.length} onClick={() => void changePage(page + 1)}><span aria-hidden="true">›</span></button>
          </div>
          <div className="stranger-page-position">
            <span aria-live="polite">{pageLoading ? "불러오는 중…" : `${page} / ${config.printedPages.length}`}</span>
            {book === "love-fragments" && <span className="stranger-printed-page">책 {config.printedPages[page - 1]}쪽</span>}
          </div>
          {pageError && <p role="alert" className="field-error">{pageError}</p>}
        </div>
      </section>
      <section className="reflection-feed stranger-feed" aria-labelledby="stranger-feed-title">
        <div className="feed-heading"><h2 id="stranger-feed-title">함께 나눈 감상 <span>{reflections.length}</span></h2></div>
        <form onSubmit={submitReflection} className="inline-composer">
          <label className="sr-only" htmlFor={`${book}-reflection-body`}>감상</label>
          <textarea id={`${book}-reflection-body`} name="body" required maxLength={3000} rows={2}
            onFocus={startComposer} onInput={startComposer} placeholder="이 책, 어떻게 읽었나요? 짧게 남겨도 좋아요."/>
          <button className="primary" disabled={!ready}>남기기</button>
          {validation && <p role="alert" className="field-error">{validation}</p>}
        </form>
        <p className="save-notice" role="status">{notice}</p>
        {reflections.map((item) => <ReflectionCard key={item.id}
          note={{ ...item, bookId: book, title: "", quote: "", source: "", isExample: false }}
          replies={data.replies.filter((reply) => reply.reflectionId === item.id)}
          likeCount={data.likes.filter((like) => like.reflectionId === item.id).length}
          liked={data.likes.some((like) => like.reflectionId === item.id && like.userId === identity?.id)}
          ready={ready}
          onLike={async () => { if (identity) await commit({ type: "like", reflectionId: item.id, userId: identity.id }); }}
          onReply={async (body) => identity ? commit({ type: "reply", reply: { id: newReadingId(book), reflectionId: item.id,
            userId: identity.id, nickname: identity.nickname, body, createdAt: new Date().toISOString() } }) : false}/>) }
      </section>
  </>;
}
