using Microsoft.AspNetCore.Diagnostics;
using MongoDB.Driver;
using SolarMicrogrid.Api.Services;

namespace SolarMicrogrid.Api.Configuration;

public sealed class ApiExceptionHandler(IProblemDetailsService problems, ILogger<ApiExceptionHandler> logger) : IExceptionHandler
{
    public async ValueTask<bool> TryHandleAsync(HttpContext context, Exception exception, CancellationToken cancellationToken)
    {
        var (status, code, detail) = exception switch
        {
            ApiException e => (e.Status, e.Code, e.Message),
            MongoException or TimeoutException => (503, "service_unavailable", "Account services are temporarily unavailable. Please try again."),
            _ => (500, "unexpected_error", "An unexpected error occurred. Please try again.")
        };
        if (status >= 500)
            logger.LogError("Request failed with {ExceptionType}. Trace: {TraceId}", exception.GetType().Name, context.TraceIdentifier);
        context.Response.StatusCode = status;
        await problems.WriteAsync(new()
        {
            HttpContext = context,
            ProblemDetails = new() { Status = status, Title = code, Detail = detail, Extensions = { ["code"] = code } }
        });
        return true;
    }
}
