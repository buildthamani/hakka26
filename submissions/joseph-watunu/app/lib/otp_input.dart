import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';

import 'otp_colors.dart';

/// One native editing session handles IME backspace, selection and SMS autofill.
/// Six visible cells expose independent accessible editing actions.
class OtpInput extends StatefulWidget {
  const OtpInput({
    super.key,
    required this.controller,
    required this.focusNode,
    required this.palette,
    required this.onChanged,
    required this.onSubmitted,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final OtpPalette palette;
  final VoidCallback onChanged;
  final VoidCallback onSubmitted;

  @override
  State<OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<OtpInput> with WidgetsBindingObserver {
  static final _digits = RegExp(r'[0-9]');
  Timer? _initialKeyboardTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    widget.controller.addListener(_refresh);
    widget.focusNode.addListener(_refresh);
    // Android may reject autofocus before its native input connection is ready.
    // Retry briefly; stop on keyboard visibility, interaction or loss of focus.
    _initialKeyboardTimer = Timer.periodic(const Duration(milliseconds: 150), (
      timer,
    ) {
      if (!mounted ||
          !widget.focusNode.hasFocus ||
          timer.tick >= 10 ||
          View.of(context).viewInsets.bottom > 0) {
        timer.cancel();
        return;
      }
      if (WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed) {
        SystemChannels.textInput.invokeMethod<void>('TextInput.show');
      }
    });
  }

  @override
  void dispose() {
    _initialKeyboardTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    widget.controller.removeListener(_refresh);
    widget.focusNode.removeListener(_refresh);
    super.dispose();
  }

  void _refresh() => setState(() {});

  @override
  void didChangeMetrics() {
    if (mounted && View.of(context).viewInsets.bottom > 0) {
      _initialKeyboardTimer?.cancel();
    }
  }

  void _select(int index) {
    _initialKeyboardTimer?.cancel();
    final end = widget.controller.text.length;
    widget.focusNode.requestFocus();
    widget.controller.selection = index < end
        ? TextSelection(baseOffset: index, extentOffset: index + 1)
        : TextSelection.collapsed(offset: end);
    // Reopen a keyboard that the user previously dismissed.
    SystemChannels.textInput.invokeMethod<void>('TextInput.show');
  }

  void _setDigit(int index, String input) {
    final digits = _digits.allMatches(input).map((m) => m.group(0)!).join();
    _select(index);
    final old = widget.controller.value;
    final selection = old.selection;
    final candidate = old.text.replaceRange(
      selection.start,
      selection.end,
      digits,
    );
    widget.controller.value = OtpInputFormatter().formatEditUpdate(
      old,
      TextEditingValue(
        text: candidate,
        selection: TextSelection.collapsed(
          offset: selection.start + digits.length,
        ),
      ),
    );
    widget.onChanged();
  }

  Future<void> _paste() async {
    final clipboard = await Clipboard.getData(Clipboard.kTextPlain);
    if (!mounted || clipboard?.text == null) return;
    final digits = _digits
        .allMatches(clipboard!.text!)
        .map((m) => m.group(0)!)
        .join();
    if (digits.isEmpty) return;
    final code = digits.substring(0, math.min(6, digits.length));
    widget.controller.value = TextEditingValue(
      text: code,
      selection: TextSelection.collapsed(offset: code.length),
    );
    widget.focusNode.requestFocus();
    widget.onChanged();
  }

  Future<void> _showPasteMenu(Offset position) async {
    final overlay =
        Overlay.of(context).context.findRenderObject()! as RenderBox;
    final result = await showMenu<String>(
      context: context,
      position: RelativeRect.fromSize(
        Rect.fromLTWH(position.dx, position.dy, 0, 0),
        overlay.size,
      ),
      items: const [PopupMenuItem(value: 'paste', child: Text('Paste'))],
    );
    if (mounted && result == 'paste') await _paste();
  }

  @override
  Widget build(BuildContext context) {
    final code = widget.controller.text;
    final selection = widget.controller.selection;
    final active = selection.isValid ? math.min(selection.start, 5) : 0;
    final palette = widget.palette;

    return AutofillGroup(
      child: SizedBox(
        height: 64,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned.fill(
              child: ExcludeSemantics(
                child: IgnorePointer(
                  child: Opacity(
                    opacity: 0,
                    child: TextField(
                      key: const ValueKey('otp_input'),
                      controller: widget.controller,
                      focusNode: widget.focusNode,
                      autofocus: true,
                      keyboardType: TextInputType.number,
                      textInputAction: TextInputAction.done,
                      autofillHints: const [AutofillHints.oneTimeCode],
                      autocorrect: false,
                      enableSuggestions: false,
                      enableIMEPersonalizedLearning: false,
                      showCursor: false,
                      inputFormatters: [OtpInputFormatter()],
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                      ),
                      onChanged: (_) {
                        _initialKeyboardTimer?.cancel();
                        widget.onChanged();
                      },
                      onSubmitted: (_) => widget.onSubmitted(),
                    ),
                  ),
                ),
              ),
            ),
            LayoutBuilder(
              builder: (context, constraints) {
                final diameter = math.min(48.0, constraints.maxWidth / 6 - 4);
                return Row(
                  textDirection: TextDirection.ltr,
                  children: List.generate(6, (index) {
                    final filled = index < code.length;
                    final focused =
                        widget.focusNode.hasFocus && index == active;
                    final background = focused
                        ? palette['digit_focused_bg']
                        : palette[filled
                              ? 'digit_filled_bg'
                              : 'digit_empty_bg'];
                    final foreground = filled
                        ? palette[focused
                              ? 'text_primary'
                              : 'digit_filled_text']
                        : palette['digit_placeholder'];
                    return Expanded(
                      child: Semantics(
                        key: ValueKey('otp_digit_${index + 1}'),
                        identifier: 'otp_digit_${index + 1}',
                        label: 'Digit ${index + 1} of 6',
                        value: filled ? code[index] : '',
                        hint: 'Long press to paste a code',
                        textField: true,
                        focusable: true,
                        focused: focused,
                        sortKey: OrdinalSortKey(index.toDouble()),
                        onTap: () => _select(index),
                        onSetText: (text) => _setDigit(index, text),
                        onDidGainAccessibilityFocus: () => _select(index),
                        customSemanticsActions: {
                          const CustomSemanticsAction(label: 'Paste code'):
                              _paste,
                        },
                        child: ExcludeSemantics(
                          child: GestureDetector(
                            behavior: HitTestBehavior.opaque,
                            onTap: () => _select(index),
                            onLongPressStart: (details) =>
                                _showPasteMenu(details.globalPosition),
                            child: SizedBox(
                              height: 64,
                              child: Center(
                                child: Container(
                                  key: ValueKey('otp_circle_${index + 1}'),
                                  width: diameter,
                                  height: diameter,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: background,
                                    border: Border.all(
                                      width: focused ? 1.3 : 1,
                                      color: focused
                                          ? palette['digit_focused_outline']
                                          : filled
                                          ? background
                                          : palette['digit_empty_outline'],
                                    ),
                                  ),
                                  child: Center(
                                    child: Padding(
                                      padding: const EdgeInsets.all(4),
                                      child: FittedBox(
                                        fit: BoxFit.scaleDown,
                                        child: Text(
                                          filled ? code[index] : '–',
                                          style: TextStyle(
                                            fontSize: 16,
                                            color: foreground,
                                          ),
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

/// Full pasted SMS codes replace any existing digits. Short edits retain
/// native caret/selection behavior and only ASCII digits are accepted.
class OtpInputFormatter extends TextInputFormatter {
  final _digitsOnly = FilteringTextInputFormatter.digitsOnly;
  final _limit = LengthLimitingTextInputFormatter(6);

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    var prefix = 0;
    while (prefix < oldValue.text.length &&
        prefix < newValue.text.length &&
        oldValue.text[prefix] == newValue.text[prefix]) {
      prefix++;
    }
    var suffix = 0;
    while (suffix < oldValue.text.length - prefix &&
        suffix < newValue.text.length - prefix &&
        oldValue.text[oldValue.text.length - suffix - 1] ==
            newValue.text[newValue.text.length - suffix - 1]) {
      suffix++;
    }
    final inserted = newValue.text.substring(
      prefix,
      newValue.text.length - suffix,
    );
    final pastedDigits = inserted.replaceAll(RegExp(r'[^0-9]'), '');
    if (pastedDigits.length >= 6) {
      return TextEditingValue(
        text: pastedDigits.substring(0, 6),
        selection: const TextSelection.collapsed(offset: 6),
      );
    }
    return _limit.formatEditUpdate(
      oldValue,
      _digitsOnly.formatEditUpdate(oldValue, newValue),
    );
  }
}
