import type { PatchMembersMeAnonymityResponseDto } from './dto';
import type { PatchMembersMeAnonymityParams } from './repository.types';

// PatchMembersMeAnonymity
export const mapPatchMembersMeAnonymityModelToRequestDTO = (
  model: PatchMembersMeAnonymityParams,
): PatchMembersMeAnonymityParams => {
  return model;
};

export const mapPatchMembersMeAnonymityResponseDTOToModel = (
  response: PatchMembersMeAnonymityResponseDto,
) => {
  return response;
};
