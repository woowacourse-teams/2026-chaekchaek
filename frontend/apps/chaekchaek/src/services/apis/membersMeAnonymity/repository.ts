import * as fetcher from './fetcher';
import {
  mapPatchMembersMeAnonymityModelToRequestDTO,
  mapPatchMembersMeAnonymityResponseDTOToModel,
} from './mapper';

import type { PatchMembersMeAnonymity } from './repository.types';

export const patchMembersMeAnonymity: PatchMembersMeAnonymity = async (model) => {
  const { displayAnonymous } = mapPatchMembersMeAnonymityModelToRequestDTO(model);

  const responseDTO = await fetcher.patchMembersMeAnonymity({
    data: { displayAnonymous },
  });

  return mapPatchMembersMeAnonymityResponseDTOToModel(responseDTO);
};
