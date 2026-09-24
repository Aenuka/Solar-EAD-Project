namespace SolarMicrogrid.Web.ApiClients;

public static class ApiClientExtensions
{
    // Reference: Julio Casal YouTube tutorials
    // https://www.youtube.com/@juliocasal
    public static void AddMicrogridApiClient(this WebApplicationBuilder builder)
    {
        var apiUrl = new Uri(builder.Configuration["Api:BaseUrl"] ?? "http://localhost:5080/api/v1/");
        if (!apiUrl.AbsoluteUri.EndsWith('/'))
        {
            throw new InvalidOperationException("Api:BaseUrl must end with '/'.");
        }
        if (!builder.Environment.IsDevelopment() && apiUrl.Scheme != "https")
        {
            throw new InvalidOperationException("Configure an HTTPS Api:BaseUrl outside Development.");
        }

        builder.Services.AddHttpContextAccessor();
        builder.Services.AddHttpClient<MicrogridApiClient>(client =>
        {
            client.BaseAddress = apiUrl;
            client.Timeout = TimeSpan.FromSeconds(15);
        });
        builder.Services.AddHttpClient("session-validation", client =>
        {
            client.BaseAddress = apiUrl;
            client.Timeout = TimeSpan.FromSeconds(10);
        });
    }
}
