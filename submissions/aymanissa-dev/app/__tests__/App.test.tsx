import ReactTestRenderer from 'react-test-renderer';

import App from '../App';
import { COPY } from '../src/otp';

test('renders the OTP screen', async () => {
  jest.useFakeTimers();
  let renderer: ReactTestRenderer.ReactTestRenderer | undefined;
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<App />);
  });
  const tree = JSON.stringify(renderer!.toJSON());
  expect(tree).toContain(COPY.title);
  expect(tree).toContain(COPY.subtitle);
  await ReactTestRenderer.act(() => renderer!.unmount());
  jest.clearAllTimers();
  jest.useRealTimers();
});
