export interface DeleteLibrariesLibraryIdBooksBookIdParams {
  libraryId: number;
  bookId: number;
}

export type DeleteLibrariesLibraryIdBooksBookId = (
  params: DeleteLibrariesLibraryIdBooksBookIdParams,
) => Promise<undefined>;
