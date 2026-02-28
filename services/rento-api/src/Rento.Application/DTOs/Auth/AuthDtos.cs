namespace Rento.Application.DTOs.Auth;

public class SendOtpRequest
{
    public required string Phone { get; init; }
}

public class SendOtpResult
{
    public required string Phone { get; init; }
    public int ExpiresIn { get; init; }
    public int RetryAfter { get; init; }
    public int AttemptsRemaining { get; init; }
}

public class VerifyOtpRequest
{
    public required string Phone { get; init; }
    public required string Otp { get; init; }
}

public class VerifyOtpResult
{
    public required string AccessToken { get; init; }
    public required string RefreshToken { get; init; }
    public string TokenType { get; init; } = "Bearer";
    public int ExpiresIn { get; init; }
    public required VerifyOtpUser User { get; init; }
}

public class VerifyOtpUser
{
    public Guid Id { get; init; }
    public required string Phone { get; init; }
    public string? FullName { get; init; }
    public required string Role { get; init; }
    public bool IsNewUser { get; init; }
}

public class RefreshTokenRequest
{
    public required string RefreshToken { get; init; }
}

public class TokenResult
{
    public required string AccessToken { get; init; }
    public required string RefreshToken { get; init; }
    public string TokenType { get; init; } = "Bearer";
    public int ExpiresIn { get; init; }
}
