import 'package:flutter/material.dart';

import 'otp_colors.dart';
import 'otp_controller.dart';

/// Invisible first character of every box.
///
/// Soft keyboards report nothing when Backspace is pressed in a truly empty
/// field, so each box keeps this character for Backspace to delete. Losing it is
/// how an empty box detects Backspace on any keyboard. It is also why the
/// placeholder is drawn separately: `hintText` only shows in an empty field.
const _sentinel = '\u200B';
const _placeholder = '–';

const _maxBoxSize = 48.0;
const _boxGap = 6.0;

/// Six circular digit boxes driven by an [OtpController].
class OtpInput extends StatefulWidget {
  const OtpInput({super.key, required this.controller});

  final OtpController controller;

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  final _fields = List.generate(
    OtpController.codeLength,
    (_) => _DigitFieldController(),
  );
  late final List<FocusNode> _focusNodes = List.generate(
    OtpController.codeLength,
    (index) => FocusNode()..addListener(() => _reportFocus(index)),
  );

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_syncWithController);
  }

  @override
  void didUpdateWidget(OtpInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller.removeListener(_syncWithController);
      widget.controller.addListener(_syncWithController);
      _syncWithController();
    }
  }

  @override
  void dispose() {
    widget.controller.removeListener(_syncWithController);
    for (final field in _fields) {
      field.dispose();
    }
    for (final focusNode in _focusNodes) {
      focusNode.dispose();
    }
    super.dispose();
  }

  void _reportFocus(int index) {
    if (_focusNodes[index].hasFocus) widget.controller.focusBox(index);
  }

  void _syncWithController() {
    _syncFields();
    final focusNode = _focusNodes[widget.controller.focusedIndex];
    if (!focusNode.hasFocus) focusNode.requestFocus();
  }

  void _syncFields() {
    final digits = widget.controller.digits;
    for (var index = 0; index < _fields.length; index++) {
      _fields[index].show(digits[index]);
    }
  }

  void _handleChanged(int index, String text) {
    final otp = widget.controller;
    final shown = '$_sentinel${otp.digits[index]}';
    final entered = text.startsWith(shown)
        ? text.substring(shown.length)
        : text.replaceAll(_sentinel, '');

    if (entered.isEmpty) {
      otp.backspace(index);
    } else {
      otp.enter(index, entered);
    }
    // Rejected input changes no state, so nothing else would restore the field.
    _syncFields();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([widget.controller, ..._focusNodes]),
      builder: (context, _) {
        final digits = widget.controller.digits;
        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var index = 0; index < digits.length; index++)
              Flexible(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: _boxGap / 2),
                  child: _DigitBox(
                    position: index + 1,
                    digit: digits[index],
                    field: _fields[index],
                    focusNode: _focusNodes[index],
                    onChanged: (text) => _handleChanged(index, text),
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

/// One circle, drawn in its filled, focused or empty state.
class _DigitBox extends StatelessWidget {
  const _DigitBox({
    required this.position,
    required this.digit,
    required this.field,
    required this.focusNode,
    required this.onChanged,
  });

  final int position;
  final String digit;
  final TextEditingController field;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final isFilled = digit.isNotEmpty;
    final isFocused = focusNode.hasFocus;

    final (background, outline, digitColor) = switch ((isFocused, isFilled)) {
      (true, _) => (
        colors.digitFocusedBg,
        colors.digitFocusedOutline,
        colors.textPrimary,
      ),
      (false, true) => (
        colors.digitFilledBg,
        colors.digitFilledBg,
        colors.digitFilledText,
      ),
      (false, false) => (
        colors.digitEmptyBg,
        colors.digitEmptyOutline,
        colors.digitPlaceholder,
      ),
    };
    final digitStyle = TextStyle(
      color: digitColor,
      fontSize: 18,
      fontWeight: FontWeight.w500,
    );

    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: _maxBoxSize),
      child: AspectRatio(
        aspectRatio: 1,
        child: Semantics(
          container: true,
          label: 'Digit $position of ${OtpController.codeLength}',
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 120),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: background,
              border: Border.all(color: outline, width: isFocused ? 1.5 : 1),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                if (!isFilled && !isFocused)
                  ExcludeSemantics(
                    child: Text(_placeholder, style: digitStyle),
                  ),
                // Fills the circle so a tap anywhere inside it lands on the field.
                Positioned.fill(
                  // The caret is pinned, so its drag handle would do nothing.
                  // It is hidden rather than removed: replacing the selection
                  // controls also removes the long-press Paste menu.
                  child: TextSelectionTheme(
                    data: const TextSelectionThemeData(
                      selectionHandleColor: Colors.transparent,
                    ),
                    child: TextField(
                      key: Key('otp_digit_$position'),
                      controller: field,
                      focusNode: focusNode,
                      autofocus: position == 1,
                      keyboardType: TextInputType.number,
                      expands: true,
                      minLines: null,
                      maxLines: null,
                      textAlign: TextAlign.center,
                      textAlignVertical: TextAlignVertical.center,
                      style: digitStyle,
                      showCursor: !isFilled,
                      cursorColor: colors.digitFocusedOutline,
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        isCollapsed: true,
                      ),
                      onChanged: onChanged,
                      // Without this the keypad's action key would drop focus.
                      onEditingComplete: () {},
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Mirrors one box's digit behind the sentinel.
///
/// The selection is always collapsed at the end, so typed and pasted text lands
/// after what is already there and Backspace always has a character to delete.
class _DigitFieldController extends TextEditingController {
  _DigitFieldController() {
    show('');
  }

  void show(String digit) => value = TextEditingValue(text: '$_sentinel$digit');

  @override
  set value(TextEditingValue newValue) {
    super.value = newValue.copyWith(
      selection: TextSelection.collapsed(offset: newValue.text.length),
    );
  }
}
