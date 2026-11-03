export interface PatchLibrariesOrderParams {
  libraryIds: number[];
}

export type PatchLibrariesOrder = (params: PatchLibrariesOrderParams) => Promise<undefined>;
