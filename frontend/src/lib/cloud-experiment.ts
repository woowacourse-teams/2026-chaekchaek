import { initialExperiment } from "./examples";
import { readStoredExperiment, type Book, type Experiment, type Like, type Reflection, type Reply } from "./experiment";

export type CloudExperiment = { books: Book[]; reflections: Reflection[]; replies: Reply[]; likes: Like[] };
export type ExperimentMutation =
  | { type: "book"; book: Book }
  | { type: "reflection"; reflection: Reflection }
  | { type: "reply"; reply: Reply }
  | { type: "like"; reflectionId: string; userId: string };

export function mergeCloudExperiment(cloud: CloudExperiment): Experiment {
  const base = initialExperiment();
  const remote = readStoredExperiment(JSON.stringify({ version: 1, ...cloud }));
  const bookIds = new Set(base.books.map((book) => book.id));
  const reflectionIds = new Set(base.reflections.map((reflection) => reflection.id));
  return {
    version: 1,
    books: [...base.books, ...remote.books.filter((book) => !bookIds.has(book.id))],
    reflections: [...base.reflections, ...remote.reflections.filter((reflection) => !reflectionIds.has(reflection.id))],
    replies: remote.replies,
    likes: remote.likes,
  };
}
