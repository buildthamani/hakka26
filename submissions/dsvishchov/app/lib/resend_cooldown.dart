import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/foundation.dart';

class ResendCooldown extends ChangeNotifier {
  ResendCooldown({DateTime Function()? now}) : _now = now ?? DateTime.now {
    restart();
  }
  final DateTime Function() _now;
  late DateTime _deadline;
  Timer? _timer;
  int _seconds = 30;
  int get seconds => _seconds;
  bool get canResend => _seconds == 0;

  void restart() {
    _timer?.cancel();
    _deadline = _now().add(const Duration(seconds: 30));
    _seconds = 30;
    _timer = Timer.periodic(
      const Duration(milliseconds: 200),
      (_) => refresh(),
    );
    notifyListeners();
  }

  void refresh() {
    final seconds = math.max(
      0,
      (_deadline.difference(_now()).inMilliseconds / 1000).ceil(),
    );
    if (seconds != _seconds) {
      _seconds = seconds;
      notifyListeners();
    }
    if (_seconds == 0) _timer?.cancel();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
