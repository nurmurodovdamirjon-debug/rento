using FluentValidation;
using Rento.Application.DTOs.Auth;

namespace Rento.Application.Validators;

internal static class AuthValidationRules
{
    public const string UzbekistanPhonePattern = @"^\+998\d{9}$";
    public const string UzbekistanPhoneMessage = "Phone must be 13 characters: +998XXXXXXXXX";
}

public class SendOtpRequestValidator : AbstractValidator<SendOtpRequest>
{
    public SendOtpRequestValidator()
    {
        RuleFor(x => x.Phone)
            .Matches(AuthValidationRules.UzbekistanPhonePattern)
            .WithMessage(AuthValidationRules.UzbekistanPhoneMessage);
    }
}

public class VerifyOtpRequestValidator : AbstractValidator<VerifyOtpRequest>
{
    public VerifyOtpRequestValidator()
    {
        RuleFor(x => x.Phone)
            .Matches(AuthValidationRules.UzbekistanPhonePattern)
            .WithMessage(AuthValidationRules.UzbekistanPhoneMessage);
        RuleFor(x => x.Otp)
            .Length(6)
            .Matches(@"^\d{6}$")
            .WithMessage("OTP must be 6 digits");
    }
}

public class RefreshTokenRequestValidator : AbstractValidator<RefreshTokenRequest>
{
    public RefreshTokenRequestValidator()
    {
        RuleFor(x => x.RefreshToken).NotEmpty().WithMessage("Refresh token is required");
    }
}
