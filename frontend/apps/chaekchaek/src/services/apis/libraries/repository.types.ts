export interface PostLibrariesCommand {
  name: string;
}

export type PostLibraries = (command: PostLibrariesCommand) => Promise<undefined>;
export interface GetLibrariesParams {
  memberId: number;
}

export type GetLibraries = (params: GetLibrariesParams) => Promise<{
  memberId: number;
  libraries: {
    libraryId: number;
    name: string;
    isDefault: boolean;
    totalCount: number;
    items: {
      bookId: number;
      title: string;
      spineImageUrl: string;
      coverImageUrl: string;
      authors: string[];
      isbn13: string;
    }[];
  }[];
}>;
