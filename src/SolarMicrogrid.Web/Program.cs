using Microsoft.AspNetCore.Mvc.ViewFeatures;
using Microsoft.AspNetCore.Mvc.ViewFeatures.Infrastructure;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.Security;
using SolarMicrogrid.Web.Presentation;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers(options =>
{
    options.Filters.Add<PortalAntiforgeryFilter>();
    options.Filters.Add<ApiExceptionFilter>();
    options.Filters.Add<PortalNoticeFilter>();
});
builder.Services.AddAntiforgery();
builder.Services.AddDataProtection();
// Keep encrypted redirect notices without registering views or Razor services.
builder.Services.AddSingleton<TempDataSerializer, PortalNoticeSerializer>();
builder.Services.AddSingleton<ITempDataProvider, CookieTempDataProvider>();
builder.Services.AddSingleton<ITempDataDictionaryFactory, TempDataDictionaryFactory>();
builder.AddMicrogridApiClient();
builder.AddPortalAuthentication();

var app = builder.Build();

app.UseExceptionHandler("/Home/Error");
if (!app.Environment.IsDevelopment())
{
    app.UseHsts();
    app.UseHttpsRedirection();
}

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
