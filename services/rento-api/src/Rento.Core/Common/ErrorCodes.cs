namespace Rento.Core.Common;

public static class ErrorCodes
{
    public const string AuthRequired = "AUTH_REQUIRED";
    public const string AuthTokenInvalid = "AUTH_TOKEN_INVALID";
    public const string AuthTokenExpired = "AUTH_TOKEN_EXPIRED";
    public const string UserNotFound = "USER_NOT_FOUND";
    public const string UserBlocked = "USER_BLOCKED";
    public const string ListingNotFound = "LISTING_NOT_FOUND";
    public const string ListingNotOwner = "LISTING_NOT_OWNER";
    public const string InvalidId = "INVALID_ID";
    public const string ValidationError = "VALIDATION_ERROR";
    public const string NotFound = "NOT_FOUND";
    public const string RateLimitExceeded = "RATE_LIMIT_EXCEEDED";
    public const string InternalError = "INTERNAL_ERROR";
    public const string AdminRequired = "ADMIN_REQUIRED";
    public const string AlreadyReported = "ALREADY_REPORTED";
    public const string SelfReport = "SELF_REPORT";
    public const string ReportNotFound = "REPORT_NOT_FOUND";
    public const string InvalidStatusTransition = "INVALID_STATUS_TRANSITION";
    public const string FileRequired = "FILE_REQUIRED";
    public const string MediaTooLarge = "MEDIA_TOO_LARGE";
    public const string MediaInvalidType = "MEDIA_INVALID_TYPE";
    public const string InvalidBucket = "INVALID_BUCKET";
    public const string NoFields = "NO_FIELDS";
    public const string ChatRoomAccessDenied = "CHAT_ROOM_ACCESS_DENIED";
    public const string ServiceUnavailable = "SERVICE_UNAVAILABLE";
}
