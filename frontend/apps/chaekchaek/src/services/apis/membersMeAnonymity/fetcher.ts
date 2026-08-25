import { instance } from '@/services/core/http';

import type { PatchMembersMeAnonymityRequestDto, PatchMembersMeAnonymityResponseDto } from './dto';

export const patchMembersMeAnonymity = async ({
  data: { displayAnonymous },
}: PatchMembersMeAnonymityRequestDto): Promise<PatchMembersMeAnonymityResponseDto> => {
  const response = await instance('/api/v1/members/me/anonymity', {
    method: 'patch',
    data: { displayAnonymous },
  });

  return response.data;
};
