import { suite, describe, it, expect } from 'vitest';

import { isString } from './validators';

suite('validators', () => {
  describe('isString', () => {
    it('문자열이면 true를 반환한다', () => {
      expect(isString('string')).toBe(true);
    });
    it('숫자이면 false를 반환한다', () => {
      expect(isString(1)).toBe(false);
    });
    it('undefined이면 false를 반환한다', () => {
      expect(isString(undefined)).toBe(false);
    });
  });
});
