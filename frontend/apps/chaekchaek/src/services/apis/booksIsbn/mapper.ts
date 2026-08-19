import type { GetBooksIsbnResponseDto } from './dto';
import type { GetBooksIsbnParams } from './repository.types';

// GetBooksIsbn
export const mapGetBooksIsbnModelToRequestDTO = (model: GetBooksIsbnParams): GetBooksIsbnParams => {
  return model;
};

export const mapGetBooksIsbnResponseDTOToModel = (response: GetBooksIsbnResponseDto) => {
  return response;
};
