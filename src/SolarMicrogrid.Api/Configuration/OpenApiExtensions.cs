using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi;

namespace SolarMicrogrid.Api.Configuration;

public static class OpenApiExtensions
{
    public static void AddApiDocumentation(this IServiceCollection services)
    {
        services.AddOpenApi(options =>
        {
            options.AddDocumentTransformer((document, context, ct) =>
            {
                document.Info.Title = "Solar Microgrid — Component 1";
                document.Info.Version = "v1";
                document.Servers = [new OpenApiServer { Url = "/" }];
                document.Components ??= new();
                document.Components.SecuritySchemes ??= new Dictionary<string, IOpenApiSecurityScheme>();
                document.Components.SecuritySchemes["Bearer"] = new OpenApiSecurityScheme
                {
                    Type = SecuritySchemeType.Http,
                    Scheme = "bearer",
                    BearerFormat = "JWT",
                    Description = "Access token from a staff or prosumer login."
                };
                return Task.CompletedTask;
            });
            options.AddOperationTransformer((operation, context, ct) =>
            {
                // WithJsonBody lets authorization run first; the body still only accepts JSON.
                if (operation.RequestBody?.Content?.Remove("*/*", out var jsonBody) == true)
                {
                    operation.RequestBody.Content["application/json"] = jsonBody;
                }

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
    }
}
