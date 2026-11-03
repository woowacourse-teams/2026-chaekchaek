type Isbn = string;

export type SearchPanelProps = {
  libraryId: number;
  onSubmit: (isbns: Isbn[]) => Promise<void>;
};
