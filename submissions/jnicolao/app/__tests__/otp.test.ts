import { initialState, otpReducer, OtpState, RESEND_SECONDS, resendLabel } from '../src/otp';

const typed = (code: string): OtpState =>
  code.split('').reduce((state, digit, index) => otpReducer(state, { type: 'input', index, text: digit }), initialState);

test('typing a digit stores it and moves focus to the next box', () => {
  const state = otpReducer(initialState, { type: 'input', index: 0, text: '4' });

  expect(state.digits).toEqual(['4', '', '', '', '', '']);
  expect(state.focusedIndex).toBe(1);
});

test('non-digits are rejected', () => {
  expect(otpReducer(initialState, { type: 'input', index: 0, text: 'a' })).toBe(initialState);

  const filled = typed('2');
  expect(otpReducer(filled, { type: 'input', index: 0, text: '2x' })).toBe(filled);
});

test('typing over a filled box replaces its digit whichever side the caret was on', () => {
  expect(otpReducer(typed('21'), { type: 'input', index: 0, text: '27' }).digits.join('')).toBe('71');
  expect(otpReducer(typed('21'), { type: 'input', index: 0, text: '72' }).digits.join('')).toBe('71');
  expect(otpReducer(typed('21'), { type: 'input', index: 0, text: '22' }).digits.join('')).toBe('21');
});

test('backspace on an empty box moves back and clears the previous digit', () => {
  const state = otpReducer(typed('21'), { type: 'backspace', index: 2 });

  expect(state.digits).toEqual(['2', '', '', '', '', '']);
  expect(state.focusedIndex).toBe(1);
});

test('backspace on the first box does nothing', () => {
  expect(otpReducer(initialState, { type: 'backspace', index: 0 })).toBe(initialState);
});

test('pasting "Your code is 482913" fills the six boxes', () => {
  const state = otpReducer(initialState, { type: 'input', index: 0, text: 'Your code is 482913' });

  expect(state.digits.join('')).toBe('482913');
  expect(state.focusedIndex).toBe(5);
});

test('pasting into a box that already holds a digit still fills the six boxes', () => {
  expect(otpReducer(typed('2'), { type: 'input', index: 0, text: '2Your code is 482913' }).digits.join('')).toBe('482913');
  expect(otpReducer(typed('2'), { type: 'input', index: 0, text: 'Your code is 4829132' }).digits.join('')).toBe('482913');
});

test('submit does nothing until all six digits are filled', () => {
  const state = typed('12345');

  expect(otpReducer(state, { type: 'submit' })).toBe(state);
});

test('123456 succeeds', () => {
  expect(otpReducer(typed('123456'), { type: 'submit' }).status).toBe('success');
});

test('any other code fails, clears the boxes and focuses the first box', () => {
  const state = otpReducer(typed('654321'), { type: 'submit' });

  expect(state.status).toBe('error');
  expect(state.digits).toEqual(['', '', '', '', '', '']);
  expect(state.focusedIndex).toBe(0);
});

test('typing again after an error clears the message', () => {
  const failed = otpReducer(typed('654321'), { type: 'submit' });

  expect(otpReducer(failed, { type: 'input', index: 0, text: '1' }).status).toBe('idle');
});

test('resend is blocked until the countdown reaches zero, then restarts it', () => {
  const waiting: OtpState = { ...initialState, secondsLeft: 2 };
  expect(otpReducer(waiting, { type: 'resend' })).toBe(waiting);

  const ready = otpReducer(otpReducer(waiting, { type: 'tick' }), { type: 'tick' });
  expect(ready.secondsLeft).toBe(0);
  expect(otpReducer(ready, { type: 'resend' }).secondsLeft).toBe(RESEND_SECONDS);
});

test('the resend label counts down as 0:SS', () => {
  expect(resendLabel(30)).toBe('Resend in 0:30');
  expect(resendLabel(7)).toBe('Resend in 0:07');
  expect(resendLabel(0)).toBe('Resend');
});
