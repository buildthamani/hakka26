import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_code.dart';
import 'otp_colors.dart';

enum OtpBoxState { filled, focused, empty }

/// Six circular digit boxes driven by one hidden, autofocused numeric field.
///
/// The single field gives reliable backspace on an empty box (it just deletes the
/// previous digit), whole-code paste and one-time-code autofill. The boxes only render.
class OtpInput extends StatefulWidget {
  const OtpInput({
    super.key,
    required this.code,
    required this.focusNode,
    required this.onChanged,
    this.enabled = true,
  });

  final String code;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;
  final bool enabled;

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  late final TextEditingController _text = TextEditingController(text: widget.code);

  @override
  void initState() {
    super.initState();
    widget.focusNode.addListener(_onFocusChange);
  }

  @override
  void didUpdateWidget(OtpInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.focusNode != widget.focusNode) {
      oldWidget.focusNode.removeListener(_onFocusChange);
      widget.focusNode.addListener(_onFocusChange);
    }
    if (_text.text != widget.code) {
      _text.value = TextEditingValue(
        text: widget.code,
        selection: TextSelection.collapsed(offset: widget.code.length),
      );
    }
  }

  @override
  void dispose() {
    widget.focusNode.removeListener(_onFocusChange);
    _text.dispose();
    super.dispose();
  }

  void _onFocusChange() => setState(() {});

  Future<void> _pasteFromClipboard() async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    final pasted = nextCode(widget.code, widget.code + (data?.text ?? ''));
    if (pasted != widget.code) widget.onChanged(pasted);
    widget.focusNode.requestFocus();
  }

  OtpBoxState _stateFor(int index) {
    if (index < widget.code.length) return OtpBoxState.filled;
    final isActive = index == widget.code.length;
    return isActive && widget.focusNode.hasFocus && widget.enabled ? OtpBoxState.focused : OtpBoxState.empty;
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned.fill(
          child: Opacity(
            opacity: 0,
            alwaysIncludeSemantics: true,
            child: TextField(
              key: const Key('otp_input'),
              controller: _text,
              focusNode: widget.focusNode,
              enabled: widget.enabled,
              autofocus: true,
              keyboardType: TextInputType.number,
              textInputAction: TextInputAction.done,
              autofillHints: const [AutofillHints.oneTimeCode],
              inputFormatters: const [_OtpFormatter()],
              showCursor: false,
              enableInteractiveSelection: false,
              decoration: const InputDecoration(
                border: InputBorder.none,
                counterText: '',
                semanticCounterText: '',
                labelText: 'One-time code, $kOtpLength digits',
              ),
              onChanged: widget.onChanged,
            ),
          ),
        ),
        GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: widget.enabled ? widget.focusNode.requestFocus : null,
          onLongPress: widget.enabled ? _pasteFromClipboard : null,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var i = 0; i < kOtpLength; i++) ...[
                if (i > 0) const SizedBox(width: 10),
                Flexible(
                  child: OtpDigitBox(
                    key: Key('otp_digit_${i + 1}'),
                    position: i + 1,
                    digit: i < widget.code.length ? widget.code[i] : '',
                    state: _stateFor(i),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

/// One circular box. Announced to screen readers as "Digit N of 6".
class OtpDigitBox extends StatelessWidget {
  const OtpDigitBox({
    super.key,
    required this.position,
    required this.digit,
    required this.state,
  });

  /// Placeholder for empty boxes; deliberately not `0`, so it can't be mistaken for a digit.
  static const placeholder = '•';

  final int position;
  final String digit;
  final OtpBoxState state;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final (Color background, Color border, double borderWidth, Color textColor) = switch (state) {
      OtpBoxState.filled => (colors.digitFilledBg, colors.digitFilledBg, 1.0, colors.digitFilledText),
      OtpBoxState.focused => (colors.digitFocusedBg, colors.digitFocusedOutline, 2.0, colors.textPrimary),
      OtpBoxState.empty => (colors.digitEmptyBg, colors.digitEmptyOutline, 1.0, colors.digitPlaceholder),
    };

    return Semantics(
      container: true,
      label: 'Digit $position of $kOtpLength',
      value: digit.isEmpty ? 'empty' : digit,
      child: ExcludeSemantics(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 48),
          child: AspectRatio(
            aspectRatio: 1,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 120),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: background,
                border: Border.all(color: border, width: borderWidth),
              ),
              child: Text(
                digit.isEmpty ? placeholder : digit,
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500, color: textColor),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Applies [nextCode] to every edit: digits only, paste extraction, max six digits.
class _OtpFormatter extends TextInputFormatter {
  const _OtpFormatter();

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final code = nextCode(oldValue.text, newValue.text);
    return TextEditingValue(text: code, selection: TextSelection.collapsed(offset: code.length));
  }
}
