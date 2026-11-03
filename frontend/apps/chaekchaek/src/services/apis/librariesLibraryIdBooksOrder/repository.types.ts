export interface PatchLibrariesLibraryIdBooksOrderParams {
  libraryId: number;
  bookIds: number[];
}

export type PatchLibrariesLibraryIdBooksOrder = (
  params: PatchLibrariesLibraryIdBooksOrderParams,
) => Promise<undefined>;
