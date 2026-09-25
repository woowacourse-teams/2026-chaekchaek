"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { assignNickname } from "./nickname";
import { applyStrangerMutation, emptyStrangerExperiment, type StrangerExperiment, type StrangerMutation } from "./stranger-experiment";
import { detectDevice, detectSource } from "./stranger-platform";
import { newReadingId, type ReadingBookId } from "./reading-book-config";

type Identity = { id: string; nickname: string };

export function useReadingExperiment(book: ReadingBookId, trackVisit = false) {
  const dataKey = `chaekchaek-${book}-preview-v1`;
  const personKey = `chaekchaek-${book}-participant-v1`;
  const apiPath = `/api/experiments/${book}`;
  const [data, setData] = useState<StrangerExperiment>(emptyStrangerExperiment);
  const dataRef = useRef(data);
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [ready, setReady] = useState(false);
  const [cloud, setCloud] = useState(false);
  const [error, setError] = useState("");
  const cloudRef = useRef(false);
  const readyRef = useRef(false);
  const commit = useCallback(async (mutation: StrangerMutation): Promise<boolean> => {
    if (!readyRef.current) return false;
    try {
      if (cloudRef.current) {
        const response = await fetch(apiPath, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(mutation) });
        if (!response.ok) throw new Error("save failed");
      }
      const next = applyStrangerMutation(dataRef.current, mutation);
      if (!cloudRef.current) localStorage.setItem(dataKey, JSON.stringify(next));
      dataRef.current = next;
      setData(next);
      setError("");
      return true;
    } catch {
      setError("저장하지 못했습니다. 입력은 유지됩니다. 잠시 후 다시 시도해 주세요.");
      return false;
    }
  }, [apiPath, dataKey]);
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        let person: Identity | null = null;
        if (trackVisit) {
          const stored = localStorage.getItem(personKey);
          person = stored ? JSON.parse(stored) as Identity : { id: newReadingId(book), nickname: assignNickname() };
          if (!person || typeof person.id !== "string" || typeof person.nickname !== "string") throw new Error("invalid identity");
          localStorage.setItem(personKey, JSON.stringify(person));
        }
        const response = await fetch(apiPath, { cache: "no-store" });
        if (!active) return;
        const isCloud = response.ok;
        const initial = isCloud ? await response.json() as StrangerExperiment
          : localStorage.getItem(dataKey) ? JSON.parse(localStorage.getItem(dataKey)!) as StrangerExperiment : emptyStrangerExperiment();
        if (!active) return;
        dataRef.current = initial;
        setData(initial);
        cloudRef.current = isCloud;
        setCloud(isCloud);
        setIdentity(person);
        if (person) {
          const mutation: StrangerMutation = { type: "visit", userId: person.id, nickname: person.nickname,
            source: detectSource(window.location.href, document.referrer),
            device: detectDevice(navigator.userAgent, navigator.maxTouchPoints) };
          if (isCloud) {
            const saved = await fetch(apiPath, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(mutation) });
            if (!saved.ok) throw new Error("visit failed");
          }
          if (!active) return;
          const next = applyStrangerMutation(dataRef.current, mutation);
          if (!isCloud) localStorage.setItem(dataKey, JSON.stringify(next));
          dataRef.current = next;
          setData(next);
        }
        readyRef.current = true;
        setReady(true);
      } catch {
        if (active) { setError("실험 데이터를 열지 못했습니다. 잠시 후 다시 시도해 주세요."); readyRef.current = false; setReady(false); }
      }
    }
    void load();
    return () => { active = false; };
  }, [apiPath, book, dataKey, personKey, trackVisit]);
  return { data, identity, ready, cloud, error, commit };
}

export function useStrangerExperiment(trackVisit = false) {
  return useReadingExperiment("stranger", trackVisit);
}
