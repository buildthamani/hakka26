import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_controller.dart';
import 'otp_theme.dart';

class OtpInput extends StatelessWidget {
  final OtpController controller;
  final OtpColors colors;

  /// Called with the full code after each user edit (not after [OtpController.clear]).
  final ValueChanged<String>? onChanged;

  const OtpInput({
    super.key,
    required this.controller,
    required this.colors,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    // Drag handles and highlights are meaningless on one-digit boxes; long-press paste still works.
    return TextSelectionTheme(
      data: const TextSelectionThemeData(
        selectionHandleColor: Colors.transparent,
        selectionColor: Colors.transparent,
      ),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 360),
        child: ListenableBuilder(
          listenable: controller,
          builder: (context, _) => Row(
            children: [
              for (var i = 0; i < controller.length; i++) ...[
                if (i > 0) const SizedBox(width: 6),
                Expanded(
                  child: AspectRatio(
                    aspectRatio: 1,
                    child: _DigitBox(
                      index: i,
                      length: controller.length,
                      field: controller.fields[i],
                      focusNode: controller.focusNodes[i],
                      colors: colors,
                      onChanged: (digits) {
                        controller.handleInput(i, digits);
                        onChanged?.call(controller.code);
                      },
                      onBackspaceOnEmpty: () {
                        controller.handleBackspaceOnEmpty(i);
                        onChanged?.call(controller.code);
                      },
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _DigitBox extends StatelessWidget {
  final int index;
  final int length;
  final TextEditingController field;
  final FocusNode focusNode;
  final OtpColors colors;
  final ValueChanged<String> onChanged;
  final VoidCallback onBackspaceOnEmpty;

  const _DigitBox({
    required this.index,
    required this.length,
    required this.field,
    required this.focusNode,
    required this.colors,
    required this.onChanged,
    required this.onBackspaceOnEmpty,
  });

  @override
  Widget build(BuildContext context) {
    final focused = focusNode.hasFocus;
    final filled = field.text.isNotEmpty;

    final Color background;
    final Color? outline;
    final Color digitColor;
    if (focused) {
      background = colors.digitFocusedBg;
      outline = colors.digitFocusedOutline;
      digitColor = colors.textPrimary;
    } else if (filled) {
      background = colors.digitFilledBg;
      outline = null;
      digitColor = colors.digitFilledText;
    } else {
      background = colors.digitEmptyBg;
      outline = colors.digitEmptyOutline;
      digitColor = colors.textPrimary;
    }

    return Semantics(
      label: 'Digit ${index + 1} of $length',
      value: field.text,
      textField: true,
      focused: focused,
      onTap: focusNode.requestFocus,
      excludeSemantics: true,
      child: Focus(
        canRequestFocus: false,
        skipTraversal: true,
        onKeyEvent: (node, event) {
          if (event is KeyDownEvent &&
              event.logicalKey == LogicalKeyboardKey.backspace &&
              field.text.isEmpty) {
            onBackspaceOnEmpty();
            return KeyEventResult.handled;
          }
          return KeyEventResult.ignored;
        },
        child: Container(
          decoration: BoxDecoration(
            color: background,
            shape: BoxShape.circle,
            border: outline != null
                ? Border.all(color: outline, width: 1.2)
                : null,
          ),
          alignment: Alignment.center,
          child: TextField(
            key: Key('otp_digit_${index + 1}'),
            controller: field,
            focusNode: focusNode,
            onChanged: onChanged,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.next,
            inputFormatters: [_NewDigitsFormatter()],
            textAlign: TextAlign.center,
            maxLines: 1,
            showCursor: false,
            decoration: InputDecoration(
              border: InputBorder.none,
              isCollapsed: true,
              hintText: '•',
              hintStyle: TextStyle(
                color: colors.digitPlaceholder,
                fontSize: 20,
              ),
            ),
            style: TextStyle(
              color: digitColor,
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}

/// Keeps only the digits the user just added, so typing into a filled box replaces its digit
/// and a paste such as "Your code is 482913" arrives as "482913".
class _NewDigitsFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    final previous = oldValue.text;
    var added = digits;
    if (previous.isNotEmpty && digits.length > previous.length) {
      if (digits.startsWith(previous)) {
        added = digits.substring(previous.length);
      } else if (digits.endsWith(previous)) {
        added = digits.substring(0, digits.length - previous.length);
      }
    }
    return TextEditingValue(
      text: added,
      selection: TextSelection.collapsed(offset: added.length),
    );
  }
}
