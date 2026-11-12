import { createRepositoryRequestHeaders } from '@/services/context/requestHeaders';
import * as fetcher from './fetcher';
import {
  mapPostPmfSurveyResponsesModelToRequestDTO,
  mapPostPmfSurveyResponsesResponseDTOToModel,
} from './mapper';

import type { PostPmfSurveyResponses } from './repository.types';

export const postPmfSurveyResponses: PostPmfSurveyResponses = async (model, context) => {
  const { submissionToken, answers } = mapPostPmfSurveyResponsesModelToRequestDTO(model);

  const { guestToken } = context ?? {};
  const headers = createRepositoryRequestHeaders({ guestToken });

  const responseDTO = await fetcher.postPmfSurveyResponses({
    data: { submissionToken, answers },
    headers,
  });

  return mapPostPmfSurveyResponsesResponseDTOToModel(responseDTO);
};
