export interface PostLibraryBulkDeleteCommand {
  bookIds: number[];
}

export type PostLibraryBulkDelete = (command: PostLibraryBulkDeleteCommand) => Promise<undefined>;
