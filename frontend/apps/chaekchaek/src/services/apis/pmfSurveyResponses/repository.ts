import * as fetcher from './fetcher';
import {
  mapPostPmfSurveyResponsesModelToRequestDTO,
  mapPostPmfSurveyResponsesResponseDTOToModel,
} from './mapper';

import type { PostPmfSurveyResponses } from './repository.types';

export const postPmfSurveyResponses: PostPmfSurveyResponses = async (model) => {
  const { submissionToken, answers } = mapPostPmfSurveyResponsesModelToRequestDTO(model);

  const responseDTO = await fetcher.postPmfSurveyResponses({
    data: { submissionToken, answers },
  });

  return mapPostPmfSurveyResponsesResponseDTOToModel(responseDTO);
};
