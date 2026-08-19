import * as fetcher from './fetcher';
import { mapGetBooksModelToRequestDTO, mapGetBooksResponseDTOToModel } from './mapper';

import type { GetBooks } from './repository.types';

export const getBooks: GetBooks = async (model) => {
  const { query, page } = mapGetBooksModelToRequestDTO(model);

  const responseDTO = await fetcher.getBooks({
    query: { query, page },
  });

  return mapGetBooksResponseDTOToModel(responseDTO);
};
