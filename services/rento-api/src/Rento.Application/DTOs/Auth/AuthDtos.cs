namespace Rento.Application.DTOs.Auth;

public class SendOtpRequest
{
    public string Phone { get; set; } = "";
}

public class SendOtpResult
{
    public string Phone { get; set; } = "";
    public int ExpiresIn { get; set; }
    public int RetryAfter { get; set; }
    public int AttemptsRemaining { get; set; }
}

public class VerifyOtpRequest
{
    public string Phone { get; set; } = "";
    public string Otp { get; set; } = "";
}

public class VerifyOtpResult
{
    public string AccessToken { get; set; } = "";
    public string RefreshToken { get; set; } = "";
    public string TokenType { get; set; } = "Bearer";
    public int ExpiresIn { get; set; }
    public VerifyOtpUser User { get; set; } = new();
}

public class VerifyOtpUser
{
    public Guid Id { get; set; }
    public string Phone { get; set; } = "";
    public string? FullName { get; set; }
    public string Role { get; set; } = "";
    public bool IsNewUser { get; set; }
}

public class RefreshTokenRequest
{
    public string RefreshToken { get; set; } = "";
}

public class TokenResult
{
    public string AccessToken { get; set; } = "";
    public string RefreshToken { get; set; } = "";
    public string TokenType { get; set; } = "Bearer";
    public int ExpiresIn { get; set; }
}
