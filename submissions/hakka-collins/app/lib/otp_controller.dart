import 'package:flutter/widgets.dart';

/// Owns the digits and focus of an [OtpInput]; the single source of truth for the code.
class OtpController extends ChangeNotifier {
  final int length;
  late final List<TextEditingController> fields = List.generate(
    length,
    (_) => TextEditingController(),
  );
  late final List<FocusNode> focusNodes = List.generate(
    length,
    (_) => FocusNode(),
  );

  OtpController({this.length = 6}) {
    for (final node in focusNodes) {
      node.addListener(notifyListeners);
    }
    for (final field in fields) {
      field.addListener(() => _keepCursorAtEnd(field));
    }
  }

  String get code => fields.map((f) => f.text).join();

  bool get isComplete => fields.every((f) => f.text.isNotEmpty);

  void focus(int index) => focusNodes[index].requestFocus();

  /// [digits] is what the user typed or pasted into box [index], already digits-only.
  void handleInput(int index, String digits) {
    if (digits.length > 1) {
      _fillFrom(index, digits);
    } else if (digits.isNotEmpty) {
      _advanceTo(index + 1);
    }
    notifyListeners();
  }

  void handleBackspaceOnEmpty(int index) {
    if (index == 0) return;
    fields[index - 1].clear();
    focus(index - 1);
    notifyListeners();
  }

  void clear() {
    for (final field in fields) {
      field.clear();
    }
    focus(0);
    notifyListeners();
  }

  void _fillFrom(int start, String digits) {
    var index = start;
    for (final digit in digits.split('')) {
      if (index == length) break;
      fields[index++].text = digit;
    }
    _advanceTo(index);
  }

  // A complete code releases focus so every box shows as filled and the keyboard closes.
  void _advanceTo(int index) {
    if (isComplete) {
      for (final node in focusNodes) {
        node.unfocus();
      }
    } else {
      focus(index < length ? index : fields.indexWhere((f) => f.text.isEmpty));
    }
  }

  // Keeps backspace deleting the digit and new typing replacing it, wherever the box was tapped.
  void _keepCursorAtEnd(TextEditingController field) {
    final end = TextSelection.collapsed(offset: field.text.length);
    if (field.selection != end) field.selection = end;
  }

  @override
  void dispose() {
    for (final field in fields) {
      field.dispose();
    }
    for (final node in focusNodes) {
      node.dispose();
    }
    super.dispose();
  }
}
