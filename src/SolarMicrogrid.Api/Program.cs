using System.Text.Json.Serialization;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarMicrogrid.Api.Configuration;
using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Endpoints;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Api.Services;

var builder = WebApplication.CreateBuilder(args);
builder.Configuration.AddJsonFile("appsettings.Local.json", optional: true).AddEnvironmentVariables();

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
builder.Services.AddValidation();
builder.AddMicrogridDatabase();
builder.AddApiAuthentication();

// Preserve the JSON rules used by the Android app and the staff portal.
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter(allowIntegerValues: false));
    options.SerializerOptions.UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow;
    options.SerializerOptions.NumberHandling = JsonNumberHandling.Strict;
});
builder.Services.Configure<RouteHandlerOptions>(options => options.ThrowOnBadRequest = true);
builder.Services.AddProblemDetails(options => options.CustomizeProblemDetails = context =>
    context.ProblemDetails.Extensions["traceId"] = context.HttpContext.TraceIdentifier);
builder.Services.AddExceptionHandler<ApiExceptionHandler>();
builder.Services.AddApiDocumentation();

builder.Services.AddSingleton<PasswordService>();
builder.Services.AddSingleton<TokenService>();
builder.Services.AddScoped<AuthService>();
builder.Services.AddScoped<StaffService>();
builder.Services.AddScoped<ProsumerService>();
builder.Services.AddScoped<DashboardService>();
builder.Services.AddScoped<StationService>();
builder.Services.AddScoped<IReservationRepository, ReservationRepository>();
builder.Services.AddScoped<ReservationService>();
builder.Services.AddScoped<MongoInitializer>();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer();
builder.Services.AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
    .Configure<IOptions<JwtOptions>>((options, settings) =>
    {
        var jwt = settings.Value;
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new()
        {
            ValidateIssuerSigningKey = true, IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt.SigningKey)),
            ValidateIssuer = true, ValidIssuer = jwt.Issuer, ValidateAudience = true, ValidAudience = jwt.Audience,
            ValidateLifetime = true, RequireExpirationTime = true, ClockSkew = TimeSpan.FromSeconds(15),
            ValidAlgorithms = [SecurityAlgorithms.HmacSha256], NameClaimType = "name", RoleClaimType = "role"
        };
        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = async context =>
            {
                var principal = context.Principal!;
                var id = principal.FindFirst("sub")?.Value;
                var role = principal.FindFirst("role")?.Value;
                var version = principal.FindFirst("sv")?.Value;
                if (id is null || role is not (Roles.Backoffice or Roles.GridOperator or Roles.Prosumer) || !long.TryParse(version, out var securityVersion))
                { context.Fail("Invalid session."); return; }
                var account = await context.HttpContext.RequestServices.GetRequiredService<AuthService>()
                    .FindAccountAsync(id, role, context.HttpContext.RequestAborted);
                if (account is null || account.Status != AccountStatus.Active || account.Role != role || account.SecurityVersion != securityVersion)
                    context.Fail("Session expired or revoked.");
            }
        };
    });
builder.Services.AddAuthorization(options => options.FallbackPolicy = new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build());
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = 429;
    options.AddPolicy("auth", context => RateLimitPartition.GetFixedWindowLimiter(
        context.Connection.RemoteIpAddress?.ToString() ?? "unknown",
        _ => new FixedWindowRateLimiterOptions
        {
            PermitLimit = builder.Configuration.GetValue("RateLimiting:AuthPermitLimit", 30),
            Window = TimeSpan.FromMinutes(1), QueueLimit = 0
        }));
    options.OnRejected = async (context, ct) =>
    {
        context.HttpContext.Response.Headers.RetryAfter = "60";
        await Results.Problem(statusCode: 429, title: "Too many attempts", detail: "Please wait a minute before trying again.")
            .ExecuteAsync(context.HttpContext);
    };
});

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages(async context =>
    await Results.Problem(statusCode: context.HttpContext.Response.StatusCode).ExecuteAsync(context.HttpContext));

if (!app.Environment.IsDevelopment())
{
    app.UseHsts();
    app.UseHttpsRedirection();
}

app.Use(async (context, next) =>
{
    context.Response.Headers.CacheControl = "no-store";
    context.Response.Headers["X-Content-Type-Options"] = "nosniff";
    await next();
});
app.UseRouting();
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();

app.MapAuthEndpoints();
app.MapStaffEndpoints();
app.MapProsumersEndpoints();
app.MapDashboardEndpoints();
app.MapStationsEndpoints();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi().AllowAnonymous();
}

app.MapGet("/health", async (IMongoDatabase database, CancellationToken ct) =>
{
    await database.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1), cancellationToken: ct);
    return Results.Ok(new { status = "healthy" });
}).AllowAnonymous();

await app.InitializeDatabaseAsync();
app.Run();
