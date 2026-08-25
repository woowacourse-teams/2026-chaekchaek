import { instance } from '@/services/core/http';

import type { PatchMembersMeNicknameRequestDto, PatchMembersMeNicknameResponseDto } from './dto';

export const patchMembersMeNickname = async ({
  data: { nickname },
}: PatchMembersMeNicknameRequestDto): Promise<PatchMembersMeNicknameResponseDto> => {
  const response = await instance('/api/v1/members/me/nickname', {
    method: 'patch',
    data: { nickname },
  });

  return response.data;
};
