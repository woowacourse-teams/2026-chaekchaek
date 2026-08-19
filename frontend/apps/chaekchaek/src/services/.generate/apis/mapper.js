export const component = (name, endPoint) =>
  `${Object.entries(endPoint)
    .map(([method, endPointValue]) => {
      const upperMethod = method.charAt(0).toUpperCase() + method.slice(1);

      const key = name;
      const upperKey = name.charAt(0).toUpperCase() + name.slice(1);

      return `import type { ${upperMethod}${upperKey}ResponseDto } from "./dto";
import type {
  ${upperMethod}${upperKey}Params,
} from "./repository.types";

// ${upperMethod}${upperKey}
export const map${upperMethod}${upperKey}ModelToRequestDTO = (
  model: ${upperMethod}${upperKey}Params,
): ${upperMethod}${upperKey}Params => {
  return model;
};

export const map${upperMethod}${upperKey}ResponseDTOToModel = (
  response: ${upperMethod}${upperKey}ResponseDto,
) => {
  return response;
};
`;
    })
    .join('')}`;
