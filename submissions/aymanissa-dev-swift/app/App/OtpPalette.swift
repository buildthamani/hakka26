import SwiftUI

struct OtpPalette: Equatable {
    let sheet: Color
    let scrim: Color
    let textPrimary: Color
    let textSecondary: Color
    let digitFilledBackground: Color
    let digitFilledText: Color
    let digitFocusedBackground: Color
    let digitFocusedOutline: Color
    let digitEmptyBackground: Color
    let digitEmptyOutline: Color
    let digitPlaceholder: Color
    let buttonBackground: Color
    let buttonText: Color
    let buttonDisabledBackground: Color
    let link: Color
    let success: Color
    let error: Color

    private static let mainColourBytes: Data? = colourBytes(in: .main)
    private static let cachedLight = palette(from: mainColourBytes, scheme: .light)
    private static let cachedDark = palette(from: mainColourBytes, scheme: .dark)

    static func load(scheme: ColorScheme, bundle: Bundle = .main) -> OtpPalette {
        if bundle === Bundle.main {
            return scheme == .dark ? cachedDark : cachedLight
        }
        return palette(from: colourBytes(in: bundle), scheme: scheme)
    }

    private static func colourBytes(in bundle: Bundle) -> Data? {
        guard let url = bundle.url(forResource: "colours", withExtension: "json") else {
            return nil
        }
        return try? Data(contentsOf: url)
    }

    private static func palette(from data: Data?, scheme: ColorScheme) -> OtpPalette {
        if let data = data, let palette = try? decode(data, scheme: scheme) {
            return palette
        }
        return unresolved(scheme)
    }

    static func decode(_ data: Data, scheme: ColorScheme) throws -> OtpPalette {
        let file = try JSONDecoder().decode(OtpColorFile.self, from: data)
        let tokens = scheme == .dark ? file.otp.dark : file.otp.light
        return tokens.palette
    }

    /// Monochrome stand-in used only when colours.json cannot be read.
    private static func unresolved(_ scheme: ColorScheme) -> OtpPalette {
        let dark = scheme == .dark
        let ink: Color = dark ? .white : .black
        let paper: Color = dark ? .black : .white
        let muted = Color.gray.opacity(dark ? 0.7 : 0.55)
        return OtpPalette(
            sheet: paper,
            scrim: .black.opacity(dark ? 0.8 : 0.6),
            textPrimary: ink,
            textSecondary: muted,
            digitFilledBackground: ink,
            digitFilledText: paper,
            digitFocusedBackground: paper,
            digitFocusedOutline: ink,
            digitEmptyBackground: muted.opacity(0.25),
            digitEmptyOutline: muted.opacity(0.45),
            digitPlaceholder: muted,
            buttonBackground: ink,
            buttonText: paper,
            buttonDisabledBackground: muted.opacity(0.35),
            link: ink,
            success: .green,
            error: .red
        )
    }
}

struct OtpColorFile: Decodable {
    struct Themes: Decodable {
        let light: OtpColorTokens
        let dark: OtpColorTokens
    }

    let otp: Themes
}

struct OtpColorTokens: Decodable {
    let sheet: String
    let scrim: String
    let textPrimary: String
    let textSecondary: String
    let digitFilledBackground: String
    let digitFilledText: String
    let digitFocusedBackground: String
    let digitFocusedOutline: String
    let digitEmptyBackground: String
    let digitEmptyOutline: String
    let digitPlaceholder: String
    let buttonBackground: String
    let buttonText: String
    let buttonDisabledBackground: String
    let link: String
    let success: String
    let error: String

    enum CodingKeys: String, CodingKey {
        case sheet, scrim, link, success, error
        case textPrimary = "text_primary"
        case textSecondary = "text_secondary"
        case digitFilledBackground = "digit_filled_bg"
        case digitFilledText = "digit_filled_text"
        case digitFocusedBackground = "digit_focused_bg"
        case digitFocusedOutline = "digit_focused_outline"
        case digitEmptyBackground = "digit_empty_bg"
        case digitEmptyOutline = "digit_empty_outline"
        case digitPlaceholder = "digit_placeholder"
        case buttonBackground = "button_bg"
        case buttonText = "button_text"
        case buttonDisabledBackground = "button_disabled_bg"
    }

    var palette: OtpPalette {
        OtpPalette(
            sheet: HexColor.color(sheet),
            scrim: HexColor.color(scrim),
            textPrimary: HexColor.color(textPrimary),
            textSecondary: HexColor.color(textSecondary),
            digitFilledBackground: HexColor.color(digitFilledBackground),
            digitFilledText: HexColor.color(digitFilledText),
            digitFocusedBackground: HexColor.color(digitFocusedBackground),
            digitFocusedOutline: HexColor.color(digitFocusedOutline),
            digitEmptyBackground: HexColor.color(digitEmptyBackground),
            digitEmptyOutline: HexColor.color(digitEmptyOutline),
            digitPlaceholder: HexColor.color(digitPlaceholder),
            buttonBackground: HexColor.color(buttonBackground),
            buttonText: HexColor.color(buttonText),
            buttonDisabledBackground: HexColor.color(buttonDisabledBackground),
            link: HexColor.color(link),
            success: HexColor.color(success),
            error: HexColor.color(error)
        )
    }
}

enum HexColor {
    struct RGBA: Equatable {
        var red: Double
        var green: Double
        var blue: Double
        var alpha: Double
    }

    static func color(_ hex: String) -> Color {
        let rgba = parse(hex) ?? RGBA(red: 0, green: 0, blue: 0, alpha: 1)
        return Color(.sRGB, red: rgba.red, green: rgba.green, blue: rgba.blue, opacity: rgba.alpha)
    }

    static func parse(_ hex: String) -> RGBA? {
        var raw = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if raw.hasPrefix("#") {
            raw.removeFirst()
        }
        guard let value = UInt64(raw, radix: 16) else { return nil }
        switch raw.count {
        case 6:
            return RGBA(
                red: channel(value >> 16),
                green: channel(value >> 8),
                blue: channel(value),
                alpha: 1
            )
        case 8:
            return RGBA(
                red: channel(value >> 16),
                green: channel(value >> 8),
                blue: channel(value),
                alpha: channel(value >> 24)
            )
        default:
            return nil
        }
    }

    private static func channel(_ value: UInt64) -> Double {
        Double(value & 0xFF) / 255
    }
}
