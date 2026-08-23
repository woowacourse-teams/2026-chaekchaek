import * as fetcher from './fetcher';
import { mapGetMembersMeModelToRequestDTO, mapGetMembersMeResponseDTOToModel } from './mapper';

import type { GetMembersMe } from './repository.types';

export const getMembersMe: GetMembersMe = async (model) => {
  const mappedModel = mapGetMembersMeModelToRequestDTO(model);

  const responseDTO = await fetcher.getMembersMe(mappedModel);

  return mapGetMembersMeResponseDTOToModel(responseDTO);
};
import {
  mapDeleteMembersMeModelToRequestDTO,
  mapDeleteMembersMeResponseDTOToModel,
} from './mapper';

import type { DeleteMembersMe } from './repository.types';

export const deleteMembersMe: DeleteMembersMe = async (model) => {
  const mappedModel = mapDeleteMembersMeModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteMembersMe(mappedModel);

  return mapDeleteMembersMeResponseDTOToModel(responseDTO);
};
