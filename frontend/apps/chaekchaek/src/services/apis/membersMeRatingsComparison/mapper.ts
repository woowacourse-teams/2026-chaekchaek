import type { GetMembersMeRatingsComparisonResponseDto } from './dto';
import type { GetMembersMeRatingsComparisonParams } from './repository.types';

// GetMembersMeRatingsComparison
export const mapGetMembersMeRatingsComparisonModelToRequestDTO = (
  model: GetMembersMeRatingsComparisonParams,
): GetMembersMeRatingsComparisonParams => {
  return model;
};

export const mapGetMembersMeRatingsComparisonResponseDTOToModel = (
  response: GetMembersMeRatingsComparisonResponseDto,
) => {
  return response;
};
