import { paletteFor } from '../src/theme';

test('the scrim keeps its alpha when converted for React Native', () => {
  expect(paletteFor('light').scrim).toBe('rgba(0, 0, 0, 0.60)');
  expect(paletteFor('dark').scrim).toBe('rgba(0, 0, 0, 0.80)');
});

test('the digit box states use the challenge colours', () => {
  expect(paletteFor('light')).toMatchObject({
    digit_filled_bg: '#12361B',
    digit_filled_text: '#FFFFFF',
    digit_focused_bg: '#FFFFFF',
    digit_focused_outline: '#111111',
    digit_empty_bg: '#F1F1F1',
    digit_placeholder: '#9E9E9E',
  });
});
