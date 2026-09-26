import { redirect } from "next/navigation";

export default async function LoveFragmentsExperimentPage({ searchParams }: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const query = new URLSearchParams();
  for (const [key, values] of Object.entries(await searchParams)) {
    if (key === "book") continue;
    for (const value of Array.isArray(values) ? values : values === undefined ? [] : [values]) query.append(key, value);
  }
  query.set("book", "love-fragments");
  redirect(`/experiments/stranger?${query}`);
}
