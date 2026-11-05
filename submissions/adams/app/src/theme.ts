import { useColorScheme } from 'react-native';

import colours from '../assets/colours.json';

type OtpPalette = typeof colours.otp.light;
export type OtpColours = { [K in keyof OtpPalette]: string };
export type Scheme = 'light' | 'dark';

/**
 * colours.json uses Android-style `#AARRGGBB` for translucent values (e.g. the
 * scrim `#99000000`). React Native reads 8-digit hex as `#RRGGBBAA`, so convert.
 */
export function toRnColour(hex: string): string {
  if (/^#[0-9a-f]{8}$/i.test(hex)) {
    return `#${hex.slice(3)}${hex.slice(1, 3)}`;
  }
  return hex;
}

function convert(palette: OtpPalette): OtpColours {
  const out = {} as OtpColours;
  (Object.keys(palette) as (keyof OtpPalette)[]).forEach((key) => {
    out[key] = toRnColour(palette[key]);
  });
  return out;
}

export const otpColours: Record<Scheme, OtpColours> = {
  light: convert(colours.otp.light),
  dark: convert(colours.otp.dark),
};

export function useScheme(): Scheme {
  return useColorScheme() === 'dark' ? 'dark' : 'light';
}

export function useOtpColours(): OtpColours {
  return otpColours[useScheme()];
}
