abstract interface class OtpService {
  Future<bool> verify(String code);

  Future<void> resendCode();
}

/// Stand-in until a backend exists: accepts only [validCode] and never sends a real SMS.
class MockOtpService implements OtpService {
  static const validCode = '123456';

  final Duration latency;

  const MockOtpService({this.latency = const Duration(milliseconds: 600)});

  @override
  Future<bool> verify(String code) async {
    await Future<void>.delayed(latency);
    return code == validCode;
  }

  @override
  Future<void> resendCode() => Future<void>.delayed(latency);
}
