using System.Text;
using System.Text.Json.Serialization;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Conventions;
using MongoDB.Driver;
using SolarMicrogrid.Api.Configuration;
using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

var builder = WebApplication.CreateBuilder(args);
builder.Configuration.AddJsonFile("appsettings.Local.json", optional: true).AddEnvironmentVariables();
builder.Services.AddOptions<MongoOptions>().BindConfiguration("Mongo").ValidateDataAnnotations().ValidateOnStart();
builder.Services.AddOptions<JwtOptions>().BindConfiguration("Jwt").ValidateDataAnnotations().ValidateOnStart();
builder.Services.AddControllers().AddJsonOptions(options =>
{
    options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter(allowIntegerValues: false));
    options.JsonSerializerOptions.UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow;
    options.JsonSerializerOptions.NumberHandling = JsonNumberHandling.Strict;
});
// OpenAPI uses HTTP JSON options; keep these aligned with controller serialization.
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter(allowIntegerValues: false));
    options.SerializerOptions.UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow;
    options.SerializerOptions.NumberHandling = JsonNumberHandling.Strict;
});
builder.Services.AddOpenApi(options =>
{
    options.AddDocumentTransformer((document, context, ct) =>
    {
        document.Info.Title = "Solar Microgrid — Component 1";
        document.Info.Version = "v1";
        document.Servers = [new OpenApiServer { Url = "/" }];
        document.Components ??= new();
        document.Components.SecuritySchemes ??= new Dictionary<string, IOpenApiSecurityScheme>();
        document.Components.SecuritySchemes["Bearer"] = new OpenApiSecurityScheme
        { Type = SecuritySchemeType.Http, Scheme = "bearer", BearerFormat = "JWT", Description = "Access token from a staff or prosumer login." };
        return Task.CompletedTask;
    });
    options.AddOperationTransformer((operation, context, ct) =>
    {
        if (!context.Description.ActionDescriptor.EndpointMetadata.OfType<IAllowAnonymous>().Any())
        {
            operation.Security = [new OpenApiSecurityRequirement { [new OpenApiSecuritySchemeReference("Bearer", context.Document)] = [] }];
            operation.Responses ??= new();
            operation.Responses.TryAdd("401", new OpenApiResponse { Description = "Missing, invalid, expired or revoked session." });
            operation.Responses.TryAdd("403", new OpenApiResponse { Description = "Caller lacks the required role or permission." });
        }
        return Task.CompletedTask;
    });
    options.AddSchemaTransformer((schema, context, ct) =>
    {
        var type = Nullable.GetUnderlyingType(context.JsonTypeInfo.Type) ?? context.JsonTypeInfo.Type;
        if (type.IsEnum)
        {
            // Every JSON enum input is required; optional query filters use absence, not a JSON null.
            schema.Type = JsonSchemaType.String;
            schema.Enum = schema.Enum?.Where(value => value is not null).ToList();
        }
        return Task.CompletedTask;
    });
});
builder.Services.AddProblemDetails(options => options.CustomizeProblemDetails = context =>
    context.ProblemDetails.Extensions["traceId"] = context.HttpContext.TraceIdentifier);
builder.Services.AddExceptionHandler<ApiExceptionHandler>();

ConventionRegistry.Register("solar-camel-case", new ConventionPack { new CamelCaseElementNameConvention() },
    type => type.Namespace == "SolarMicrogrid.Api.Models");
builder.Services.AddSingleton<IMongoClient>(services =>
{
    var settings = MongoClientSettings.FromConnectionString(services.GetRequiredService<IOptions<MongoOptions>>().Value.ConnectionString);
    settings.ServerSelectionTimeout = TimeSpan.FromSeconds(5);
    return new MongoClient(settings);
});
builder.Services.AddSingleton(services => services.GetRequiredService<IMongoClient>()
    .GetDatabase(services.GetRequiredService<IOptions<MongoOptions>>().Value.DatabaseName));
builder.Services.AddScoped<IStaffRepository, MongoStaffRepository>();
builder.Services.AddScoped<IProsumerRepository, MongoProsumerRepository>();
builder.Services.AddSingleton<PasswordService>();
builder.Services.AddSingleton<TokenService>();
builder.Services.AddScoped<AuthService>();
builder.Services.AddScoped<StaffService>();
builder.Services.AddScoped<ProsumerService>();
builder.Services.AddScoped<DashboardService>();
builder.Services.AddScoped<IStationRepository, StationRepository>();
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
if (!app.Environment.IsDevelopment()) { app.UseHsts(); app.UseHttpsRedirection(); }
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
app.MapControllers();
if (app.Environment.IsDevelopment()) app.MapOpenApi().AllowAnonymous();
app.MapGet("/health", async (IMongoDatabase db, CancellationToken ct) =>
{
    await db.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1), cancellationToken: ct);
    return Results.Ok(new { status = "healthy" });
}).AllowAnonymous();
await using (var scope = app.Services.CreateAsyncScope())
    await scope.ServiceProvider.GetRequiredService<MongoInitializer>().InitializeAsync(CancellationToken.None);
app.Run();
