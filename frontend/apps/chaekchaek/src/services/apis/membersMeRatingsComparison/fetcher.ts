import { instance } from '@/services/core/http';

import type {
  GetMembersMeRatingsComparisonRequestDto,
  GetMembersMeRatingsComparisonResponseDto,
} from './dto';

export const getMembersMeRatingsComparison = async ({
  query: { isbn13, criterion },
}: GetMembersMeRatingsComparisonRequestDto): Promise<GetMembersMeRatingsComparisonResponseDto> => {
  const response = await instance('/api/v1/members/me/ratings/comparison', {
    method: 'get',
    query: { isbn13, criterion },
  });

  return response.data;
};
