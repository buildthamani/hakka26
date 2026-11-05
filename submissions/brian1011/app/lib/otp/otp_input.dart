import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_colors.dart';
import 'otp_controller.dart';

/// Row of circular digit boxes bound to an [OtpController].
///
/// The controller owns the digits; this widget only owns the per-box text
/// controllers and focus nodes needed to drive the keyboard.
class OtpInput extends StatefulWidget {
  const OtpInput({super.key, required this.controller});

  final OtpController controller;

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  late final List<TextEditingController> _texts;
  late final List<FocusNode> _focusNodes;
  int _appliedFocusRequest = 0;

  OtpController get _otp => widget.controller;

  @override
  void initState() {
    super.initState();
    _texts = List.generate(_otp.length, (_) => TextEditingController());
    _focusNodes = List.generate(_otp.length, (i) {
      final node = FocusNode(
        debugLabel: 'otp_digit_${i + 1}',
        onKeyEvent: (_, event) => _onKey(i, event),
      );
      node.addListener(() => _onFocusChange(i));
      return node;
    });
    _appliedFocusRequest = _otp.focusRequest;
    _otp.addListener(_syncFromController);
  }

  @override
  void dispose() {
    _otp.removeListener(_syncFromController);
    for (final t in _texts) {
      t.dispose();
    }
    for (final f in _focusNodes) {
      f.dispose();
    }
    super.dispose();
  }

  KeyEventResult _onKey(int index, KeyEvent event) {
    if (event is! KeyDownEvent && event is! KeyRepeatEvent) {
      return KeyEventResult.ignored;
    }
    if (event.logicalKey == LogicalKeyboardKey.backspace && _otp.backspaceOnEmpty(index)) {
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  void _onFocusChange(int index) {
    if (_focusNodes[index].hasFocus) {
      _otp.focusChanged(index);
      _selectAll(index);
    }
    if (mounted) setState(() {});
  }

  /// Keeps the whole digit selected so typing replaces it and a paste
  /// replaces it with the full pasted text.
  void _selectAll(int index) {
    final t = _texts[index];
    t.selection = TextSelection(baseOffset: 0, extentOffset: t.text.length);
  }

  void _syncFromController() {
    final digits = _otp.digits;
    for (var i = 0; i < digits.length; i++) {
      if (_texts[i].text != digits[i]) {
        _texts[i].value = TextEditingValue(
          text: digits[i],
          selection: TextSelection(baseOffset: 0, extentOffset: digits[i].length),
        );
      }
    }
    if (_otp.focusRequest != _appliedFocusRequest) {
      _appliedFocusRequest = _otp.focusRequest;
      _focusNodes[_otp.focusIndex].requestFocus();
      _selectAll(_otp.focusIndex);
    }
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    const gap = 8.0;
    // Sized from the screen width (the sheet has 24 px side padding) rather
    // than a LayoutBuilder, which can't sit inside SliverFillRemaining.
    final available = MediaQuery.sizeOf(context).width - 48;
    final size = math.min(52.0, (available - gap * (_otp.length - 1)) / _otp.length);
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        for (var i = 0; i < _otp.length; i++) ...[
          if (i > 0) const SizedBox(width: gap),
          _DigitBox(
            index: i,
            total: _otp.length,
            size: size,
            controller: _texts[i],
            focusNode: _focusNodes[i],
            filled: _otp.digits[i].isNotEmpty,
            onChanged: (value) => _otp.input(i, value),
          ),
        ],
      ],
    );
  }
}

class _DigitBox extends StatelessWidget {
  const _DigitBox({
    required this.index,
    required this.total,
    required this.size,
    required this.controller,
    required this.focusNode,
    required this.filled,
    required this.onChanged,
  });

  final int index;
  final int total;
  final double size;
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool filled;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final focused = focusNode.hasFocus;

    final Color background;
    final Color border;
    final Color text;
    if (focused) {
      background = colors.digitFocusedBg;
      border = colors.digitFocusedOutline;
      text = colors.textPrimary;
    } else if (filled) {
      background = colors.digitFilledBg;
      border = colors.digitFilledBg;
      text = colors.digitFilledText;
    } else {
      background = colors.digitEmptyBg;
      border = colors.digitEmptyOutline;
      text = colors.textPrimary;
    }

    return Semantics(
      label: 'Digit ${index + 1} of $total',
      container: true,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: background,
          border: Border.all(color: border, width: focused ? 1.5 : 1),
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // Placeholder drawn outside the TextField so it isn't read out
            // as part of the "Digit N of 6" label.
            if (!filled)
              ExcludeSemantics(
                child: Text('–', style: TextStyle(fontSize: 18, color: colors.digitPlaceholder)),
              ),
            TextField(
              key: Key('otp_digit_${index + 1}'),
              controller: controller,
              focusNode: focusNode,
              autofocus: index == 0,
              keyboardType: TextInputType.number,
              textInputAction: TextInputAction.next,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              autofillHints: index == 0 ? const [AutofillHints.oneTimeCode] : null,
              showCursor: false,
              textAlign: TextAlign.center,
              textAlignVertical: TextAlignVertical.center,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500, color: text),
              decoration: const InputDecoration(isCollapsed: true, border: InputBorder.none),
              onTap: () => controller.selection =
                  TextSelection(baseOffset: 0, extentOffset: controller.text.length),
              onChanged: onChanged,
            ),
          ],
        ),
      ),
    );
  }
}
