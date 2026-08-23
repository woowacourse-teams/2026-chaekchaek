import type { GetMembersMeResponseDto } from './dto';
import type { GetMembersMeParams } from './repository.types';

// GetMembersMe
export const mapGetMembersMeModelToRequestDTO = (model: GetMembersMeParams): GetMembersMeParams => {
  return model;
};

export const mapGetMembersMeResponseDTOToModel = (response: GetMembersMeResponseDto) => {
  return response;
};
import type { DeleteMembersMeResponseDto } from './dto';
import type { DeleteMembersMeParams } from './repository.types';

// DeleteMembersMe
export const mapDeleteMembersMeModelToRequestDTO = (
  model: DeleteMembersMeParams,
): DeleteMembersMeParams => {
  return model;
};

export const mapDeleteMembersMeResponseDTOToModel = (response: DeleteMembersMeResponseDto) => {
  return response;
};
