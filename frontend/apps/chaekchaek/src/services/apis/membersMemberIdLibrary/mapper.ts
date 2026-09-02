import type { GetMembersMemberIdLibraryResponseDto } from './dto';
import type { GetMembersMemberIdLibraryParams } from './repository.types';

// GetMembersMemberIdLibrary
export const mapGetMembersMemberIdLibraryModelToRequestDTO = (
  model: GetMembersMemberIdLibraryParams,
): { page: number; status: '' | 'WANT_TO_READ' | 'READING' | 'FINISHED'; sort: string } => {
  return { ...model, status: model.status === 'ALL' ? '' : model.status };
};

export const mapGetMembersMemberIdLibraryResponseDTOToModel = (
  response: GetMembersMemberIdLibraryResponseDto,
) => {
  return response;
};
