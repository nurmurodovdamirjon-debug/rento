using FluentValidation;
using Rento.Application.DTOs.Auth;

namespace Rento.Application.Validators;

public class SendOtpRequestValidator : AbstractValidator<SendOtpRequest>
{
    public SendOtpRequestValidator()
    {
        RuleFor(x => x.Phone)
            .Matches(@"^\+998\d{9}$")
            .WithMessage("Phone must be 13 characters: +998XXXXXXXXX");
    }
}

public class VerifyOtpRequestValidator : AbstractValidator<VerifyOtpRequest>
{
    public VerifyOtpRequestValidator()
    {
        RuleFor(x => x.Phone)
            .Matches(@"^\+998\d{9}$")
            .WithMessage("Phone must be 13 characters: +998XXXXXXXXX");
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
