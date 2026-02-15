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

export class ChatRoomNotFoundError extends AppError {
  constructor(message = 'Chat xonasi topilmadi') {
    super(404, 'CHAT_ROOM_NOT_FOUND', message);
  }
}

export class ChatSelfMessageError extends AppError {
  constructor(message = "O'zingizga xabar yubora olmaysiz") {
    super(400, 'CHAT_SELF_MESSAGE', message);
  }
}

export class ChatUnauthorizedError extends AppError {
  constructor(message = 'Bu chat xonasiga kirishga ruxsat yo\'q') {
    super(403, 'CHAT_UNAUTHORIZED', message);
  }
}

export class AuthTokenInvalidError extends AppError {
  constructor(message = 'Token noto\'g\'ri') {
    super(401, 'AUTH_TOKEN_INVALID', message);
  }
}

export class AuthTokenExpiredError extends AppError {
  constructor(message = 'Token muddati tugagan') {
    super(401, 'AUTH_TOKEN_EXPIRED', message);
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
  constructor(message = 'Xabar yuborish limiti oshib ketdi (30/min)') {
    super(429, 'RATE_LIMIT_EXCEEDED', message);
  }
}
