using System.Net;
using System.Net.Http.Headers;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Web.ApiClients;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddControllersWithViews(options =>
{
    options.Filters.Add(new AutoValidateAntiforgeryTokenAttribute());
    options.Filters.Add<ApiExceptionFilter>();
});
builder.Services.AddHttpContextAccessor();
var apiUrl = new Uri(builder.Configuration["Api:BaseUrl"] ?? "http://localhost:5080/api/v1/");
if (!apiUrl.AbsoluteUri.EndsWith('/')) throw new InvalidOperationException("Api:BaseUrl must end with '/'.");
if (!builder.Environment.IsDevelopment() && apiUrl.Scheme != "https")
    throw new InvalidOperationException("Configure an HTTPS Api:BaseUrl outside Development.");
builder.Services.AddHttpClient<MicrogridApiClient>(client => { client.BaseAddress = apiUrl; client.Timeout = TimeSpan.FromSeconds(15); });
builder.Services.AddHttpClient("session-validation", client => { client.BaseAddress = apiUrl; client.Timeout = TimeSpan.FromSeconds(10); });
builder.Services.AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme).AddCookie(options =>
{
    options.Cookie.Name = "SolarMicrogrid.Session";
    options.Cookie.HttpOnly = true;
    options.Cookie.SameSite = SameSiteMode.Lax;
    options.Cookie.SecurePolicy = builder.Environment.IsDevelopment() ? CookieSecurePolicy.SameAsRequest : CookieSecurePolicy.Always;
    options.LoginPath = "/Account/Login";
    options.AccessDeniedPath = "/Account/AccessDenied";
    options.SlidingExpiration = false;
    options.Events.OnValidatePrincipal = async context =>
    {
        if (context.Request.Path.StartsWithSegments("/Home/Error") || context.Request.Path.StartsWithSegments("/Account/Logout")) return;
        var token = context.Properties.GetTokenValue("access_token");
        using var request = new HttpRequestMessage(HttpMethod.Get, "auth/me");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        try
        {
            var client = context.HttpContext.RequestServices.GetRequiredService<IHttpClientFactory>().CreateClient("session-validation");
            using var response = await client.SendAsync(request, context.HttpContext.RequestAborted);
            if (response.StatusCode is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden)
            {
                context.RejectPrincipal();
                await context.HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            }
            else if (!response.IsSuccessStatusCode)
                throw new ApiFailureException(503, "The account service is temporarily unavailable.");
        }
        catch (HttpRequestException) { throw new ApiFailureException(503, "The account service is temporarily unavailable."); }
        catch (TaskCanceledException) when (!context.HttpContext.RequestAborted.IsCancellationRequested)
        { throw new ApiFailureException(503, "The account service is temporarily unavailable."); }
    };
});
builder.Services.AddAuthorization(options => options.FallbackPolicy = new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build());
var app = builder.Build();
app.UseExceptionHandler("/Home/Error");
if (!app.Environment.IsDevelopment()) { app.UseHsts(); app.UseHttpsRedirection(); }
app.Use(async (context, next) =>
{
    context.Response.Headers["X-Content-Type-Options"] = "nosniff";
    context.Response.Headers["Referrer-Policy"] = "no-referrer";
    context.Response.Headers["Content-Security-Policy"] = "default-src 'self'; style-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'; base-uri 'self'";
    context.Response.Headers.CacheControl = "no-store";
    await next();
});
app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllerRoute("default", "{controller=Home}/{action=Index}/{id?}");
app.Run();
