import type { DevicePlatform } from "./stranger-experiment";

export function detectDevice(userAgent: string, maxTouchPoints = 0): DevicePlatform {
  if (/iPhone|iPod/i.test(userAgent)) return "iPhone";
  if (/iPad/i.test(userAgent) || (/Macintosh/i.test(userAgent) && maxTouchPoints > 1)) return "iPad";
  if (/Android/i.test(userAgent)) return "Android";
  if (/Windows|Macintosh|Linux|CrOS/i.test(userAgent)) return "PC";
  return "other";
}

const normalizedSource = (value: string) => value.trim().toLowerCase().replace(/\s+/g, "-")
  .replace(/[^\p{L}\p{N}._-]/gu, "").slice(0, 64);

export function detectSource(currentUrl: string, referrer: string): string {
  try {
    const current = new URL(currentUrl);
    const campaign = normalizedSource(current.searchParams.get("utm_source") ?? "");
    if (campaign) return campaign;
    if (!referrer) return "unknown";
    const referred = new URL(referrer);
    if (referred.origin === current.origin) return "unknown";
    const host = referred.hostname.toLowerCase().replace(/^www\./, "");
    if (host === "instagram.com" || host.endsWith(".instagram.com")) return "instagram";
    if (host === "kakao.com" || host.endsWith(".kakao.com")) return "kakao";
    if (host === "naver.com" || host.endsWith(".naver.com")) return "naver";
    if (host === "google.com" || host.endsWith(".google.com")) return "google";
    return normalizedSource(host) || "unknown";
  } catch { return "unknown"; }
}

export const sourceLabel = (source: string) => source === "unknown" ? "직접/확인 불가" : source;
export const deviceLabel = (device: string) => device === "other" ? "기타/확인 불가" : device;
