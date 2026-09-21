namespace SolarMicrogrid.Api.Services;

public sealed class ApiException(int status, string code, string message) : Exception(message)
{
    public int Status { get; } = status;
    public string Code { get; } = code;
    public static ApiException Invalid(string message) => new(400, "validation_failed", message);
    public static ApiException NotFound() => new(404, "not_found", "The requested account or request was not found.");
    public static ApiException Conflict(string message) => new(409, "state_conflict", message);
    public static ApiException Forbidden(string message) => new(403, "forbidden", message);
    public static ApiException InvalidCredentials() => new(401, "invalid_credentials", "Invalid credentials or account unavailable.");
}
