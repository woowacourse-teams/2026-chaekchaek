import { ReadingExperimentPage } from "../../../components/reading-experiment-page";

export default async function StrangerExperimentPage({ searchParams }: {
  searchParams: Promise<{ book?: string | string[] }>;
}) {
  const selected = (await searchParams).book;
  const initialBook = selected === "stranger" || selected === "love-fragments" ? selected : null;
  return <ReadingExperimentPage initialBook={initialBook}/>;
}
