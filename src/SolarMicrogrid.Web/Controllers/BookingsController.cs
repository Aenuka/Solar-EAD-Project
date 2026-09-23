/*
 * File: BookingsController.cs
 * Author: Sajith
 * Description: Web UI controller for viewing reservations (calls API).
 */

using Microsoft.AspNetCore.Mvc;
using System.Net.Http.Headers;
using System.Text.Json;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

public class BookingsController : Controller
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _config;

    public BookingsController(IHttpClientFactory httpClientFactory, IConfiguration config)
    {
        _httpClientFactory = httpClientFactory;
        _config = config;
    }

    // Helper: Get token from session/cookie (you'll need to coordinate with Aenuka)
    private string? GetToken() => HttpContext.Session.GetString("JwtToken");

    // GET: /Bookings
    public async Task<IActionResult> Index(string? status, string? stationId)
    {
        var client = _httpClientFactory.CreateClient();
        var apiUrl = _config["ApiBaseUrl"] ?? "http://localhost:5080";
        var token = GetToken();

        if (string.IsNullOrEmpty(token))
            return RedirectToAction("Login", "Account");

        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        // Call the API for search/filter
        var url = $"{apiUrl}/api/v1/reservations/search?status={status}&stationId={stationId}";
        var response = await client.GetAsync(url);

        if (!response.IsSuccessStatusCode)
        {
            ViewBag.Error = "Failed to load bookings.";
            return View(new List<ReservationViewModel>());
        }

        var json = await response.Content.ReadAsStringAsync();
        var doc = JsonDocument.Parse(json);
        var bookings = JsonSerializer.Deserialize<List<ReservationViewModel>>(
            doc.RootElement.GetProperty("data").GetRawText(),
            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

        return View(bookings ?? new List<ReservationViewModel>());
    }

    // GET: /Bookings/Pending
    public async Task<IActionResult> Pending()
    {
        var client = _httpClientFactory.CreateClient();
        var apiUrl = _config["ApiBaseUrl"] ?? "http://localhost:5080";
        var token = GetToken();

        if (string.IsNullOrEmpty(token))
            return RedirectToAction("Login", "Account");

        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        var response = await client.GetAsync($"{apiUrl}/api/v1/reservations/pending");
        var json = await response.Content.ReadAsStringAsync();
        var doc = JsonDocument.Parse(json);
        var bookings = JsonSerializer.Deserialize<List<ReservationViewModel>>(
            doc.RootElement.GetProperty("data").GetRawText(),
            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

        return View(bookings ?? new List<ReservationViewModel>());
    }
}