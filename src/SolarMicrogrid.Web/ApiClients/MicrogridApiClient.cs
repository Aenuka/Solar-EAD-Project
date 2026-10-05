
/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Sends portal requests to the API with the session's JWT bearer token and supports account operations and logout.
 // File: MicrogridApiClient.cs
// Purpose: Sends authenticated portal requests to the microgrid API.
// Contributors: Shared web API client used by station and reservation features.
 */


using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ApiClients;

public sealed class MicrogridApiClient(HttpClient http, IHttpContextAccessor accessor)
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter(allowIntegerValues: false) }
    };

    // Retrieves a typed response from the API.
    public Task<T> GetAsync<T>(string path, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Get, path, null, ct);

    // Sends a typed POST request with a JSON body.
    public Task<T> PostAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Post, path, body, ct);

    // Sends a typed partial update to the API.
    public Task<T> PatchAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Patch, path, body, ct);

    // Sends PUT requests used by station schedule and availability updates. *****
    public Task<T> PutAsync<T>(string path, object body, CancellationToken ct) =>
        SendAsync<T>(HttpMethod.Put, path, body, ct);

    // Ends the current API session.
    public async Task LogoutAsync(CancellationToken ct) =>
        await SendAsync<object>(HttpMethod.Post, "auth/logout", null, ct);

    //Tharaka
    // ===== Reservation Methods (Sajith) =====

/// <summary>
/// Creates a new reservation via the API.
/// </summary>
public Task<ReservationResponse> CreateReservationAsync(ReservationInput input, CancellationToken ct)
    => PostAsync<ReservationResponse>("reservations", input, ct);

/// <summary>
/// Updates an existing reservation.
/// </summary>
public Task<ReservationResponse> UpdateReservationAsync(string id, UpdateReservationInput input, CancellationToken ct)
    => PutAsync<ReservationResponse>($"reservations/{id}", input, ct);

/// <summary>
/// Cancels an existing reservation.
/// </summary>
public Task<ReservationResponse> CancelReservationAsync(string id, CancelReservationInput input, CancellationToken ct)
    => PatchAsync<ReservationResponse>($"reservations/{id}/cancel", input, ct);

/// <summary>
/// Gets all reservations for a prosumer by NIC.
/// </summary>
public Task<List<ReservationResponse>> GetReservationHistoryAsync(string nic, CancellationToken ct)
    => GetAsync<List<ReservationResponse>>($"reservations/history?nic={Uri.EscapeDataString(nic)}", ct);

/// <summary>
/// Gets all pending reservations (for operator dashboard).
/// </summary>
public Task<List<ReservationResponse>> GetPendingReservationsAsync(CancellationToken ct)
    => GetAsync<List<ReservationResponse>>("reservations/pending", ct);

/// <summary>
/// Searches/filters reservations.
/// </summary>
public Task<List<ReservationResponse>> SearchReservationsAsync(string? status, string? stationId, string? nic, CancellationToken ct)
{
    var q = $"reservations/search?status={status}&stationId={stationId}&nic={nic}";
    return GetAsync<List<ReservationResponse>>(q, ct);
}

/// <summary>
/// Approves a pending reservation.
/// </summary>
public Task<ReservationResponse> ApproveReservationAsync(string id, CancellationToken ct)
    => PatchAsync<ReservationResponse>($"reservations/{id}/approve", new { }, ct);

/// <summary>
/// Gets the count of approved future reservations (for dashboard).
/// </summary>
public Task<ApprovedFutureCountResponse> GetApprovedFutureCountAsync(CancellationToken ct)
    => GetAsync<ApprovedFutureCountResponse>("reservations/approved-future/count", ct);

    // Adds authentication, sends the request, and reads its typed response.
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

    // Converts an unsuccessful HTTP response into an API failure.
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
