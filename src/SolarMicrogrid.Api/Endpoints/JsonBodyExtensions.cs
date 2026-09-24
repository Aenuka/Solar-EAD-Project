namespace SolarMicrogrid.Api.Endpoints;

public static class JsonBodyExtensions
{
    public static RouteHandlerBuilder WithJsonBody<T>(this RouteHandlerBuilder endpoint) where T : notnull
    {
        // Match the route first so its authorization and rate limits run before body binding.
        // The JSON binder still rejects non-JSON bodies with 415. Without this, routing can
        // replace the endpoint with a 415 endpoint that loses the AllowAnonymous/role metadata.
        return endpoint.Accepts<T>("*/*");
    }
}
