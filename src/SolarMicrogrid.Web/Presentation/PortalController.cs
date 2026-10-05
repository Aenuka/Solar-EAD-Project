using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace SolarMicrogrid.Web.Presentation;

// HTTP form handlers expose data to React; there is no server-side view engine.
public abstract class PortalController : ControllerBase
{
    protected Dictionary<string, object?> PageMeta { get; } = new();

    protected ITempDataDictionary TempData => HttpContext.RequestServices
        .GetRequiredService<ITempDataDictionaryFactory>().GetTempData(HttpContext);

    protected ReactPageResult ReactPage(object? model = null, string? page = null) => new()
    {
        Model = model,
        Page = page,
        Meta = PageMeta
    };
}
