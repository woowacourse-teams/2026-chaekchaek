import * as fetcher from './fetcher';
import { mapGetLibraryModelToRequestDTO, mapGetLibraryResponseDTOToModel } from './mapper';

import type { GetLibrary } from './repository.types';

// TODO(API_GENERATOR):
// Request DTO가 fetcher 호출 구조에 종속되어 있어 mapper 타입으로 재사용하기 어렵다.
// Repository <-> DTO 변환 대상 필드를 메타데이터 객체로 관리하고,
// 이를 기반으로 mapper 및 관련 API 코드를 생성하도록 generator 개선 필요.
export const getLibrary: GetLibrary = async (model) => {
  const { page, status, sort } = mapGetLibraryModelToRequestDTO(model);

  const responseDTO = await fetcher.getLibrary({
    query: { page, status, sort },
  });

  return mapGetLibraryResponseDTOToModel(responseDTO);
};
import { mapPostLibraryModelToRequestDTO, mapPostLibraryResponseDTOToModel } from './mapper';

import type { PostLibrary } from './repository.types';

export const postLibrary: PostLibrary = async (model) => {
  const { isbn13, totalPages, status } = mapPostLibraryModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibrary({
    data: { isbn13, totalPages, status },
  });

  return mapPostLibraryResponseDTOToModel(responseDTO);
};
