export function Heart({ filled = false }: { filled?: boolean }) {
  return <svg viewBox="0 0 24 24" aria-hidden="true" fill={filled ? "currentColor" : "none"}><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/></svg>;
}
export function Comment() {
  return <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"><path d="M20 13a4 4 0 0 1-4 4H8l-5 4V7a4 4 0 0 1 4-4h9a4 4 0 0 1 4 4Z"/></svg>;
}
