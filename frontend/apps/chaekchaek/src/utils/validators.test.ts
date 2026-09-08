import { suite, describe, it, expect } from 'vitest';

import { isString } from './validators';

suite('validators', () => {
  describe('isString', () => {
    it('문자열이 넘어오면 true를 return 한다', () => {
      expect(isString('string')).toBe(true);
    });
    it('숫자가 넘어오면 false를 return 한다', () => {
      expect(isString(1)).toBe(false);
    });
    it('undefined가 넘어오면 false를 return 한다', () => {
      expect(isString(undefined)).toBe(false);
    });
  });
});
