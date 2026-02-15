// ===== Custom Error Classes =====

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly code: string;
  public readonly details?: unknown;

  constructor(statusCode: number, code: string, message: string, details?: unknown) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

// ===== Auth Errors =====

export class AuthInvalidPhoneError extends AppError {
  constructor(message = 'Telefon raqam formati noto\'g\'ri') {
    super(400, 'AUTH_INVALID_PHONE', message);
  }
}

export class AuthOtpExpiredError extends AppError {
  constructor(message = 'OTP muddati tugagan') {
    super(400, 'AUTH_OTP_EXPIRED', message);
  }
}

export class AuthOtpInvalidError extends AppError {
  constructor(message = 'OTP kodi noto\'g\'ri') {
    super(400, 'AUTH_OTP_INVALID', message);
  }
}

export class AuthOtpLimitError extends AppError {
  constructor(message = 'SMS yuborish limiti tugagan. Keyinroq urinib ko\'ring') {
    super(429, 'AUTH_OTP_LIMIT', message);
  }
}

export class AuthTokenExpiredError extends AppError {
  constructor(message = 'Token muddati tugagan') {
    super(401, 'AUTH_TOKEN_EXPIRED', message);
  }
}

export class AuthTokenInvalidError extends AppError {
  constructor(message = 'Token noto\'g\'ri') {
    super(401, 'AUTH_TOKEN_INVALID', message);
  }
}

export class UserBlockedError extends AppError {
  constructor(message = 'Foydalanuvchi bloklangan') {
    super(403, 'USER_BLOCKED', message);
  }
}

export class ValidationError extends AppError {
  constructor(message: string, details?: unknown) {
    super(400, 'VALIDATION_ERROR', message, details);
  }
}

export class NotFoundError extends AppError {
  constructor(message = 'Topilmadi') {
    super(404, 'NOT_FOUND', message);
  }
}

export class RateLimitError extends AppError {
  constructor(message = 'So\'rovlar limiti oshib ketdi') {
    super(429, 'RATE_LIMIT_EXCEEDED', message);
  }
}
