/// Checks a one-time code. Swap in a backend-backed implementation in a real app.
abstract interface class OtpVerifier {
  Future<bool> verify(String code);
}

/// Local stand-in for the backend: accepts a single known code.
class FixedCodeOtpVerifier implements OtpVerifier {
  const FixedCodeOtpVerifier(this.expectedCode);

  final String expectedCode;

  @override
  Future<bool> verify(String code) async => code == expectedCode;
}
