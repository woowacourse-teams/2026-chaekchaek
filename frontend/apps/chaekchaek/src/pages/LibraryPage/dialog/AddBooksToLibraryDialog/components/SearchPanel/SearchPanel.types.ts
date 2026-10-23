type Isbn = string;

export type SearchPanelProps = {
  onSubmit: (isbns: Isbn[]) => Promise<void>;
};
