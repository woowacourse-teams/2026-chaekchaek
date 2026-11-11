export interface DeleteLibraryBookIdParams {
  bookId: number;
}

export type DeleteLibraryBookId = (params: DeleteLibraryBookIdParams) => Promise<undefined>;
