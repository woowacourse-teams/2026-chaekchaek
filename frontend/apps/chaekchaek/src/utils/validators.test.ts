import { describe, it, expect } from 'vitest';

import {
  isString,
  isEmptyString,
  isRequired,
  isNumericString,
  isValidMonth,
  length,
  minLength,
  maxLength,
  rangeLength,
} from './validators';

const nonStringValues = [1, undefined, null, {}, [], true];

describe('isString', () => {
  it('문자열이면 true를 반환한다', () => {
    expect(isString('string')).toBe(true);
  });

  it.each([
    ['숫자', 1],
    [undefined, undefined],
    [null, null],
    ['객체', {}],
    ['배열', []],
  ])('%s 이면 false를 반환한다', (_, value) => {
    expect(isString(value)).toBe(false);
  });
});

describe('isEmptyString', () => {
  it('빈 문자열이면 true를 반환한다', () => {
    expect(isEmptyString('')).toBe(true);
  });

  it.each(['string', ' ', ...nonStringValues])('%j 이면 false를 반환한다', (value) => {
    expect(isEmptyString(value)).toBe(false);
  });
});

describe('isRequired', () => {
  it.each(['string', '0', ' '])('빈 문자열이 아닌 %j 이면 true를 반환한다', (value) => {
    expect(isRequired(value)).toBe(true);
  });

  it.each(['', ...nonStringValues])('%j 이면 false를 반환한다', (value) => {
    expect(isRequired(value)).toBe(false);
  });
});

describe('isNumericString', () => {
  it.each(['0', '123', '001'])('숫자로만 이루어진 %j 이면 true를 반환한다', (value) => {
    expect(isNumericString(value)).toBe(true);
  });

  it.each(['', ' ', 'abc', '1a', '-1', '+1', '1.5', '1 2', ' 1', '1 ', ...nonStringValues])(
    '%j 이면 false를 반환한다',
    (value) => {
      expect(isNumericString(value)).toBe(false);
    },
  );
});

describe('isValidMonth', () => {
  it.each(['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'])(
    '두 자리 월 %j 이면 true를 반환한다',
    (value) => {
      expect(isValidMonth(value)).toBe(true);
    },
  );

  it.each(['', '00', '13', '1', '001', '1.0', '01 ', ' 01', 'ab', ...nonStringValues])(
    '%j 이면 false를 반환한다',
    (value) => {
      expect(isValidMonth(value)).toBe(false);
    },
  );
});

describe('length', () => {
  it.each([
    ['ab', false],
    ['abc', true],
    ['abcd', false],
  ])('%j의 길이가 지정한 길이와 같은지 검사한다', (value, expected) => {
    expect(length(value, { length: 3 })).toBe(expected);
  });

  it('빈 문자열의 길이는 0이다', () => {
    expect(length('', { length: 0 })).toBe(true);
  });

  it('길이를 지정하지 않으면 false를 반환한다', () => {
    expect(length('', {})).toBe(false);
  });

  it.each(nonStringValues)('문자열이 아닌 %j 이면 false를 반환한다', (value) => {
    expect(length(value, { length: 0 })).toBe(false);
  });
});

describe('minLength', () => {
  it.each([
    ['ab', false],
    ['abc', true],
    ['abcd', true],
  ])('%j의 길이가 최소 길이 이상인지 검사한다', (value, expected) => {
    expect(minLength(value, { minLength: 3 })).toBe(expected);
  });

  it.each([{}, { minLength: 0 }])('옵션이 %j 이면 빈 문자열도 허용한다', (options) => {
    expect(minLength('', options)).toBe(true);
  });

  it.each(nonStringValues)('문자열이 아닌 %j 이면 false를 반환한다', (value) => {
    expect(minLength(value, { minLength: 0 })).toBe(false);
  });
});

describe('maxLength', () => {
  it.each([
    ['ab', true],
    ['abc', true],
    ['abcd', false],
  ])('%j의 길이가 최대 길이 이하인지 검사한다', (value, expected) => {
    expect(maxLength(value, { maxLength: 3 })).toBe(expected);
  });

  it.each([{}, { maxLength: 0 }])('옵션이 %j 이면 빈 문자열만 허용한다', (options) => {
    expect(maxLength('', options)).toBe(true);
    expect(maxLength('a', options)).toBe(false);
  });

  it.each(nonStringValues)('문자열이 아닌 %j 이면 false를 반환한다', (value) => {
    expect(maxLength(value, { maxLength: 0 })).toBe(false);
  });
});

describe('rangeLength', () => {
  it.each([
    ['a', false],
    ['ab', true],
    ['abc', true],
    ['abcd', true],
    ['abcde', false],
  ])('%j의 길이가 최소 길이와 최대 길이를 포함한 범위에 있는지 검사한다', (value, expected) => {
    expect(rangeLength(value, { minLength: 2, maxLength: 4 })).toBe(expected);
  });

  it('최소 길이와 최대 길이가 0이면 빈 문자열만 허용한다', () => {
    expect(rangeLength('', { minLength: 0, maxLength: 0 })).toBe(true);
    expect(rangeLength('a', { minLength: 0, maxLength: 0 })).toBe(false);
  });

  it('최소 길이가 최대 길이보다 크면 false를 반환한다', () => {
    expect(rangeLength('abc', { minLength: 4, maxLength: 2 })).toBe(false);
  });

  it.each(nonStringValues)('문자열이 아닌 %j 이면 false를 반환한다', (value) => {
    expect(rangeLength(value, { minLength: 0, maxLength: 0 })).toBe(false);
  });
});
