import * as fetcher from './fetcher';
import {
  mapGetMembersMemberIdLibraryModelToRequestDTO,
  mapGetMembersMemberIdLibraryResponseDTOToModel,
} from './mapper';

import type { GetMembersMemberIdLibrary } from './repository.types';

export const getMembersMemberIdLibrary: GetMembersMemberIdLibrary = async (model) => {
  const { page, status, sort, memberId } = mapGetMembersMemberIdLibraryModelToRequestDTO(model);

  const responseDTO = await fetcher.getMembersMemberIdLibrary({
    query: { page, status, sort },
    pathParams: [{ name: 'memberId', value: memberId }],
  });

  return mapGetMembersMemberIdLibraryResponseDTOToModel(responseDTO);
};
