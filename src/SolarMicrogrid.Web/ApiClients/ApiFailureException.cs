namespace SolarMicrogrid.Web.ApiClients;

public sealed class ApiFailureException(int statusCode, string message, Dictionary<string, string[]>? errors = null)
    : Exception(message)
{
    public int StatusCode { get; } = statusCode;

    public Dictionary<string, string[]> Errors { get; } = errors ?? [];
}
