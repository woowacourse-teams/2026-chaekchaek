export interface PatchLibraryBulkStatusParams {
  bookIds: number[];
  status: string;
}

export type PatchLibraryBulkStatus = (params: PatchLibraryBulkStatusParams) => Promise<undefined>;
