"use client";
import { useState, type FormEvent } from "react";
import type { Reflection, Reply } from "../lib/experiment";
import { Heart, Comment } from "./icons";
export function ReflectionCard({ note, replies, likeCount, liked, ready, onLike, onReply }: {
  note: Reflection; replies: Reply[]; likeCount: number; liked: boolean; ready: boolean;
  onLike: () => void; onReply: (body: string) => boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  const [validation, setValidation] = useState("");
  function submitReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const body = String(new FormData(form).get("reply") ?? "").trim();
    if (!body) { setValidation("답글을 입력해 주세요."); return; }
    if (onReply(body)) {
      (form.elements.namedItem("reply") as HTMLTextAreaElement).value = "";
      setValidation("");
    }
  }
  return <article className="reflection" data-testid="reflection">
    {note.title && <h3>{note.title}</h3>}
    {note.quote && <blockquote><p>{note.quote}</p>{(note.source || note.isExample) && <cite>{note.source}{note.isExample && " · 영문 번역을 바탕으로 옮긴 문구"}</cite>}</blockquote>}
    <p className="reflection-body">{note.body}</p>
    {note.sourceUrl && <a className="review-source" href={note.sourceUrl} target="_blank" rel="noreferrer">{note.source} · 원문 보기 ↗</a>}
    <div className="reflection-actions">
      <button type="button" aria-pressed={liked} disabled={!ready} onClick={onLike} className={liked ? "liked" : ""}><Heart filled={liked}/>좋아요 <span>{likeCount}</span></button>
      <button type="button" aria-expanded={expanded} aria-controls={"replies-" + note.id} onClick={() => setExpanded(!expanded)}><Comment/>답글 <span>{replies.length}</span></button>
    </div>
    {expanded && <div id={"replies-" + note.id} className="replies">
      {replies.length === 0 && <p className="muted">아직 답글이 없어요. 첫 생각을 나눠주세요.</p>}
      {replies.map((reply) => <div className="reply" key={reply.id}><p>{reply.body}</p></div>)}
      <form onSubmit={submitReply} className="reply-form">

        <label><span className="sr-only">답글 내용</span><textarea name="reply" placeholder="이 감상에 어떤 생각이 드나요?" required maxLength={1000} rows={1}/></label>
        {validation && <p role="alert" className="field-error">{validation}</p>}
        <div className="form-actions"><button className="primary" disabled={!ready}>답글 남기기</button></div>
      </form>
    </div>}
  </article>;
}
