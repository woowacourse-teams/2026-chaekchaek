import type { GetBooksResponseDto } from './dto';
import type { GetBooksParams } from './repository.types';

// GetBooks
export const mapGetBooksModelToRequestDTO = (model: GetBooksParams): GetBooksParams => {
  return model;
};

export const mapGetBooksResponseDTOToModel = (response: GetBooksResponseDto) => {
  return response.data;
};
