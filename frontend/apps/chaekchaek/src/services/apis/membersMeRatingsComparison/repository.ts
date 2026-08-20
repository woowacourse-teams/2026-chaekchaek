import * as fetcher from './fetcher';
import {
  mapGetMembersMeRatingsComparisonModelToRequestDTO,
  mapGetMembersMeRatingsComparisonResponseDTOToModel,
} from './mapper';

import type { GetMembersMeRatingsComparison } from './repository.types';

export const getMembersMeRatingsComparison: GetMembersMeRatingsComparison = async (model) => {
  const { isbn13, criterion } = mapGetMembersMeRatingsComparisonModelToRequestDTO(model);

  const responseDTO = await fetcher.getMembersMeRatingsComparison({
    query: { isbn13, criterion },
  });

  return mapGetMembersMeRatingsComparisonResponseDTOToModel(responseDTO);
};
