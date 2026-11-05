import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'palette.dart';

/// The six-box OTP entry row.
///
/// A single, invisible [TextField] owns the real text input so pasting a whole
/// SMS message works naturally. The visible boxes are driven by [digits], and
/// the box at `digits.length` is shown as focused. Typing appends a digit and
/// advances that focus; backspace removes the trailing digit, which is the
/// same as clearing the previous box and moving the focus back.
class OtpInput extends StatelessWidget {
  const OtpInput({
    super.key,
    required this.digits,
    required this.colors,
    required this.controller,
    required this.focusNode,
    required this.onChanged,
    this.boxSize = 48,
  });

  static const int length = 6;
  static const double gap = 10;

  final String digits;
  final OtpColors colors;
  final TextEditingController controller;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;
  final double boxSize;

  void _focusInput() {
    if (!focusNode.hasFocus) {
      focusNode.requestFocus();
    }
    controller.selection = TextSelection.collapsed(offset: controller.text.length);
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var index = 0; index < length; index++) ...[
              if (index > 0) const SizedBox(width: gap),
              _OtpBox(
                index: index,
                digit: index < digits.length ? digits[index] : null,
                focused: index == digits.length,
                size: boxSize,
                colors: colors,
                onTap: _focusInput,
              ),
            ],
          ],
        ),
        Positioned.fill(
          child: IgnorePointer(
            child: Opacity(
              opacity: 0,
              child: ExcludeSemantics(
                child: TextField(
                  key: const Key('otp_input'),
                  controller: controller,
                  focusNode: focusNode,
                  autofocus: true,
                  keyboardType: TextInputType.number,
                  textInputAction: TextInputAction.done,
                  autofillHints: const [AutofillHints.oneTimeCode],
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(length),
                  ],
                  showCursor: false,
                  enableSuggestions: false,
                  autocorrect: false,
                  decoration: const InputDecoration(
                    isDense: true,
                    contentPadding: EdgeInsets.zero,
                    border: InputBorder.none,
                    counterText: '',
                  ),
                  style: const TextStyle(fontSize: 1),
                  onChanged: onChanged,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _OtpBox extends StatelessWidget {
  const _OtpBox({
    required this.index,
    required this.digit,
    required this.focused,
    required this.size,
    required this.colors,
    required this.onTap,
  });

  final int index;
  final String? digit;
  final bool focused;
  final double size;
  final OtpColors colors;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final filled = digit != null;
    final background = filled
        ? colors.digitFilledBg
        : focused
            ? colors.digitFocusedBg
            : colors.digitEmptyBg;
    final outline = filled
        ? Colors.transparent
        : focused
            ? colors.digitFocusedOutline
            : colors.digitEmptyOutline;
    final foreground = filled
        ? colors.digitFilledText
        : focused
            ? colors.textPrimary
            : colors.digitPlaceholder;

    return Semantics(
      key: Key('otp_digit_${index + 1}'),
      container: true,
      label: 'Digit ${index + 1} of 6',
      value: filled ? digit : 'empty',
      onTap: onTap,
      child: ExcludeSemantics(
        child: GestureDetector(
          onTap: onTap,
          behavior: HitTestBehavior.opaque,
          child: AnimatedContainer(
            key: Key('otp_box_${index + 1}'),
            duration: const Duration(milliseconds: 150),
            curve: Curves.easeOut,
            width: size,
            height: size,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: background,
              border: Border.all(
                color: outline,
                width: focused ? 2 : 1,
              ),
            ),
            child: Text(
              filled ? digit! : '-',
              style: TextStyle(
                color: foreground,
                fontSize: size * 0.38,
                fontWeight: FontWeight.w600,
                height: 1,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
