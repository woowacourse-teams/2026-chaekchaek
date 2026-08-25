import * as fetcher from './fetcher';
import {
  mapPatchMembersMeNicknameModelToRequestDTO,
  mapPatchMembersMeNicknameResponseDTOToModel,
} from './mapper';

import type { PatchMembersMeNickname } from './repository.types';

export const patchMembersMeNickname: PatchMembersMeNickname = async (model) => {
  const { nickname } = mapPatchMembersMeNicknameModelToRequestDTO(model);

  const responseDTO = await fetcher.patchMembersMeNickname({
    data: { nickname },
  });

  return mapPatchMembersMeNicknameResponseDTOToModel(responseDTO);
};
