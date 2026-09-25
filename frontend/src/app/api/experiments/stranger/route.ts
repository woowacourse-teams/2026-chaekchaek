import { getReadingExperiment, postReadingExperiment } from "../../../../lib/reading-experiment-route";

export const dynamic = "force-dynamic";
export async function GET() { return getReadingExperiment("stranger"); }
export async function POST(request: Request) { return postReadingExperiment("stranger", request); }
