import { initialOtpState, otpReducer } from '../src/otp/useOtp';
import { joinDigits } from '../src/otp/otp';

const typeAll = (code: string) =>
  code.split('').reduce(
    (state, char, index) => otpReducer(state, { type: 'change', index, text: char }),
    initialOtpState,
  );

describe('otpReducer', () => {
  it('advances focus as digits are typed', () => {
    const state = otpReducer(initialOtpState, { type: 'change', index: 0, text: '1' });
    expect(state.focusedIndex).toBe(1);
  });

  it('does not submit an incomplete code', () => {
    const state = otpReducer(typeAll('123'), { type: 'submit' });
    expect(state.status).toBe('idle');
  });

  it('succeeds for 123456', () => {
    expect(otpReducer(typeAll('123456'), { type: 'submit' }).status).toBe('success');
  });

  it('on a wrong code: errors, clears boxes and requests focus on the first box', () => {
    const before = typeAll('111111');
    const after = otpReducer(before, { type: 'submit' });
    expect(after.status).toBe('error');
    expect(joinDigits(after.digits)).toBe('');
    expect(after.focusedIndex).toBe(0);
    expect(after.focusRequestId).toBeGreaterThan(before.focusRequestId);
  });

  it('clears the error once the user edits again', () => {
    const errored = otpReducer(typeAll('111111'), { type: 'submit' });
    const edited = otpReducer(errored, { type: 'change', index: 0, text: '5' });
    expect(edited.status).toBe('idle');
  });

  it('locks the form after success', () => {
    const done = otpReducer(typeAll('123456'), { type: 'submit' });
    const attempted = otpReducer(done, { type: 'change', index: 0, text: '9' });
    expect(attempted).toBe(done);
  });
});
