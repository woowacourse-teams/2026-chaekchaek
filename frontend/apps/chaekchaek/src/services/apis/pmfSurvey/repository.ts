import { createRepositoryRequestHeaders } from '@/services/context/requestHeaders';

import * as fetcher from './fetcher';
import { mapGetPmfSurveyModelToRequestDTO, mapGetPmfSurveyResponseDTOToModel } from './mapper';

import type { GetPmfSurvey } from './repository.types';

export const getPmfSurvey: GetPmfSurvey = async (model, context) => {
  const mappedModel = mapGetPmfSurveyModelToRequestDTO(model);

  const { guestToken } = context ?? {};
  const headers = createRepositoryRequestHeaders({ guestToken });

  const responseDTO = await fetcher.getPmfSurvey({ ...mappedModel, headers });

  return mapGetPmfSurveyResponseDTOToModel(responseDTO);
};
