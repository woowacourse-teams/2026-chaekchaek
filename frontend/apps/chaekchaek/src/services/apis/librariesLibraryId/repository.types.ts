export interface GetLibrariesLibraryIdParams {
  libraryId: number;
}

export type GetLibrariesLibraryId = (params: GetLibrariesLibraryIdParams) => Promise<{
  isDefault: boolean;
  libraryId: number;
  name: string;
  totalCount: number;
  items: {
    coverImageUrl: string;
    isbn13: string;
    totalPages: number;
    spineImageUrl: string;
    currentPage: number;
    title: string;
    readingUpdatedAt: string;
    status: string;
    authors: string[];
    bookId: number;
  }[];
  memberId: number;
}>;
export interface PostLibrariesLibraryIdCommand {
  libraryId: number;
  isbn13: string;
  totalPages: number;
  status: string;
}

export type PostLibrariesLibraryId = (command: PostLibrariesLibraryIdCommand) => Promise<undefined>;
export interface DeleteLibrariesLibraryIdParams {
  libraryId: number;
}

export type DeleteLibrariesLibraryId = (
  params: DeleteLibrariesLibraryIdParams,
) => Promise<undefined>;
export interface PatchLibrariesLibraryIdParams {
  libraryId: number;
  name: string;
}

export type PatchLibrariesLibraryId = (params: PatchLibrariesLibraryIdParams) => Promise<undefined>;
