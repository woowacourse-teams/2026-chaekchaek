import type { GetPmfSurveyResponseDto } from './dto';
import type { GetPmfSurveyParams } from './repository.types';

// GetPmfSurvey
export const mapGetPmfSurveyModelToRequestDTO = (model: GetPmfSurveyParams): GetPmfSurveyParams => {
  return model;
};

export const mapGetPmfSurveyResponseDTOToModel = (response: GetPmfSurveyResponseDto) => {
  return response;
};
