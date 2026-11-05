import SwiftUI
import XCTest
@testable import App

final class OtpReducerTests: XCTestCase {
    func testInitialStateIsEmptyFocusedAndCounting() {
        let state = OtpState.initial
        XCTAssertEqual(state.digits, ["", "", "", "", "", ""])
        XCTAssertEqual(state.focusedIndex, 0)
        XCTAssertEqual(state.status, .idle)
        XCTAssertEqual(state.secondsRemaining, 30)
        XCTAssertFalse(state.canSubmit)
        XCTAssertFalse(state.canResend)
        XCTAssertEqual(state.statusMessage, "")
        XCTAssertEqual(state.resendLabel, "Resend in 0:30")
    }

    func testTypingADigitAdvancesAndTheLastBoxHoldsFocus() {
        let first = OtpReducer.reduce(.initial, .input(index: 0, text: "4"))
        XCTAssertEqual(first.digits, ["4", "", "", "", "", ""])
        XCTAssertEqual(first.focusedIndex, 1)

        let replaced = OtpReducer.reduce(first, .input(index: 0, text: "2"))
        XCTAssertEqual(replaced.digits[0], "2")
        XCTAssertEqual(replaced.focusedIndex, 1)

        var state = OtpState.initial
        for (index, character) in "123456".enumerated() {
            state = OtpReducer.reduce(state, .input(index: index, text: String(character)))
        }
        XCTAssertEqual(state.code, "123456")
        XCTAssertEqual(state.focusedIndex, 5)
        XCTAssertTrue(state.canSubmit)
    }

    func testOnlyAsciiDigitsAreAcceptedAndZeroIsKept() {
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 0, text: "a")), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 0, text: " ")), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 0, text: "٠")), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 0, text: "５")), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 0, text: "no digits")), .initial)

        let zero = OtpReducer.reduce(.initial, .input(index: 0, text: "0"))
        XCTAssertEqual(zero.digits, ["0", "", "", "", "", ""])
        XCTAssertNotEqual(zero.digits[1], "0")
        XCTAssertEqual(OtpCopy.placeholder, "–")
        XCTAssertFalse(OtpCopy.placeholder.contains(where: \.isNumber))
        XCTAssertEqual(OtpDigitText.display("0"), "0")
        XCTAssertNotEqual(OtpDigitText.display(""), "0")
        XCTAssertEqual(OtpDigitText.display(""), OtpDigitText.emptyMarker)
        XCTAssertFalse(OtpDigitText.emptyMarker.contains(where: \.isNumber))
    }

    func testBackspaceClearsTheCurrentBoxOrThePreviousOne() {
        let filled = OtpReducer.reduce(.initial, .input(index: 0, text: "4"))
        let cleared = OtpReducer.reduce(filled, .backspace(index: 0))
        XCTAssertEqual(cleared.digits[0], "")
        XCTAssertEqual(cleared.focusedIndex, 0)
        XCTAssertEqual(OtpReducer.reduce(.initial, .backspace(index: 0)), .initial)

        var state = OtpState.initial
        for (index, character) in "215".enumerated() {
            state = OtpReducer.reduce(state, .input(index: index, text: String(character)))
        }
        XCTAssertEqual(state.focusedIndex, 3)
        state = OtpReducer.reduce(state, .backspace(index: 3))
        XCTAssertEqual(state.digits, ["2", "1", "", "", "", ""])
        XCTAssertEqual(state.focusedIndex, 2)
        state = OtpReducer.reduce(state, .backspace(index: 2))
        XCTAssertEqual(state.digits, ["2", "", "", "", "", ""])
        XCTAssertEqual(state.focusedIndex, 1)
        state = OtpReducer.reduce(state, .backspace(index: 1))
        XCTAssertEqual(state.digits, ["", "", "", "", "", ""])
        XCTAssertEqual(state.focusedIndex, 0)
    }

    func testPasteExtractsDigitsFromText() {
        let sentence = OtpReducer.reduce(.initial, .input(index: 2, text: "Your code is 482913"))
        XCTAssertEqual(sentence.digits, ["4", "8", "2", "9", "1", "3"])
        XCTAssertEqual(sentence.code, "482913")
        XCTAssertEqual(sentence.focusedIndex, 5)
        XCTAssertEqual(sentence.status, .idle)
        XCTAssertTrue(sentence.canSubmit)

        XCTAssertEqual(
            OtpReducer.reduce(.initial, .input(index: 0, text: "code 48291399")).code,
            "482913"
        )
        XCTAssertEqual(
            OtpReducer.reduce(.initial, .input(index: 1, text: "4a8b2913")).code,
            "482913"
        )
        XCTAssertEqual(OtpDigitText.cleaned("\u{200B}482913"), "482913")
        XCTAssertEqual(OtpDigitText.cleaned("\u{200B}"), "")

        var partial = OtpReducer.reduce(.initial, .input(index: 0, text: "9"))
        partial = OtpReducer.reduce(partial, .input(index: 3, text: "12"))
        XCTAssertEqual(partial.digits, ["9", "", "", "1", "2", ""])
        XCTAssertEqual(partial.focusedIndex, 5)
    }

    func testSubmitAcceptsOnlyACompleteCode() {
        var state = OtpState.initial
        XCTAssertFalse(state.canSubmit)
        for (index, character) in "12345".enumerated() {
            state = OtpReducer.reduce(state, .input(index: index, text: String(character)))
            XCTAssertFalse(state.canSubmit)
            XCTAssertEqual(OtpReducer.reduce(state, .submit), state)
        }

        state = OtpReducer.reduce(state, .input(index: 5, text: "6"))
        XCTAssertTrue(state.canSubmit)
        let accepted = OtpReducer.reduce(state, .submit)
        XCTAssertEqual(accepted.status, .success)
        XCTAssertEqual(accepted.statusMessage, "You're in.")
        XCTAssertEqual(accepted.code, "123456")

        let cleared = OtpReducer.reduce(state, .backspace(index: 3))
        XCTAssertEqual(cleared.digits[3], "")
        XCTAssertFalse(cleared.canSubmit)
        XCTAssertEqual(OtpReducer.reduce(cleared, .submit), cleared)
    }

    func testWrongCodeClearsTheBoxesAndANewDigitReturnsToIdle() {
        let rejected = OtpReducer.reduce(filled("482913", secondsRemaining: 12), .submit)
        XCTAssertEqual(rejected.status, .error)
        XCTAssertEqual(rejected.statusMessage, "That code didn't work. Try again.")
        XCTAssertEqual(rejected.digits, ["", "", "", "", "", ""])
        XCTAssertEqual(rejected.focusedIndex, 0)
        XCTAssertEqual(rejected.secondsRemaining, 12)
        XCTAssertFalse(rejected.canSubmit)

        let zeros = OtpReducer.reduce(filled("000000", secondsRemaining: 9), .submit)
        XCTAssertEqual(zeros.status, .error)
        XCTAssertEqual(zeros.digits, Array(repeating: "", count: 6))
        XCTAssertEqual(zeros.focusedIndex, 0)
        XCTAssertEqual(zeros.secondsRemaining, 9)

        let typed = OtpReducer.reduce(rejected, .input(index: 0, text: "1"))
        XCTAssertEqual(typed.status, .idle)
        XCTAssertEqual(typed.statusMessage, "")
        XCTAssertEqual(typed.digits[0], "1")
    }

    func testResendCountsDownThenRestartsWithoutClearingDigits() {
        let ticking = OtpReducer.reduce(.initial, .tick)
        XCTAssertEqual(ticking.secondsRemaining, 29)
        XCTAssertEqual(ticking.resendLabel, "Resend in 0:29")
        XCTAssertEqual(OtpCopy.countdown(seconds: 30), "Resend in 0:30")
        XCTAssertEqual(OtpCopy.countdown(seconds: 5), "Resend in 0:05")
        XCTAssertEqual(OtpCopy.countdown(seconds: 0), "Resend in 0:00")

        var open = OtpState.initial
        open.secondsRemaining = 1
        open = OtpReducer.reduce(open, .tick)
        XCTAssertEqual(open.secondsRemaining, 0)
        XCTAssertTrue(open.canResend)
        XCTAssertEqual(open.resendLabel, "Didn't receive the code? Resend")
        XCTAssertFalse(open.resendLabel.hasSuffix("."))
        XCTAssertEqual(OtpReducer.reduce(open, .tick).secondsRemaining, 0)
        XCTAssertEqual(OtpReducer.reduce(.initial, .resend).secondsRemaining, 30)

        let held = OtpState(
            digits: ["2", "1", "5", "", "", ""],
            focusedIndex: 3,
            status: .error,
            secondsRemaining: 0
        )
        let blocked = OtpState(
            digits: held.digits,
            focusedIndex: held.focusedIndex,
            status: .idle,
            secondsRemaining: 12
        )
        XCTAssertEqual(OtpReducer.reduce(blocked, .resend), blocked)

        let restarted = OtpReducer.reduce(held, .resend)
        XCTAssertEqual(restarted.secondsRemaining, 30)
        XCTAssertEqual(restarted.digits, held.digits)
        XCTAssertEqual(restarted.status, .error)
        XCTAssertEqual(restarted.resendLabel, "Resend in 0:30")
        XCTAssertFalse(restarted.canResend)
    }

    func testFocusStaysInsideTheBoxes() {
        XCTAssertEqual(OtpReducer.reduce(.initial, .focus(0)), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .focus(3)).focusedIndex, 3)
        XCTAssertEqual(OtpReducer.reduce(.initial, .focus(6)), .initial)
        XCTAssertEqual(OtpReducer.reduce(.initial, .input(index: 8, text: "1")), .initial)
    }

    func testCopyAndIdentifiersMatchTheChallengeList() {
        XCTAssertEqual(OtpCopy.title, "Enter your code")
        XCTAssertEqual(OtpCopy.subtitle, "A 6-digit code has been sent to your phone.")
        XCTAssertEqual(OtpCopy.resendIdle, "Didn't receive the code? Resend")
        XCTAssertEqual(OtpCopy.resendIdle, OtpCopy.resendPrompt + OtpCopy.resendAction)
        XCTAssertEqual(OtpCopy.submit, "Submit")
        XCTAssertEqual(OtpCopy.success, "You're in.")
        XCTAssertEqual(OtpCopy.failure, "That code didn't work. Try again.")
        XCTAssertEqual(OtpCopy.digitLabel(1), "Digit 1 of 6")
        XCTAssertEqual(OtpCopy.digitLabel(6), "Digit 6 of 6")
        XCTAssertEqual(OtpID.digits, [
            "otp_digit_1",
            "otp_digit_2",
            "otp_digit_3",
            "otp_digit_4",
            "otp_digit_5",
            "otp_digit_6",
        ])
        XCTAssertEqual(OtpID.submit, "otp_submit")
        XCTAssertEqual(OtpID.resend, "otp_resend")
        XCTAssertEqual(OtpID.status, "otp_status")
    }

    func testStoreAppliesDigitsAndTicksWithoutAClock() {
        let store = OtpStore(startClock: false)
        store.send(.input(index: 0, text: "0"))
        XCTAssertEqual(store.state.digits, ["0", "", "", "", "", ""])
        XCTAssertEqual(store.state.secondsRemaining, 30)
        XCTAssertEqual(store.state.resendLabel, "Resend in 0:30")
        XCTAssertFalse(store.state.canSubmit)
        store.send(.tick)
        XCTAssertEqual(store.state.secondsRemaining, 29)
        XCTAssertEqual(store.state.resendLabel, "Resend in 0:29")
        store.send(.input(index: 0, text: "a"))
        XCTAssertEqual(store.state.digits[0], "0")
    }

    private func filled(_ code: String, secondsRemaining: Int = 30) -> OtpState {
        OtpState(
            digits: code.map { String($0) },
            focusedIndex: OtpState.length - 1,
            status: .idle,
            secondsRemaining: secondsRemaining
        )
    }
}

final class OtpPaletteTests: XCTestCase {
    func testHexParsesRGBAndLeadingAlpha() {
        let green = HexColor.parse("#12361B")
        XCTAssertEqual(green?.red ?? -1, Double(0x12) / 255, accuracy: 0.001)
        XCTAssertEqual(green?.green ?? -1, Double(0x36) / 255, accuracy: 0.001)
        XCTAssertEqual(green?.blue ?? -1, Double(0x1B) / 255, accuracy: 0.001)
        XCTAssertEqual(green?.alpha ?? -1, 1, accuracy: 0.001)

        let scrim = HexColor.parse("#99000000")
        XCTAssertEqual(scrim?.alpha ?? -1, Double(0x99) / 255, accuracy: 0.001)
        XCTAssertEqual(scrim?.red ?? -1, 0, accuracy: 0.001)
        XCTAssertEqual(scrim?.green ?? -1, 0, accuracy: 0.001)
        XCTAssertEqual(scrim?.blue ?? -1, 0, accuracy: 0.001)
        XCTAssertNil(HexColor.parse("#12"))
    }

    func testBundledColoursDriveLightAndDark() throws {
        let url = try XCTUnwrap(Bundle.main.url(forResource: "colours", withExtension: "json"))
        let data = try Data(contentsOf: url)
        let file = try JSONDecoder().decode(OtpColorFile.self, from: data)

        XCTAssertEqual(file.otp.light.digitFilledBackground, "#12361B")
        XCTAssertEqual(file.otp.light.digitFilledText, "#FFFFFF")
        XCTAssertEqual(file.otp.light.digitFocusedBackground, "#FFFFFF")
        XCTAssertEqual(file.otp.light.digitFocusedOutline, "#111111")
        XCTAssertEqual(file.otp.light.digitEmptyBackground, "#F1F1F1")
        XCTAssertEqual(file.otp.light.digitPlaceholder, "#9E9E9E")
        XCTAssertNotEqual(file.otp.light.buttonBackground, file.otp.light.buttonDisabledBackground)
        XCTAssertNotEqual(file.otp.light.digitFilledBackground, file.otp.light.digitEmptyBackground)
        XCTAssertNotEqual(file.otp.light.digitFilledBackground, file.otp.light.digitFocusedBackground)

        XCTAssertEqual(file.otp.dark.sheet, "#161A17")
        XCTAssertEqual(file.otp.dark.digitFilledBackground, "#2E7D45")
        XCTAssertEqual(file.otp.dark.digitFilledText, "#FFFFFF")
        XCTAssertNotEqual(file.otp.light.sheet, file.otp.dark.sheet)
        XCTAssertNotEqual(file.otp.light.textPrimary, file.otp.dark.textPrimary)
        XCTAssertNotEqual(file.otp.light.digitFilledBackground, file.otp.dark.digitFilledBackground)

        let light = try OtpPalette.decode(data, scheme: .light)
        let dark = try OtpPalette.decode(data, scheme: .dark)
        XCTAssertNotEqual(light, dark)
        XCTAssertEqual(OtpPalette.load(scheme: .light), light)
        XCTAssertEqual(OtpPalette.load(scheme: .dark), dark)
    }
}
