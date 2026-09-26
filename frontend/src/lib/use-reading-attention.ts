"use client";
import { useCallback, useEffect, useRef } from "react";
import { newReadingId, type ReadingBookId } from "./reading-book-config";
import type { ReadingAttentionEvent, StrangerMutation } from "./stranger-experiment";

type RecordMutation = (mutation: StrangerMutation) => Promise<boolean>;
const TICK_MS = 5_000;
const FLUSH_MS = 15_000;
const IDLE_MS = 120_000;

export function useReadingAttention(book: ReadingBookId, page: number, userId: string | undefined,
  cloud: boolean, commit: RecordMutation) {
  const visible = useRef(new Set<string>());
  const pending = useRef(new Map<string, number>());
  const currentPage = useRef(page);
  const lastTick = useRef(0);
  const lastActivity = useRef(0);

  const send = useCallback((target: string, eventType: "view" | "dwell", durationMs = 0, keepalive = false) => {
    if (!userId) return;
    const event: ReadingAttentionEvent = { id: newReadingId(book), userId, target, eventType, durationMs,
      createdAt: new Date().toISOString() };
    const mutation: StrangerMutation = { type: "attention", event };
    if (cloud) {
      const body = JSON.stringify(mutation);
      const endpoint = `/api/experiments/${book}`;
      if (keepalive && navigator.sendBeacon?.(endpoint, new Blob([body], { type: "application/json" }))) return;
      void fetch(endpoint, { method: "POST", headers: { "Content-Type": "application/json" }, body, keepalive: true }).catch(() => {});
      return;
    }
    void commit(mutation);
  }, [book, cloud, commit, userId]);

  const accrue = useCallback(() => {
    const now = performance.now();
    const elapsed = Math.min(Math.max(now - lastTick.current, 0), TICK_MS);
    lastTick.current = now;
    if (document.visibilityState !== "visible" || now - lastActivity.current > IDLE_MS) return;
    for (const marker of visible.current) {
      const target = marker === "page" ? `page:${currentPage.current}` : marker;
      pending.current.set(target, (pending.current.get(target) ?? 0) + elapsed);
    }
  }, []);

  const flush = useCallback((keepalive = false) => {
    accrue();
    for (const [target, duration] of pending.current) {
      if (duration >= 1_000) send(target, "dwell", Math.min(Math.round(duration), 300_000), keepalive);
    }
    pending.current.clear();
  }, [accrue, send]);

  useEffect(() => {
    lastTick.current = performance.now();
    lastActivity.current = lastTick.current;
    const elements = document.querySelectorAll<HTMLElement>("[data-attention]");
    const observer = new IntersectionObserver((entries) => {
      accrue();
      for (const entry of entries) {
        const marker = entry.target.getAttribute("data-attention");
        if (!marker) continue;
        const visibleHeight = entry.intersectionRect.height;
        if (entry.isIntersecting && visibleHeight >= Math.min(160, entry.boundingClientRect.height * 0.5))
          visible.current.add(marker);
        else visible.current.delete(marker);
      }
    }, { threshold: [0, 0.25, 0.5, 1] });
    elements.forEach((element) => observer.observe(element));
    const onActivity = () => { lastActivity.current = performance.now(); };
    const onVisibility = () => { flush(true); lastTick.current = performance.now(); };
    const onPageHide = () => flush(true);
    for (const name of ["scroll", "pointerdown", "keydown", "touchstart"])
      window.addEventListener(name, onActivity, { passive: true });
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("pagehide", onPageHide);
    const tick = window.setInterval(accrue, TICK_MS);
    const interval = window.setInterval(() => flush(), FLUSH_MS);
    return () => {
      window.clearInterval(tick);
      window.clearInterval(interval);
      flush(true);
      observer.disconnect();
      visible.current.clear();
      for (const name of ["scroll", "pointerdown", "keydown", "touchstart"])
        window.removeEventListener(name, onActivity);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("pagehide", onPageHide);
    };
  }, [accrue, flush]);

  useEffect(() => {
    if (!userId) return;
    flush();
    currentPage.current = page;
    send(`page:${page}`, "view");
  }, [flush, page, send, userId]);

  return useCallback((target: string) => send(target, "view"), [send]);
}
