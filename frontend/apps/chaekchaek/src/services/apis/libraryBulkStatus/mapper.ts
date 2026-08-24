import type { PatchLibraryBulkStatusResponseDto } from './dto';
import type { PatchLibraryBulkStatusParams } from './repository.types';

// PatchLibraryBulkStatus
export const mapPatchLibraryBulkStatusModelToRequestDTO = (
  model: PatchLibraryBulkStatusParams,
): PatchLibraryBulkStatusParams => {
  return model;
};

export const mapPatchLibraryBulkStatusResponseDTOToModel = (
  response: PatchLibraryBulkStatusResponseDto,
) => {
  return response;
};
