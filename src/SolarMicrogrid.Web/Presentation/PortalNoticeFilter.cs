using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace SolarMicrogrid.Web.Presentation;

public sealed class PortalNoticeFilter(ITempDataDictionaryFactory factory) : IAsyncAlwaysRunResultFilter
{
    public async Task OnResultExecutionAsync(ResultExecutingContext context, ResultExecutionDelegate next)
    {
        // ReactPageResult consumes notices itself; redirects preserve them for the next request.
        if (context.Result is not ReactPageResult)
        {
            factory.GetTempData(context.HttpContext).Save();
        }
        await next();
    }
}
