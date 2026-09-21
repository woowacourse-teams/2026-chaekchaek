"use client";
import { useCallback, useEffect, useRef } from "react";
import type { BookEvent, ExperimentMutation } from "./cloud-experiment";

const FLUSH_INTERVAL_MS = 10_000;

function sendEvent(event: BookEvent, keepalive = false) {
  const body = JSON.stringify({ type: "bookEvent", event } satisfies ExperimentMutation);
  if (keepalive && navigator.sendBeacon) {
    navigator.sendBeacon("/api/experiment", new Blob([body], { type: "application/json" }));
    return;
  }
  void fetch("/api/experiment", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    keepalive,
  });
}

export function useBookAnalytics(enabled: boolean, userId?: string) {
  const active = useRef<{ bookId: string; since: number } | null>(null);

  const record = useCallback((bookId: string, eventType: BookEvent["eventType"], durationMs = 0, keepalive = false) => {
    if (!enabled || !userId) return;
    sendEvent({ id: crypto.randomUUID(), bookId, userId, eventType, durationMs, createdAt: new Date().toISOString() }, keepalive);
  }, [enabled, userId]);

  const flush = useCallback((keepalive = false) => {
    const current = active.current;
    if (!current || (!keepalive && document.visibilityState !== "visible")) return;
    const durationMs = Math.round(performance.now() - current.since);
    if (durationMs >= 1_000) record(current.bookId, "dwell", durationMs, keepalive);
    active.current = { ...current, since: performance.now() };
  }, [record]);

  useEffect(() => {
    const interval = window.setInterval(() => flush(), FLUSH_INTERVAL_MS);
    const onVisibilityChange = () => {
      if (document.visibilityState === "hidden") flush(true);
      else if (active.current) active.current = { ...active.current, since: performance.now() };
    };
    const onPageHide = () => flush(true);
    document.addEventListener("visibilitychange", onVisibilityChange);
    window.addEventListener("pagehide", onPageHide);
    return () => {
      window.clearInterval(interval);
      flush(true);
      document.removeEventListener("visibilitychange", onVisibilityChange);
      window.removeEventListener("pagehide", onPageHide);
    };
  }, [flush]);

  return useCallback((bookId: string) => {
    if (!enabled || !userId || active.current?.bookId === bookId) return;
    flush();
    record(bookId, "view");
    active.current = { bookId, since: performance.now() };
  }, [enabled, flush, record, userId]);
}
