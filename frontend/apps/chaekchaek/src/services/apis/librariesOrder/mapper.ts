import type { PatchLibrariesOrderResponseDto } from './dto';
import type { PatchLibrariesOrderParams } from './repository.types';

// PatchLibrariesOrder
export const mapPatchLibrariesOrderModelToRequestDTO = (
  model: PatchLibrariesOrderParams,
): PatchLibrariesOrderParams => {
  return model;
};

export const mapPatchLibrariesOrderResponseDTOToModel = (
  response: PatchLibrariesOrderResponseDto,
) => {
  return response;
};
