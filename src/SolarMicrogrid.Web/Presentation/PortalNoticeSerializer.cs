using System.Text.Json;
using Microsoft.AspNetCore.Mvc.ViewFeatures.Infrastructure;

namespace SolarMicrogrid.Web.Presentation;

// Only short text notices are stored in the standard encrypted TempData cookie.
public sealed class PortalNoticeSerializer : TempDataSerializer
{
    public override bool CanSerializeType(Type type) => type == typeof(string);

    public override byte[] Serialize(IDictionary<string, object> values) =>
        JsonSerializer.SerializeToUtf8Bytes(values.ToDictionary(item => item.Key, item => (string)item.Value));

    public override IDictionary<string, object> Deserialize(byte[] value) =>
        (JsonSerializer.Deserialize<Dictionary<string, string>>(value) ?? new())
            .ToDictionary(item => item.Key, item => (object)item.Value);
}
