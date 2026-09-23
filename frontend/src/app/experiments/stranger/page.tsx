"use client";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { ReflectionCard } from "../../../components/reflection-card";
import { useStrangerExperiment } from "../../../lib/use-stranger-experiment";
import type { ReadingStatus, StrangerReflection } from "../../../lib/stranger-experiment";

const COVER_URL = "https://image.yes24.com/goods/192097306/XL";
const scanPath = (page: number) => `/experiments/stranger/page-${page}.jpg`;
const scanLoads = new Map<number, Promise<void>>();

function prepareScan(page: number): Promise<void> {
  const cached = scanLoads.get(page);
  if (cached) return cached;
  const image = new Image();
  image.src = scanPath(page);
  const ready = image.decode().catch((error) => {
    scanLoads.delete(page);
    throw error;
  });
  scanLoads.set(page, ready);
  return ready;
}

export default function StrangerExperimentPage() {
  const { data, identity, ready, cloud, error, commit } = useStrangerExperiment(true);
  const [page, setPage] = useState(1);
  const [pageLoading, setPageLoading] = useState(false);
  const [pageError, setPageError] = useState("");
  const [zoom, setZoom] = useState(false);
  const [notice, setNotice] = useState("");
  const [validation, setValidation] = useState("");
  const recordedPages = useRef(new Set<number>());
  const pendingReflectionId = useRef<string | null>(null);
  const pageTransitioning = useRef(false);
  const person = data.participants.find((item) => item.id === identity?.id);
  const readingStatus = person?.readingStatus ?? null;
  const reflections = [...data.reflections].sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  useEffect(() => {
    if (!ready || !identity || !readingStatus || recordedPages.current.has(page)) return;
    recordedPages.current.add(page);
    void commit({ type: "page", userId: identity.id, page }).then((saved) => {
      if (!saved) recordedPages.current.delete(page);
    });
  }, [ready, identity, readingStatus, page, commit]);
  useEffect(() => {
    if (!zoom) return;
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") setZoom(false); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [zoom]);
  useEffect(() => {
    if (!readingStatus) return;
    for (const neighbor of [page - 1, page + 1]) {
      if (neighbor >= 1 && neighbor <= 6) void prepareScan(neighbor).catch(() => {});
    }
  }, [page, readingStatus]);
  async function changePage(nextPage: number) {
    if (pageTransitioning.current || nextPage < 1 || nextPage > 6) return;
    pageTransitioning.current = true;
    setPageLoading(true);
    setPageError("");
    try {
      await prepareScan(nextPage);
      setPage(nextPage);
    } catch {
      setPageError("페이지 이미지를 불러오지 못했어요. 다시 눌러 주세요.");
    } finally {
      pageTransitioning.current = false;
      setPageLoading(false);
    }
  }
  async function chooseReading(status: ReadingStatus) {
    if (!identity) return;
    if (await commit({ type: "reading", userId: identity.id, readingStatus: status })) setNotice("");
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
    const reflection: StrangerReflection = { id: pendingReflectionId.current ?? crypto.randomUUID(), userId: identity.id,
      nickname: identity.nickname, body, createdAt: new Date().toISOString() };
    pendingReflectionId.current = reflection.id;
    if (await commit({ type: "reflection", reflection })) {
      pendingReflectionId.current = null;
      form.reset(); setValidation(""); setNotice("감상을 남겼어요.");
    }
  }
  return <main className="experiment-shell stranger-shell">
    <section className="stranger-book" aria-label="읽음 여부 선택">
      <img src={COVER_URL} alt="이방인 책 표지" width={110} height={160}/>
      <div><h1>이방인</h1><p className="muted">읽어보셨나요?</p>
        <div className="stranger-choices">
          <button type="button" className={readingStatus === "read" ? "primary" : "secondary"} aria-pressed={readingStatus === "read"} disabled={!ready} onClick={() => chooseReading("read")}>읽었어요</button>
          <button type="button" className={readingStatus === "unread" ? "primary" : "secondary"} aria-pressed={readingStatus === "unread"} disabled={!ready} onClick={() => chooseReading("unread")}>안 읽었어요</button>
        </div>
      </div>
    </section>
    {error && <p role="alert" className="error-banner">{error}</p>}
    {readingStatus && <>
      <section className="stranger-context" aria-labelledby="stranger-context-title">
        <h2 id="stranger-context-title">앞선 줄거리</h2>
        <p>뫼르소는 범죄 사건에 연루되어, 피의자 신분으로 법정 재판에 참석하게 된다. 재판이 시작되었지만, 뫼르소의 ‘범죄 행위’ 그 자체보다는 지난 어머니 장례식에서의 태도와 평소 행실을 끌어와 그를 비난하기 시작한다.</p>
      </section>
      <section className="stranger-reading" aria-labelledby="stranger-reading-title">
        <h2 id="stranger-reading-title">발췌문 99-104쪽</h2>
        <p className="muted">책에 적힌 필기와 밑줄도 함께 볼 수 있어요.</p>
        <div className="stranger-reader">
          <div className="stranger-scan-stage">
            <button className="stranger-scan-button" type="button" onClick={() => setZoom(true)} aria-label={`${page}번째 발췌문 크게 보기`}>
              <img className={`stranger-scan stranger-scan-${page % 2 ? "odd" : "even"}`} src={scanPath(page)} alt={`이방인 발췌문 ${page} / 6, 책 ${page + 98}쪽`} width={960} height={1440}/>
            </button>
            <button className="secondary stranger-side-nav stranger-side-nav-prev" type="button" aria-label="이전" disabled={pageLoading || page === 1} onClick={() => void changePage(page - 1)}><span aria-hidden="true">‹</span></button>
            <button className="secondary stranger-side-nav stranger-side-nav-next" type="button" aria-label="다음" disabled={pageLoading || page === 6} onClick={() => void changePage(page + 1)}><span aria-hidden="true">›</span></button>
          </div>
          <div className="stranger-page-position">
            <span aria-live="polite">{pageLoading ? "불러오는 중…" : `${page} / 6`}</span>
          </div>
          {pageError && <p role="alert" className="field-error">{pageError}</p>}
          <button className="stranger-zoom-link" type="button" onClick={() => setZoom(true)}>필기까지 크게 보기</button>
        </div>
      </section>
      <section className="stranger-examples" aria-labelledby="stranger-examples-title">
        <h2 id="stranger-examples-title">제공된 감상평</h2>
        <p className="muted">발췌문을 읽고 생각을 나눌 때 참고해 보세요.</p>
        <blockquote><p>가끔 삶이 거대한 어항 같다는 생각이 듭니다. 인간이 만든 법과 도덕 안에서 살고, 감정까지 정해진 방식으로 보여줘야 한다는 점에서 우리는 어항 속 금붕어와 크게 다르지 않은지도 모릅니다. 세상에 정해진 의미가 없다면 허무할 수도 있지만 무의미에서 시작되는 의미만큼은 누구도 강요할 수 없는 나만의 것이기도 합니다. 이방인은 당연하다고 믿어온 삶의 규칙과 감정이 정말 내 것인지 묻게 만드는 책입니다.</p></blockquote>
        <blockquote><p>{`의욕이 없고 권태를 느끼는 주인공
소시오패스와 다를 바 없지만 사실 누구나 가져봤을 만한 감정과 생각들
결국 사회적 규범이라는 가면 아래에 있는 나의 모습이었다
감정과 사건의 본질보다 도덕성에 집착하는 등장인물들은 현실과 다를 바 없어보였고
사제 앞에서 속마음을 쏟아내는 마지막 장면은 한마디 한마디가 인상적이었다
모두가 가면을 쓴 세상에서 민낯의 주인공은 이방인이었다`}</p></blockquote>
      </section>
      <section className="reflection-feed stranger-feed" aria-labelledby="stranger-feed-title">
        <div className="feed-heading"><h2 id="stranger-feed-title">함께 나눈 감상 <span>{reflections.length}</span></h2></div>
        <form onSubmit={submitReflection} className="inline-composer">
          <label className="sr-only" htmlFor="stranger-reflection-body">감상</label>
          <textarea id="stranger-reflection-body" name="body" required maxLength={3000} rows={2}
            onFocus={startComposer} onInput={startComposer} placeholder="이 책, 어떻게 읽었나요? 짧게 남겨도 좋아요."/>
          <button className="primary" disabled={!ready}>남기기</button>
          {validation && <p role="alert" className="field-error">{validation}</p>}
        </form>
        <p className="save-notice" role="status">{notice}</p>
        {reflections.length === 0 && <p className="empty-state">아직 나눈 감상이 없어요.</p>}
        {reflections.map((item) => <ReflectionCard key={item.id}
          note={{ ...item, bookId: "stranger", title: "", quote: "", source: "", isExample: false }}
          replies={data.replies.filter((reply) => reply.reflectionId === item.id)}
          likeCount={data.likes.filter((like) => like.reflectionId === item.id).length}
          liked={data.likes.some((like) => like.reflectionId === item.id && like.userId === identity?.id)}
          ready={ready}
          onLike={async () => { if (identity) await commit({ type: "like", reflectionId: item.id, userId: identity.id }); }}
          onReply={async (body) => identity ? commit({ type: "reply", reply: { id: crypto.randomUUID(), reflectionId: item.id,
            userId: identity.id, nickname: identity.nickname, body, createdAt: new Date().toISOString() } }) : false}/>) }
      </section>
    </>}
    <p className="preview-note">{cloud ? "입력한 감상과 반응은 실험 데이터로 저장됩니다." : "미리보기 · 입력은 이 브라우저에만 저장됩니다."}</p>
    {zoom && <div className="stranger-zoom-overlay" role="presentation" onClick={() => setZoom(false)}>
      <div className="stranger-zoom-panel" role="dialog" aria-modal="true" aria-label={`${page}번째 발췌문 확대 보기`} onClick={(event) => event.stopPropagation()}>
        <button className="secondary stranger-zoom-close" type="button" autoFocus onClick={() => setZoom(false)}>닫기</button>
        <img src={scanPath(page)} alt={`이방인 발췌문 ${page} / 6 확대`} width={960} height={1440}/>
      </div>
    </div>}
  </main>;
}
