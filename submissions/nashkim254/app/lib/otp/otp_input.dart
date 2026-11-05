import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/otp_colors.dart';
import 'otp_controller.dart';
import 'otp_strings.dart';

/// Six circular digit boxes driven by an [OtpController].
///
/// The controller owns the digits and decides which box is active; this
/// widget owns the text fields and focus nodes and keeps them in sync.
class OtpInput extends StatefulWidget {
  const OtpInput({super.key, required this.controller});

  final OtpController controller;

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  late final List<TextEditingController> _fields;
  late final List<FocusNode> _focusNodes;
  late int _handledFocusRequest;

  OtpController get _otp => widget.controller;

  @override
  void initState() {
    super.initState();
    _fields = List.generate(_otp.length, (_) => TextEditingController());
    _focusNodes = List.generate(_otp.length, (i) {
      final node = FocusNode(
        debugLabel: OtpTestIds.digit(i + 1),
        onKeyEvent: (_, event) => _onKey(i, event),
      );
      node.addListener(() => _onFocusChange(i));
      return node;
    });
    _handledFocusRequest = _otp.focusRequest;
    _otp.addListener(_syncFromController);
    _syncFromController();
  }

  @override
  void dispose() {
    _otp.removeListener(_syncFromController);
    for (final field in _fields) {
      field.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  /// Mirrors the controller's digits into the fields and moves focus when asked.
  void _syncFromController() {
    final digits = _otp.digits;
    for (var i = 0; i < _fields.length; i++) {
      if (_fields[i].text != digits[i]) {
        _fields[i].value = TextEditingValue(
          text: digits[i],
          selection: TextSelection.collapsed(offset: digits[i].length),
        );
      }
    }
    if (_otp.focusRequest != _handledFocusRequest) {
      _handledFocusRequest = _otp.focusRequest;
      _focusNodes[_otp.activeIndex].requestFocus();
    }
  }

  void _onFocusChange(int index) {
    final node = _focusNodes[index];
    if (!node.hasFocus) return;
    _otp.activate(index);
    // Select the digit so typing replaces it.
    final field = _fields[index];
    field.selection = TextSelection(
      baseOffset: 0,
      extentOffset: field.text.length,
    );
  }

  KeyEventResult _onKey(int index, KeyEvent event) {
    final isBackspace = event.logicalKey == LogicalKeyboardKey.backspace;
    if (isBackspace && event is! KeyUpEvent && _fields[index].text.isEmpty) {
      _otp.backspace(index);
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([_otp, ..._focusNodes]),
      builder: (context, _) {
        const gap = 10.0;
        final count = _otp.length;
        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < count; i++) ...[
              if (i > 0) const SizedBox(width: gap),
              Flexible(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 52),
                  child: AspectRatio(
                    aspectRatio: 1,
                    child: OtpDigitBox(
                      position: i + 1,
                      total: count,
                      field: _fields[i],
                      focusNode: _focusNodes[i],
                      autofocus: i == 0,
                      onChanged: (text) => _otp.input(i, text),
                    ),
                  ),
                ),
              ),
            ],
          ],
        );
      },
    );
  }
}

enum DigitState { filled, focused, empty }

/// One circular digit box.
class OtpDigitBox extends StatelessWidget {
  const OtpDigitBox({
    super.key,
    required this.position,
    required this.total,
    required this.field,
    required this.focusNode,
    required this.onChanged,
    this.autofocus = false,
  });

  final int position;
  final int total;
  final TextEditingController field;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;
  final bool autofocus;

  DigitState get state {
    if (focusNode.hasFocus) return DigitState.focused;
    return field.text.isEmpty ? DigitState.empty : DigitState.filled;
  }

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final (background, outline, textColor) = switch (state) {
      DigitState.filled => (
        colors.digitFilledBg,
        colors.digitFilledBg,
        colors.digitFilledText,
      ),
      DigitState.focused => (
        colors.digitFocusedBg,
        colors.digitFocusedOutline,
        colors.textPrimary,
      ),
      DigitState.empty => (
        colors.digitEmptyBg,
        colors.digitEmptyOutline,
        colors.textPrimary,
      ),
    };

    return Semantics(
      identifier: OtpTestIds.digit(position),
      label: OtpStrings.digitLabel(position, total),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: background,
          border: Border.all(
            color: outline,
            width: state == DigitState.focused ? 1.5 : 1,
          ),
        ),
        child: TextField(
          key: ValueKey(OtpTestIds.digit(position)),
          controller: field,
          focusNode: focusNode,
          autofocus: autofocus,
          onChanged: onChanged,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          autofillHints: position == 1
              ? const [AutofillHints.oneTimeCode]
              : null,
          textAlign: TextAlign.center,
          showCursor: false,
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: textColor,
          ),
          decoration: InputDecoration(
            border: InputBorder.none,
            isCollapsed: true,
            counterText: '',
            hintText: state == DigitState.empty ? OtpStrings.placeholder : null,
            hintStyle: TextStyle(fontSize: 20, color: colors.digitPlaceholder),
          ),
        ),
      ),
    );
  }
}
