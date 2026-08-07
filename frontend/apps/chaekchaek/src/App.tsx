import { useState, useEffect } from 'react';

export function App() {
  const [health, setHealth] = useState('');

  useEffect(() => {
    const loadData = async () => {
      const res = await fetch('/health');
      const data = (await res.json()) as { ok: string };

      setHealth(data.ok);
    };

    loadData();
  }, []);
  return (
    <>
      책첵
      <span>{health}</span>
    </>
  );
}
