import { instance } from '@/services/core/http';

import type {
  GetMembersMemberIdLibraryRequestDto,
  GetMembersMemberIdLibraryResponseDto,
} from './dto';

export const getMembersMemberIdLibrary = async ({
  pathParams: [{ value: memberId }],
  query: { page, status, sort },
}: GetMembersMemberIdLibraryRequestDto): Promise<GetMembersMemberIdLibraryResponseDto> => {
  const response = await instance(`/api/v1/members/${memberId}/library`, {
    method: 'get',
    // pathParams: [{ name: 'memberId', value: memberId }],
    query: { page, status, sort },
  });

  return response.data;
};
