import * as fetcher from './fetcher';
import { mapGetBooksIsbnModelToRequestDTO, mapGetBooksIsbnResponseDTOToModel } from './mapper';

import type { GetBooksIsbn } from './repository.types';

export const getBooksIsbn: GetBooksIsbn = async (model) => {
  const { isbn } = mapGetBooksIsbnModelToRequestDTO(model);

  const responseDTO = await fetcher.getBooksIsbn({
    pathParams: [{ name: 'isbn', value: isbn }],
  });

  return mapGetBooksIsbnResponseDTOToModel(responseDTO);
};
