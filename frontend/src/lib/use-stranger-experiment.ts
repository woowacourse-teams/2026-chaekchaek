"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { assignNickname } from "./nickname";
import { applyStrangerMutation, emptyStrangerExperiment, type StrangerExperiment, type StrangerMutation } from "./stranger-experiment";

const DATA_KEY = "chaekchaek-stranger-preview-v1";
const PERSON_KEY = "chaekchaek-stranger-participant-v1";
type Identity = { id: string; nickname: string };

export function useStrangerExperiment(trackVisit = false) {
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
        const response = await fetch("/api/experiments/stranger", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(mutation) });
        if (!response.ok) throw new Error("save failed");
      }
      const next = applyStrangerMutation(dataRef.current, mutation);
      if (!cloudRef.current) localStorage.setItem(DATA_KEY, JSON.stringify(next));
      dataRef.current = next;
      setData(next);
      setError("");
      return true;
    } catch {
      setError("저장하지 못했습니다. 입력은 유지됩니다. 잠시 후 다시 시도해 주세요.");
      return false;
    }
  }, []);
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        let person: Identity | null = null;
        if (trackVisit) {
          const stored = localStorage.getItem(PERSON_KEY);
          person = stored ? JSON.parse(stored) as Identity : { id: crypto.randomUUID(), nickname: assignNickname() };
          if (!person || typeof person.id !== "string" || typeof person.nickname !== "string") throw new Error("invalid identity");
          localStorage.setItem(PERSON_KEY, JSON.stringify(person));
        }
        const response = await fetch("/api/experiments/stranger", { cache: "no-store" });
        if (!active) return;
        const isCloud = response.ok;
        const initial = isCloud ? await response.json() as StrangerExperiment
          : localStorage.getItem(DATA_KEY) ? JSON.parse(localStorage.getItem(DATA_KEY)!) as StrangerExperiment : emptyStrangerExperiment();
        if (!active) return;
        dataRef.current = initial;
        setData(initial);
        cloudRef.current = isCloud;
        setCloud(isCloud);
        setIdentity(person);
        if (person) {
          const mutation: StrangerMutation = { type: "visit", userId: person.id, nickname: person.nickname };
          if (isCloud) {
            const saved = await fetch("/api/experiments/stranger", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(mutation) });
            if (!saved.ok) throw new Error("visit failed");
          }
          if (!active) return;
          const next = applyStrangerMutation(dataRef.current, mutation);
          if (!isCloud) localStorage.setItem(DATA_KEY, JSON.stringify(next));
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
  }, [trackVisit]);
  return { data, identity, ready, cloud, error, commit };
}
