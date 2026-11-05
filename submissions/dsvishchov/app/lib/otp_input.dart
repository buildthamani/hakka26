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
  final _focus = FocusNode();

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_changed);
    _focus.addListener(_focusChanged);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _activate(0);
    });
  }

  void _changed() {
    _focus.requestFocus();
    if (mounted) setState(() {});
  }

  void _focusChanged() {
    if (mounted) setState(() {});
  }

  void _activate(int index) {
    widget.controller.select(index);
    _focus.requestFocus();
    SystemChannels.textInput.invokeMethod<void>('TextInput.show');
  }

  @override
  void dispose() {
    widget.controller.removeListener(_changed);
    _focus.removeListener(_focusChanged);
    _focus.dispose();
    super.dispose();
  }

  Widget _digit(int index, double slotWidth, double diameter) {
    final digit = widget.controller.digitAt(index);
    final filled = digit.isNotEmpty;
    final focused = _focus.hasFocus && widget.controller.activeIndex == index;
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
        widget.controller.enter(index, text);
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
              child: Center(
                child: Text(
                  digit.isEmpty ? '–' : digit,
                  style: TextStyle(
                    fontSize: 18,
                    color:
                        palette[filled
                            ? 'digit_filled_text'
                            : 'digit_placeholder'],
                  ),
                ),
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
    return Stack(
      children: [
        Positioned(
          left: 0,
          top: 0,
          width: 1,
          height: 1,
          child: ExcludeSemantics(
            child: Opacity(
              opacity: 0,
              child: TextField(
                key: const ValueKey('otp_input'),
                controller: widget.controller,
                focusNode: _focus,
                autofocus: true,
                keyboardType: TextInputType.number,
                textInputAction: TextInputAction.done,
                autofillHints: const [AutofillHints.oneTimeCode],
                inputFormatters: [
                  TextInputFormatter.withFunction(OtpController.formatEdit),
                ],
                enableSuggestions: false,
                autocorrect: false,
                showCursor: false,
                decoration: const InputDecoration(
                  border: InputBorder.none,
                  isCollapsed: true,
                ),
              ),
            ),
          ),
        ),
        Wrap(
          alignment: WrapAlignment.center,
          runSpacing: 8,
          children: List.generate(6, (i) => _digit(i, slot, diameter)),
        ),
      ],
    );
  }
}
