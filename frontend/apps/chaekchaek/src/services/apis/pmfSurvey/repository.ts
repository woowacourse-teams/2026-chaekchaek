import * as fetcher from './fetcher';
import { mapGetPmfSurveyModelToRequestDTO, mapGetPmfSurveyResponseDTOToModel } from './mapper';

import type { GetPmfSurvey } from './repository.types';

export const getPmfSurvey: GetPmfSurvey = async (model) => {
  const mappedModel = mapGetPmfSurveyModelToRequestDTO(model);

  const responseDTO = await fetcher.getPmfSurvey(mappedModel);

  return mapGetPmfSurveyResponseDTOToModel(responseDTO);
};
