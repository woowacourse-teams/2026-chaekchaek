import type { GetHomePopularBooksResponseDto } from './dto';
import type { GetHomePopularBooksParams } from './repository.types';

// GetHomePopularBooks
export const mapGetHomePopularBooksModelToRequestDTO = (
  model: GetHomePopularBooksParams,
): GetHomePopularBooksParams => {
  return model;
};

export const mapGetHomePopularBooksResponseDTOToModel = (
  response: GetHomePopularBooksResponseDto,
) => {
  return response;
};
