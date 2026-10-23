export interface DeleteLibrariesLibraryIdParams {
  libraryId: number;
}

export type DeleteLibrariesLibraryId = (
  params: DeleteLibrariesLibraryIdParams,
) => Promise<undefined>;
export interface GetLibrariesLibraryIdParams {
  libraryId: number;
  page: number;
}

export type GetLibrariesLibraryId = (params: GetLibrariesLibraryIdParams) => Promise<{
  libraryId: number;
  memberId: number;
  name: string;
  isDefault: boolean;
  totalCount: number;
  nextPage: number;
  items: {
    bookId: number;
    isbn13: string;
    title: string;
    spineImageUrl: string;
    coverImageUrl: string;
    totalPages: number;
    authors: string[];
  }[];
}>;
export interface PostLibrariesLibraryIdCommand {
  libraryId: number;
  isbn13: string;
  totalPages: number;
}

export type PostLibrariesLibraryId = (command: PostLibrariesLibraryIdCommand) => Promise<undefined>;
