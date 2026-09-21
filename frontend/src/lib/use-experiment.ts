"use client";
import { useEffect, useState } from "react";
import { initialExperiment } from "./examples";
import { withCollectedBooks } from "./books";
import { assignNickname } from "./nickname";
import { readStoredExperiment, type Experiment, type Participant } from "./experiment";

export const STORAGE_KEY = "chaekchaek-experiment-preview-v1";
export const PARTICIPANT_KEY = "chaekchaek-preview-participant-v1";

export function useExperiment() {
  const [experiment, setExperiment] = useState(initialExperiment);
  const [participant, setParticipant] = useState<Participant | null>(null);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) setExperiment(withCollectedBooks(readStoredExperiment(saved)));
      const profile = localStorage.getItem(PARTICIPANT_KEY);
      const identity = profile ? JSON.parse(profile) : { id: crypto.randomUUID(), nickname: assignNickname() };
      if (!identity || typeof identity.id !== "string" || typeof identity.nickname !== "string") throw new Error("invalid participant");
      if (!identity.nickname) identity.nickname = assignNickname();
      localStorage.setItem(PARTICIPANT_KEY, JSON.stringify(identity));
      setParticipant(identity);
      setReady(true);
    } catch {
      setError("저장된 미리보기를 열지 못했습니다. 브라우저의 사이트 저장 권한과 데이터를 확인해 주세요. 기존 데이터는 변경하지 않았습니다.");
    }
    const synchronize = (event: StorageEvent) => {
      if (event.key !== STORAGE_KEY) return;
      try { setExperiment(event.newValue ? withCollectedBooks(readStoredExperiment(event.newValue)) : initialExperiment()); }
      catch { setError("다른 탭의 변경을 읽지 못했습니다. 페이지를 새로고침해 주세요."); setReady(false); }
    };
    window.addEventListener("storage", synchronize);
    return () => window.removeEventListener("storage", synchronize);
  }, []);
  function changeExperiment(update: (current: Experiment) => Experiment): boolean {
    if (!ready) return false;
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      const next = update(saved ? withCollectedBooks(readStoredExperiment(saved)) : experiment);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      setExperiment(next);
      setError("");
      return true;
    } catch {
      setError("저장하지 못했습니다. 입력은 유지됩니다. 브라우저의 사이트 저장 공간을 확인하고 다시 시도해 주세요.");
      return false;
    }
  }
  return { experiment, participant, ready, error, changeExperiment };
}
