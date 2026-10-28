type Isbn = string;

export type MyLibraryPanelProps = {
  libraryId: number;
  onSubmit: (isbns: Isbn[]) => Promise<void>;
};
