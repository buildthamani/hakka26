import 'dart:async';
import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import 'otp_backdrop.dart';
import 'otp_input.dart';
import 'otp_sheet.dart';
import 'palette.dart';

/// OTP verification screen shown as a rounded bottom sheet.
class OtpScreen extends StatefulWidget {
  const OtpScreen({
    super.key,
    required this.palettes,
    this.validCode = '123456',
    this.countdownSeconds = 30,
  });

  final OtpPalettes palettes;
  final String validCode;
  final int countdownSeconds;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  String _digits = '';
  String? _status;
  bool _isError = false;
  late int _secondsLeft;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _secondsLeft = widget.countdownSeconds;
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _focusNode.requestFocus();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _tick(Timer timer) {
    if (!mounted) {
      timer.cancel();
      return;
    }
    setState(() {
      if (_secondsLeft > 0) _secondsLeft--;
      if (_secondsLeft == 0) {
        timer.cancel();
        _timer = null;
      }
    });
  }

  void _restartCountdown() {
    _timer?.cancel();
    setState(() => _secondsLeft = widget.countdownSeconds);
    _timer = Timer.periodic(const Duration(seconds: 1), _tick);
  }

  void _onInputChanged(String value) {
    final digits = value.replaceAll(RegExp(r'\D'), '');
    setState(() {
      _digits = digits;
      _status = null;
      _isError = false;
    });
  }

  void _submit() {
    if (_digits.length != OtpInput.length) return;

    if (_digits == widget.validCode) {
      _focusNode.unfocus();
      setState(() {
        _status = "You're in.";
        _isError = false;
      });
      return;
    }

    _controller.clear();
    setState(() {
      _digits = '';
      _status = "That code didn't work. Try again.";
      _isError = true;
    });
    _focusNode.requestFocus();
    _controller.selection = const TextSelection.collapsed(offset: 0);
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final colors = isDark ? widget.palettes.dark : widget.palettes.light;
    final logo = isDark
        ? 'assets/thamani-logo-white.png'
        : 'assets/thamani-logo.png';

    return Scaffold(
      backgroundColor: colors.sheet,
      body: LayoutBuilder(
        builder: (context, constraints) {
          return Stack(
            fit: StackFit.expand,
            children: [
              OtpBackdrop(brand: widget.palettes.brand),
              ClipRect(
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
                  child: ColoredBox(color: colors.scrim),
                ),
              ),
              Align(
                alignment: Alignment.bottomCenter,
                child: OtpSheet(
                  height: constraints.maxHeight * 0.9,
                  colors: colors,
                  logoAsset: logo,
                  digits: _digits,
                  controller: _controller,
                  focusNode: _focusNode,
                  onChanged: _onInputChanged,
                  status: _status,
                  isError: _isError,
                  secondsLeft: _secondsLeft,
                  onResend: _secondsLeft == 0 ? _restartCountdown : null,
                  onSubmit:
                      _digits.length == OtpInput.length ? _submit : null,
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
