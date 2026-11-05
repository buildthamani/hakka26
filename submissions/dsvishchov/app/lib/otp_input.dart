import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'otp_controller.dart';
import 'otp_palette.dart';

class OtpInput extends StatefulWidget {
  const OtpInput({
    super.key,
    required this.controller,
    required this.palette,
    required this.availableWidth,
  });
  final OtpController controller;
  final OtpPalette palette;
  final double availableWidth;
  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> {
  // Mobile IMEs need not send physical Backspace events. An invisible sentinel
  // makes deletion from an empty field observable through editing updates.
  // It is never part of the OTP, its painted value, or accessibility semantics.
  static const _sentinel = '\u200B';
  final _fields = List.generate(
    6,
    (_) => TextEditingController(text: _sentinel),
  );
  final _focus = List.generate(6, (_) => FocusNode());

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_sync);
    for (var index = 0; index < _focus.length; index++) {
      final current = index;
      _focus[index].addListener(() {
        if (_focus[current].hasFocus) widget.controller.select(current);
        if (mounted) setState(() {});
      });
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _activate(0);
    });
  }

  void _sync() {
    for (var i = 0; i < _fields.length; i++) {
      final text = '$_sentinel${widget.controller.digitAt(i)}';
      _fields[i].value = TextEditingValue(
        text: text,
        selection: TextSelection(baseOffset: 1, extentOffset: text.length),
      );
    }
    _focus[widget.controller.activeIndex].requestFocus();
    if (mounted) setState(() {});
  }

  void _edit(int index, String raw) {
    if (raw.isEmpty || raw == _sentinel) {
      widget.controller.backspace(index);
    } else if (RegExp(r'[0-9]').hasMatch(raw)) {
      widget.controller.enter(index, raw);
    }
    _sync();
  }

  void _activate(int index) {
    widget.controller.select(index);
    _focus[index].requestFocus();
    _sync();
    SystemChannels.textInput.invokeMethod<void>('TextInput.show');
  }

  @override
  void dispose() {
    widget.controller.removeListener(_sync);
    for (final field in _fields) {
      field.dispose();
    }
    for (final node in _focus) {
      node.dispose();
    }
    super.dispose();
  }

  Widget _digit(int index, double slotWidth, double diameter) {
    final digit = widget.controller.digitAt(index);
    final filled = digit.isNotEmpty;
    final focused = _focus[index].hasFocus;
    final palette = widget.palette;
    return Semantics(
      key: ValueKey('otp_digit_${index + 1}'),
      identifier: 'otp_digit_${index + 1}',
      container: true,
      excludeSemantics: true,
      textField: true,
      focusable: true,
      focused: focused,
      label: 'Digit ${index + 1} of 6',
      value: digit,
      maxValueLength: 1,
      currentValueLength: digit.length,
      onTap: () => _activate(index),
      onSetText: (text) {
        _activate(index);
        _edit(index, text);
      },
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: () => _activate(index),
        child: SizedBox(
          width: slotWidth,
          height: math.max(48, diameter + 4),
          child: Center(
            child: Container(
              width: diameter,
              height: diameter,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color:
                    palette[filled
                        ? 'digit_filled_bg'
                        : focused
                        ? 'digit_focused_bg'
                        : 'digit_empty_bg'],
                border: Border.all(
                  width: focused ? 1.5 : 1,
                  color:
                      palette[filled
                          ? 'digit_filled_bg'
                          : focused
                          ? 'digit_focused_outline'
                          : 'digit_empty_outline'],
                ),
              ),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  Text(
                    digit.isEmpty ? '–' : digit,
                    style: TextStyle(
                      fontSize: 18,
                      color:
                          palette[filled
                              ? 'digit_filled_text'
                              : 'digit_placeholder'],
                    ),
                  ),
                  Focus(
                    onKeyEvent: (_, event) {
                      if (event is KeyDownEvent &&
                          event.logicalKey == LogicalKeyboardKey.backspace) {
                        widget.controller.backspace(index);
                        return KeyEventResult.handled;
                      }
                      return KeyEventResult.ignored;
                    },
                    child: TextSelectionTheme(
                      data: const TextSelectionThemeData(
                        cursorColor: Colors.transparent,
                        selectionColor: Colors.transparent,
                        selectionHandleColor: Colors.transparent,
                      ),
                      child: TextField(
                        key: ValueKey('otp_input_${index + 1}'),
                        controller: _fields[index],
                        focusNode: _focus[index],
                        autofocus: index == 0,
                        keyboardType: TextInputType.number,
                        textInputAction: index == 5
                            ? TextInputAction.done
                            : TextInputAction.next,
                        autofillHints: const [AutofillHints.oneTimeCode],
                        enableSuggestions: false,
                        autocorrect: false,
                        smartDashesType: SmartDashesType.disabled,
                        smartQuotesType: SmartQuotesType.disabled,
                        showCursor: false,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: Colors.transparent,
                          fontSize: 18,
                        ),
                        decoration: const InputDecoration(
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          isCollapsed: true,
                          contentPadding: EdgeInsets.zero,
                        ),
                        onTap: () => _activate(index),
                        onChanged: (raw) => _edit(index, raw),
                        onSubmitted: (_) {
                          if (index < 5) _activate(index + 1);
                        },
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scale = MediaQuery.textScalerOf(context).scale(18) / 18;
    final minSlot = math.max(48.0, 24 * scale + 20);
    final columns = widget.availableWidth >= minSlot * 6 ? 6 : 3;
    final slot = widget.availableWidth / columns;
    final diameter = math.min(slot - 4, math.max(48.0, 24 * scale + 16));
    return Wrap(
      alignment: WrapAlignment.center,
      runSpacing: 8,
      children: List.generate(6, (i) => _digit(i, slot, diameter)),
    );
  }
}
