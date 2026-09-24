using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication;

namespace SolarMicrogrid.Web.ApiClients;

public sealed class MicrogridApiClient(HttpClient http, IHttpContextAccessor accessor)
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter(allowIntegerValues: false) }
    };

    public Task<T> GetAsync<T>(string path, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Get, path, null, ct);

    public Task<T> PostAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Post, path, body, ct);

    public Task<T> PatchAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Patch, path, body, ct);

    public Task<T> PutAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Put, path, body, ct);

    public async Task LogoutAsync(CancellationToken ct) =>
        await SendAsync<object>(HttpMethod.Post, "auth/logout", null, ct);

    private async Task<T> SendAsync<T>(HttpMethod method, string path, object? body, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(method, path);
        var context = accessor.HttpContext;
        var token = context is null ? null : await context.GetTokenAsync("access_token");
        if (!string.IsNullOrEmpty(token))
        {
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        }
        if (body is not null)
        {
            request.Content = JsonContent.Create(body, options: Json);
        }

        try
        {
            using var response = await http.SendAsync(request, ct);
            if (!response.IsSuccessStatusCode)
            {
                var failure = await ReadFailureAsync(response, ct);
                throw failure;
            }
            if (response.StatusCode == HttpStatusCode.NoContent)
            {
                return default!;
            }

            return await response.Content.ReadFromJsonAsync<T>(Json, ct)
                ?? throw new ApiFailureException(502, "The account service returned an empty response.");
        }
        catch (HttpRequestException)
        {
            throw new ApiFailureException(503, "The account service is unavailable. Please try again shortly.");
        }
        catch (TaskCanceledException) when (!ct.IsCancellationRequested)
        {
            throw new ApiFailureException(503, "The account service took too long to respond. Please try again.");
        }
    }

    private static async Task<ApiFailureException> ReadFailureAsync(HttpResponseMessage response, CancellationToken ct)
    {
        ApiProblem? problem = null;
        try
        {
            problem = await response.Content.ReadFromJsonAsync<ApiProblem>(Json, ct);
        }
        catch (JsonException)
        {
            // A proxy/server error may contain HTML instead of the API's JSON error body.
        }

        return new ApiFailureException((int)response.StatusCode,
            problem?.Detail ?? problem?.Title ?? "The account service could not complete this request.",
            problem?.Errors);
    }

    private sealed record ApiProblem(string? Title, string? Detail, Dictionary<string, string[]>? Errors);
}
