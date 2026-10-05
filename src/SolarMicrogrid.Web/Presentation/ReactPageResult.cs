using System.Security.Claims;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace SolarMicrogrid.Web.Presentation;

// Sends the Vite HTML entry point plus escaped data, or JSON for React requests.
public sealed class ReactPageResult : ActionResult
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        Converters = { new JsonStringEnumConverter() }
    };

    public object? Model { get; init; }
    public string? Page { get; init; }
    public IReadOnlyDictionary<string, object?> Meta { get; init; } = new Dictionary<string, object?>();
    public int? StatusCode { get; init; }

    public override async Task ExecuteResultAsync(ActionContext context)
    {
        var http = context.HttpContext;
        var services = http.RequestServices;
        var tempData = services.GetRequiredService<ITempDataDictionaryFactory>().GetTempData(http);
        var antiforgery = services.GetRequiredService<IAntiforgery>();
        var environment = services.GetRequiredService<IWebHostEnvironment>();
        var data = new
        {
            controller = context.RouteData.Values["controller"]?.ToString(),
            page = Page ?? context.RouteData.Values["action"]?.ToString() ?? "Index",
            model = Model,
            meta = Meta,
            errors = context.ModelState.Where(item => item.Value!.Errors.Count > 0)
                .ToDictionary(item => item.Key, item => item.Value!.Errors.Select(error =>
                    string.IsNullOrEmpty(error.ErrorMessage) ? "Check the entered value." : error.ErrorMessage)),
            notices = new { success = tempData["Success"], error = tempData["Error"], info = tempData["LogoutNotice"] },
            user = http.User.Identity?.IsAuthenticated == true ? new
            {
                id = http.User.FindFirstValue(ClaimTypes.NameIdentifier),
                name = http.User.Identity.Name,
                role = http.User.FindFirstValue(ClaimTypes.Role)
            } : null,
            csrfToken = antiforgery.GetAndStoreTokens(http).RequestToken
        };
        var json = JsonSerializer.Serialize(data, JsonOptions);
        var wantsJson = http.Request.Headers.Accept.Any(value => value?.Contains("application/json") == true);
        var content = wantsJson ? json : (await File.ReadAllTextAsync(
            Path.Combine(environment.WebRootPath, "app", "index.html"), http.RequestAborted))
            .Replace("<!--PORTAL_DATA-->", $"<script id=\"portal-data\" type=\"application/json\">{json}</script>");

        // Consume flash notices before writing the response headers.
        tempData.Save();
        await new ContentResult
        {
            Content = content,
            ContentType = wantsJson ? "application/json; charset=utf-8" : "text/html; charset=utf-8",
            StatusCode = StatusCode ?? http.Response.StatusCode
        }.ExecuteResultAsync(context);
    }
}
