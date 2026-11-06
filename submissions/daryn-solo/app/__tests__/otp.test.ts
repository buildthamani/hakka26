import {
  applyBackspace,
  applyInput,
  emptyDigits,
  extractDigits,
  formatCountdown,
  isComplete,
  joinDigits,
  verifyCode,
} from '../src/otp/otp';

const filled = (code: string) => code.split('');

describe('extractDigits', () => {
  it('pulls the code out of pasted text', () => {
    expect(extractDigits('Your code is 482913')).toBe('482913');
  });
  it('returns an empty string when there are no digits', () => {
    expect(extractDigits('abc!?')).toBe('');
  });
});

describe('applyInput', () => {
  it('fills the box and moves focus to the next one', () => {
    const result = applyInput(emptyDigits(), 0, '4');
    expect(result.digits[0]).toBe('4');
    expect(result.focusIndex).toBe(1);
  });

  it('keeps focus on the last box after typing the last digit', () => {
    expect(applyInput(emptyDigits(), 5, '9').focusIndex).toBe(5);
  });

  it('rejects non-digit input', () => {
    const start = emptyDigits();
    const result = applyInput(start, 2, 'x');
    expect(result.digits).toBe(start);
    expect(result.focusIndex).toBe(2);
  });

  it('clears the box when text is empty', () => {
    const result = applyInput(filled('123456'), 3, '');
    expect(result.digits[3]).toBe('');
    expect(result.focusIndex).toBe(3);
  });

  it('replaces the digit when typing over a filled box', () => {
    const result = applyInput(filled('123456'), 0, '17');
    expect(result.digits[0]).toBe('7');
    expect(result.focusIndex).toBe(1);
  });

  it('fills all six boxes from pasted text', () => {
    const result = applyInput(emptyDigits(), 3, 'Your code is 482913');
    expect(joinDigits(result.digits)).toBe('482913');
    expect(result.focusIndex).toBe(5);
  });

  it('ignores the old digit when a full code is pasted over a filled box', () => {
    const digits = ['2', '', '', '', '', ''];
    const appended = applyInput(digits, 0, '2Your code is 482913');
    expect(joinDigits(appended.digits)).toBe('482913');
    const prepended = applyInput(digits, 0, 'Your code is 4829132');
    expect(joinDigits(prepended.digits)).toBe('482913');
  });

  it('keeps a pasted code whose first digit matches the existing digit', () => {
    const digits = ['4', '', '', '', '', ''];
    expect(joinDigits(applyInput(digits, 0, '482913').digits)).toBe('482913');
  });

  it('fills a partial paste starting at the focused box', () => {
    const result = applyInput(emptyDigits(), 2, '99');
    expect(result.digits).toEqual(['', '', '9', '9', '', '']);
    expect(result.focusIndex).toBe(4);
  });
});

describe('applyBackspace', () => {
  it('clears the previous digit and moves back when the box is empty', () => {
    const digits = ['1', '2', '', '', '', ''];
    const result = applyBackspace(digits, 2);
    expect(result.digits).toEqual(['1', '', '', '', '', '']);
    expect(result.focusIndex).toBe(1);
  });

  it('only clears the current box when it has a digit', () => {
    const result = applyBackspace(filled('123456'), 4);
    expect(result.digits[4]).toBe('');
    expect(result.digits[3]).toBe('4');
    expect(result.focusIndex).toBe(4);
  });

  it('stays on the first box when there is nothing before it', () => {
    expect(applyBackspace(emptyDigits(), 0).focusIndex).toBe(0);
  });
});

describe('code helpers', () => {
  it('knows when the code is complete', () => {
    expect(isComplete(filled('12345'))).toBe(false);
    expect(isComplete(['1', '2', '3', '4', '5', ''])).toBe(false);
    expect(isComplete(filled('123456'))).toBe(true);
  });

  it('accepts only the demo code', () => {
    expect(verifyCode('123456')).toBe(true);
    expect(verifyCode('654321')).toBe(false);
  });

  it('formats the countdown as 0:SS', () => {
    expect(formatCountdown(30)).toBe('0:30');
    expect(formatCountdown(7)).toBe('0:07');
    expect(formatCountdown(0)).toBe('0:00');
  });
});
