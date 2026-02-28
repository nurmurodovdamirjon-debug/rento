namespace Rento.Core.Common;

/// <summary>
/// Thrown when a required backing service (e.g. Redis) is temporarily unavailable.
/// GlobalExceptionMiddleware maps this to HTTP 503 Service Unavailable.
/// </summary>
public class ServiceUnavailableException : Exception
{
    public ServiceUnavailableException(string message) : base(message) { }

    public ServiceUnavailableException(string message, Exception innerException)
        : base(message, innerException) { }
}
