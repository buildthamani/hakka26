import ReactTestRenderer from 'react-test-renderer';
import { Text, TextInput } from 'react-native';

import App from '../App';
import colours from '../assets/colours.json';
import { COPY } from '../src/otp';

const light = colours.otp.light;

function styleOf(node: ReactTestRenderer.ReactTestInstance): Record<string, unknown> {
  const merged: Record<string, unknown> = {};
  const visit = (value: unknown) => {
    if (!value) return;
    if (Array.isArray(value)) {
      value.forEach(visit);
      return;
    }
    if (typeof value === 'object') Object.assign(merged, value);
  };
  visit(node.props.style);
  return merged;
}

function textIn(node: ReactTestRenderer.ReactTestInstance): string {
  return node
    .findAllByType(Text)
    .map((text) => {
      const children = text.props.children;
      if (typeof children === 'string' || typeof children === 'number') return String(children);
      return '';
    })
    .join('');
}

async function renderApp() {
  let renderer: ReactTestRenderer.ReactTestRenderer | undefined;
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<App />);
  });
  return renderer!;
}

function digit(renderer: ReactTestRenderer.ReactTestRenderer, index: number) {
  return renderer.root.findByProps({ testID: `otp_digit_${index}` });
}

async function typeDigit(renderer: ReactTestRenderer.ReactTestRenderer, index: number, value: string) {
  await ReactTestRenderer.act(() => {
    digit(renderer, index).props.onChangeText(value);
  });
}

async function typeCode(renderer: ReactTestRenderer.ReactTestRenderer, code: string) {
  for (let index = 0; index < code.length; index += 1) {
    await typeDigit(renderer, index + 1, code[index]);
  }
}

describe('OTP verification screen', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  test('renders six digit boxes, copy, and the initial countdown', async () => {
    const renderer = await renderApp();
    try {
      expect(textIn(renderer.root)).toContain(COPY.title);
      expect(textIn(renderer.root)).toContain(COPY.subtitle);
      expect(textIn(renderer.root)).toContain(COPY.submit);
      expect(textIn(renderer.root)).toContain('Resend in 0:30');
      expect(textIn(renderer.root)).not.toContain("Didn't receive the code?");

      for (let index = 1; index <= 6; index += 1) {
        const box = digit(renderer, index);
        expect(box.props.accessibilityLabel).toBe(`Digit ${index} of 6`);
        expect(box.props.keyboardType).toBe('number-pad');
        expect(box.props.value).toBe('');
      }
      expect(digit(renderer, 1).props.autoFocus).toBe(true);
      expect(renderer.root.findAllByType(TextInput)).toHaveLength(6);
      expect(renderer.root.findAllByProps({ children: '–' }).length).toBeGreaterThanOrEqual(5);
      expect(digit(renderer, 1).props.value).not.toBe('0');

      const submit = renderer.root.findByProps({ testID: 'otp_submit' });
      expect(submit.props.disabled).toBe(true);
      expect(textIn(renderer.root.findByProps({ testID: 'otp_status' }))).toBe('');

      const resend = renderer.root.findByProps({ testID: 'otp_resend' });
      expect(resend.props.disabled).toBe(true);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('typing a digit fills the box and moves focus to the next one', async () => {
    const renderer = await renderApp();
    try {
      await typeDigit(renderer, 1, '2');
      expect(digit(renderer, 1).props.value).toBe('2');

      const filled = digit(renderer, 1).parent!;
      expect(styleOf(filled).backgroundColor).toBe(light.digit_filled_bg);
      const glyph = filled.findAllByType(Text).find((node) => node.props.children === '2');
      expect(glyph).toBeTruthy();
      expect(styleOf(glyph!).color).toBe(light.digit_filled_text);

      const focused = digit(renderer, 2).parent!;
      expect(styleOf(focused).backgroundColor).toBe(light.digit_focused_bg);
      expect(styleOf(focused).borderColor).toBe(light.digit_focused_outline);

      const empty = digit(renderer, 3).parent!;
      expect(styleOf(empty).backgroundColor).toBe(light.digit_empty_bg);
      expect(empty.findByType(Text).props.children).toBe('–');
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('rejects non-digits', async () => {
    const renderer = await renderApp();
    try {
      await typeDigit(renderer, 1, 'a');
      expect(digit(renderer, 1).props.value).toBe('');
      const focused = digit(renderer, 1).parent!;
      expect(styleOf(focused).backgroundColor).toBe(light.digit_focused_bg);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('backspace on an empty box clears the previous digit and moves back', async () => {
    const renderer = await renderApp();
    try {
      await typeCode(renderer, '21');
      expect(digit(renderer, 2).props.value).toBe('1');

      await ReactTestRenderer.act(() => {
        digit(renderer, 3).props.onKeyPress({ nativeEvent: { key: 'Backspace' } });
      });

      expect(digit(renderer, 1).props.value).toBe('2');
      expect(digit(renderer, 2).props.value).toBe('');
      const focused = digit(renderer, 2).parent!;
      expect(styleOf(focused).backgroundColor).toBe(light.digit_focused_bg);
      expect(styleOf(focused).borderColor).toBe(light.digit_focused_outline);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('pasting "Your code is 482913" extracts the six digits', async () => {
    const renderer = await renderApp();
    try {
      await typeDigit(renderer, 1, 'Your code is 482913');
      expect([1, 2, 3, 4, 5, 6].map((index) => digit(renderer, index).props.value).join('')).toBe('482913');
      expect(renderer.root.findByProps({ testID: 'otp_submit' }).props.disabled).toBe(false);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('submit stays disabled until all six digits are filled', async () => {
    const renderer = await renderApp();
    try {
      const submit = () => renderer.root.findByProps({ testID: 'otp_submit' });
      await typeCode(renderer, '12345');
      expect(submit().props.disabled).toBe(true);
      await ReactTestRenderer.act(() => {
        submit().props.onPress();
      });
      expect(textIn(renderer.root.findByProps({ testID: 'otp_status' }))).toBe('');

      await typeDigit(renderer, 6, '6');
      expect(submit().props.disabled).toBe(false);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('123456 shows exactly "You\'re in."', async () => {
    const renderer = await renderApp();
    try {
      await typeCode(renderer, '123456');
      await ReactTestRenderer.act(() => {
        renderer.root.findByProps({ testID: 'otp_submit' }).props.onPress();
      });
      expect(textIn(renderer.root.findByProps({ testID: 'otp_status' }))).toBe(COPY.success);
      expect([1, 2, 3, 4, 5, 6].map((index) => digit(renderer, index).props.value).join('')).toBe('123456');
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('a wrong code shows the error, clears the boxes, and focuses the first', async () => {
    const renderer = await renderApp();
    try {
      await typeCode(renderer, '654321');
      await ReactTestRenderer.act(() => {
        renderer.root.findByProps({ testID: 'otp_submit' }).props.onPress();
      });

      expect(textIn(renderer.root.findByProps({ testID: 'otp_status' }))).toBe(COPY.error);
      for (let index = 1; index <= 6; index += 1) {
        expect(digit(renderer, index).props.value).toBe('');
      }
      const first = digit(renderer, 1).parent!;
      expect(styleOf(first).backgroundColor).toBe(light.digit_focused_bg);
      expect(styleOf(first).borderColor).toBe(light.digit_focused_outline);
      expect(renderer.root.findByProps({ testID: 'otp_submit' }).props.disabled).toBe(true);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });

  test('resend starts on a 30s countdown, then can be tapped to restart it', async () => {
    const renderer = await renderApp();
    try {
      const resend = () => renderer.root.findByProps({ testID: 'otp_resend' });
      expect(textIn(resend())).toBe('Resend in 0:30');
      expect(textIn(renderer.root)).not.toContain("Didn't receive the code?");
      expect(resend().props.disabled).toBe(true);

      await ReactTestRenderer.act(() => {
        resend().props.onPress();
      });
      expect(textIn(resend())).toBe('Resend in 0:30');

      await ReactTestRenderer.act(() => {
        jest.advanceTimersByTime(1000);
      });
      expect(textIn(resend())).toBe('Resend in 0:29');

      await ReactTestRenderer.act(() => {
        jest.advanceTimersByTime(29_000);
      });
      expect(textIn(resend())).toBe('Resend');
      expect(textIn(renderer.root)).toContain(COPY.resendIdle);
      expect(textIn(renderer.root)).not.toContain('Resend in 0:');
      expect(resend().props.disabled).toBe(false);
      expect(resend().props.accessibilityLabel).toBe(COPY.resendIdle);

      await ReactTestRenderer.act(() => {
        resend().props.onPress();
      });
      expect(textIn(resend())).toBe('Resend in 0:30');
      expect(resend().props.disabled).toBe(true);
    } finally {
      await ReactTestRenderer.act(() => renderer.unmount());
    }
  });
});
