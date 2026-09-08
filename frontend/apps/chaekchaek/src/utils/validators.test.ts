import { suite, describe, it, expect } from 'vitest';

import { isString } from './validators';

suite('validators', () => {
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
});
