export type TechnologySlide = {
  id: string;
  label: string;
  category: string;
  title: string;
  problem: string;
  solution: string;
  details: string[];
  before?: { value: string; description: string };
  after?: { value: string; description: string };
  flows?: { label: string; steps: string[] }[];
  outcome: string;
  note?: string;
};
