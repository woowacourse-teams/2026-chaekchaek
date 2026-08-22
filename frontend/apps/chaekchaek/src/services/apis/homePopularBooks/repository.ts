import * as fetcher from './fetcher';
import {
  mapGetHomePopularBooksModelToRequestDTO,
  mapGetHomePopularBooksResponseDTOToModel,
} from './mapper';

import type { GetHomePopularBooks } from './repository.types';

export const getHomePopularBooks: GetHomePopularBooks = async (model) => {
  const requestModel = mapGetHomePopularBooksModelToRequestDTO(model);

  const responseDTO = await fetcher.getHomePopularBooks(requestModel);

  return mapGetHomePopularBooksResponseDTOToModel(responseDTO);
};
