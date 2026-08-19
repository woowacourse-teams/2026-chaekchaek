export type ResponseDto<StatusCode extends number, Data> = {
  statusCode: StatusCode;
  data: Data;
};
