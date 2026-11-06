import type { PostPmfSurveyResponsesResponseDto } from './dto';
import type { PostPmfSurveyResponsesCommand } from './repository.types';

// PostPmfSurveyResponses
export const mapPostPmfSurveyResponsesModelToRequestDTO = (
  model: PostPmfSurveyResponsesCommand,
): PostPmfSurveyResponsesCommand => {
  return model;
};

export const mapPostPmfSurveyResponsesResponseDTOToModel = (
  response: PostPmfSurveyResponsesResponseDto,
) => {
  return response;
};
