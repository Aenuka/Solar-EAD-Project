using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Contracts;
using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Api.Services;

// All inventory and status mutations share the station's compare-and-swap version.
// Booking/QR services must use this service, never write a second inventory counter.
public sealed class StationService(IStationRepository repository)
{
    private static void Validate(object input)
    {
        var errors = new List<ValidationResult>();
        if (!Validator.TryValidateObject(input, new ValidationContext(input), errors, true))
            throw ApiException.Invalid(string.Join(" ", errors.Select(x => x.ErrorMessage)));
    }
    private static ApiException Missing() => new(404, "station_not_found", "The station, slot or allocation was not found.");
    private async Task<SolarStation> Find(string id, CancellationToken ct) => await repository.FindAsync(id, ct) ?? throw Missing();
    private static bool Live(StationAllocation a) => a.Status == "Reserved";
    private static int Active(SolarStation s) => s.Slots.Sum(x => x.Allocations.Count(Live));
    private static void Version(SolarStation s, long version) => ResponseMapping.CheckVersion(s.Version, version);
    private async Task<StationResponse> Save(SolarStation s, CancellationToken ct)
    {
        if (!await repository.ReplaceAsync(s with { Version = s.Version + 1 }, s.Version, ct))
            throw ApiException.Conflict("Station availability changed. Reload before trying again.");
        return Map(s with { Version = s.Version + 1 }, true);
    }
    public async Task<StationResponse> GetAsync(string id, bool staff, CancellationToken ct)
    {
        var s = await Find(id, ct);
        if (!staff && !s.Active) throw Missing();
        return Map(s, staff);
    }
    public async Task<PageResponse<StationResponse>> ListAsync(bool staff, bool activeOnly, int page, int pageSize,
        double? latitude, double? longitude, double radiusKm, CancellationToken ct)
    {
        if (latitude.HasValue != longitude.HasValue) throw ApiException.Invalid("Supply both latitude and longitude.");
        var stations = await repository.ListAsync(!staff || activeOnly, ct);
        IEnumerable<StationResponse> results = stations.Select(s => { var item = Map(s, false); return item with { Slots = item.Slots.Take(1).ToList() }; });
        if (latitude.HasValue && longitude.HasValue)
            results = results.Select(s => s with { DistanceKm = Distance(latitude.Value, longitude.Value, s.Latitude, s.Longitude) })
                .Where(s => s.DistanceKm <= radiusKm).OrderBy(s => s.DistanceKm).ThenBy(s => s.Id);
        var all = results.ToList();
        return new(all.Skip((page - 1) * pageSize).Take(pageSize).ToList(), all.Count, page, pageSize);
    }
    public async Task<StationResponse> CreateAsync(StationInput r, CancellationToken ct)
    {
        Validate(r);
        var s = Apply(new SolarStation(), r);
        await repository.InsertAsync(s, ct);
        return Map(s, true);
    }
    private static SolarStation Apply(SolarStation s, StationInput r) => s with
    { Name = r.Name.Trim(), Address = r.Address.Trim(), Latitude = r.Latitude, Longitude = r.Longitude,
      CapacityKw = r.CapacityKw, StorageKwh = r.StorageKwh, BatterySlots = r.BatterySlots };
    public async Task<StationResponse> UpdateAsync(string id, StationUpdate r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        var updated = Apply(s, r);
        foreach (var slot in s.Slots.Where(x => x.EndsAt > DateTime.UtcNow || x.Allocations.Any(Live))) CheckLimits(updated, slot);
        return await Save(updated, ct);
    }
    public async Task<StationResponse> StatusAsync(string id, StationStatusInput r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        if (!r.Active && Active(s) > 0) throw ApiException.Conflict("This station has active reservations and cannot be deactivated.");
        return await Save(s with { Active = r.Active }, ct);
    }
    public async Task<StationResponse> ScheduleAsync(string id, ScheduleInput r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        if (r.Days.Any(d => d < 0 || d > 6) || r.Days.Distinct().Count() != r.Days.Length ||
            !TimeOnly.TryParseExact(r.OpensAt, "HH:mm", out var open) || !TimeOnly.TryParseExact(r.ClosesAt, "HH:mm", out var close) || open >= close)
            throw ApiException.Invalid("Choose distinct operating days (0–6) and opening time before closing time. Overnight hours are not supported.");
        var updated = s with { Days = r.Days, OpensAt = r.OpensAt, ClosesAt = r.ClosesAt };
        if (s.Slots.Any(x => (x.EndsAt > DateTime.UtcNow || x.Allocations.Any(Live)) && !WithinSchedule(updated, x)))
            throw ApiException.Conflict("Existing slots fall outside this schedule. Remove unallocated slots first; reserved slots must be resolved by the booking component.");
        return await Save(updated, ct);
    }
    public async Task<StationResponse> AddSlotAsync(string id, SlotInput r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        if (!s.Active) throw ApiException.Conflict("Activate the station before adding slots.");
        if (r.StartsAt <= DateTimeOffset.UtcNow || r.EndsAt <= r.StartsAt || r.EndsAt - r.StartsAt > TimeSpan.FromDays(1))
            throw ApiException.Invalid("Choose a future time window ending after its start, within one day.");
        if (s.Slots.Count >= 500) throw ApiException.Conflict("Station has 500 windows. Archive completed windows before adding more.");
        var slot = new EnergySlot { StartsAt = r.StartsAt.UtcDateTime, EndsAt = r.EndsAt.UtcDateTime,
            UsableSlots = r.UsableSlots, UsableEnergyKwh = r.UsableEnergyKwh };
        if (!WithinSchedule(s, slot)) throw ApiException.Invalid("The slot must fall within the station's operating days and hours (Asia/Colombo).");
        if (s.Slots.Any(x => slot.StartsAt < x.EndsAt && slot.EndsAt > x.StartsAt))
            throw ApiException.Conflict("Station time windows cannot overlap.");
        CheckLimits(s, slot);
        return await Save(s with { Slots = [..s.Slots, slot] }, ct);
    }
    public async Task<StationResponse> AvailabilityAsync(string id, string slotId, AvailabilityInput r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        var slot = s.Slots.Find(x => x.Id == slotId) ?? throw Missing();
        if (slot.EndsAt <= DateTime.UtcNow) throw ApiException.Conflict("Past slot availability cannot be edited.");
        var updated = slot with { UsableSlots = r.UsableSlots, UsableEnergyKwh = r.UsableEnergyKwh };
        CheckLimits(s, updated);
        return await Save(ReplaceSlot(s, updated), ct);
    }
    public async Task<StationResponse> RemoveSlotAsync(string id, string slotId, StationVersion r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct); Version(s, r.Version);
        var slot = s.Slots.Find(x => x.Id == slotId) ?? throw Missing();
        if (slot.Allocations.Any(Live)) throw ApiException.Conflict("A slot with active reservations cannot be removed.");
        if (slot.Allocations.Count > 0 && slot.EndsAt > DateTime.UtcNow)
            throw ApiException.Conflict("A used window can only be archived after it ends.");
        return await Save(s with { Slots = s.Slots.Where(x => x.Id != slotId).ToList() }, ct);
    }
    public async Task<StationResponse> ReserveAsync(string id, string slotId, AllocationInput r, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct);
        var slot = s.Slots.Find(x => x.Id == slotId) ?? throw Missing();
        var existing = s.Slots.SelectMany(x => x.Allocations).FirstOrDefault(x => x.BookingId == r.BookingId);
        if (existing != null)
        {
            if (slot.Allocations.Contains(existing) && existing.Status == "Reserved" && existing.Slots == r.Slots && existing.EnergyKwh == r.EnergyKwh)
                return Map(s, true); // A retry of a committed request never deducts twice.
            throw ApiException.Conflict("Booking ID already used. Use a new allocation ID when modifying a booking.");
        }
        Version(s, r.Version);
        if (!s.Active) throw ApiException.Conflict("Station is inactive.");
        if (slot.StartsAt <= DateTime.UtcNow || slot.StartsAt > DateTime.UtcNow.AddDays(7))
            throw ApiException.Conflict("Reservations must start in the next seven days.");
        if (slot.Allocations.Count >= 100) throw ApiException.Conflict("This window's allocation limit has been reached.");
        var updated = slot with { Allocations = [..slot.Allocations, new(r.BookingId, r.Slots, r.EnergyKwh, "Reserved")] };
        CheckLimits(s, updated);
        return await Save(ReplaceSlot(s, updated), ct);
    }
    public async Task<StationResponse> EndAllocationAsync(string id, string slotId, string bookingId, StationVersion r, bool complete, CancellationToken ct)
    {
        Validate(r);
        var s = await Find(id, ct);
        var slot = s.Slots.Find(x => x.Id == slotId) ?? throw Missing();
        var allocation = slot.Allocations.Find(x => x.BookingId == bookingId) ?? throw Missing();
        var status = complete ? "Completed" : "Cancelled";
        if (allocation.Status == status) return Map(s, true);
        Version(s, r.Version);
        if (!Live(allocation)) throw ApiException.Conflict("Allocation has already ended.");
        if (complete && DateTime.UtcNow < slot.StartsAt) throw ApiException.Conflict("Transfer cannot be completed before the window starts.");
        if (!complete && slot.StartsAt < DateTime.UtcNow.AddHours(12)) throw ApiException.Conflict("Cancellation requires at least twelve hours' notice.");
        var updated = slot with { Allocations = slot.Allocations.Select(x => x.BookingId == bookingId ? x with { Status = status } : x).ToList() };
        return await Save(ReplaceSlot(s, updated), ct);
    }
    private static SolarStation ReplaceSlot(SolarStation s, EnergySlot slot) => s with { Slots = s.Slots.Select(x => x.Id == slot.Id ? slot : x).ToList() };
    private static void CheckLimits(SolarStation s, EnergySlot slot)
    {
        // Completed energy remains consumed; only cancellation returns inventory.
        var used = slot.Allocations.Where(x => x.Status != "Cancelled").ToList();
        if (slot.UsableSlots > s.BatterySlots || slot.UsableEnergyKwh > s.StorageKwh)
            throw ApiException.Conflict("Window availability exceeds the station's battery, storage or power capacity.");
        var hours = (slot.EndsAt - slot.StartsAt).TotalHours;
        var maxEnergy = s.CapacityKw * hours;
        if (slot.UsableEnergyKwh > maxEnergy + 0.000001)
            throw ApiException.Conflict(FormattableString.Invariant(
                $"Usable energy exceeds this window's power capacity. {s.CapacityKw:0.##} kW for {hours:0.##} hour(s) permits at most {maxEnergy:0.##} kWh."));
        if (slot.UsableSlots < used.Sum(x => x.Slots) || slot.UsableEnergyKwh + 0.000001 < used.Sum(x => x.EnergyKwh))
            throw ApiException.Conflict("Availability cannot be lower than reserved/completed usage; this request would overbook the window.");
    }
    private static bool WithinSchedule(SolarStation s, EnergySlot slot)
    {
        var start = new DateTimeOffset(slot.StartsAt, TimeSpan.Zero).ToOffset(TimeSpan.FromMinutes(330));
        var end = new DateTimeOffset(slot.EndsAt, TimeSpan.Zero).ToOffset(TimeSpan.FromMinutes(330));
        return start.Date == end.Date && s.Days.Contains((int)start.DayOfWeek) &&
            TimeOnly.FromDateTime(start.DateTime) >= TimeOnly.ParseExact(s.OpensAt, "HH:mm") &&
            TimeOnly.FromDateTime(end.DateTime) <= TimeOnly.ParseExact(s.ClosesAt, "HH:mm");
    }
    private static StationResponse Map(SolarStation s, bool staff) => new(s.Id, s.Name, s.Address, s.Latitude, s.Longitude,
        s.CapacityKw, s.StorageKwh, s.BatterySlots, s.Active, s.Version, new(s.Days, s.OpensAt, s.ClosesAt, "Asia/Colombo"), Active(s),
        s.Slots.Where(x => staff || x.EndsAt > DateTime.UtcNow).OrderBy(x => x.StartsAt).Select(x =>
        {
            var used = x.Allocations.Where(a => a.Status != "Cancelled").ToList();
            return new EnergySlotResponse(x.Id, x.StartsAt, x.EndsAt, x.UsableSlots, x.UsableEnergyKwh,
                x.UsableSlots - used.Sum(a => a.Slots), Math.Max(0, x.UsableEnergyKwh - used.Sum(a => a.EnergyKwh)),
                x.Allocations.Count(Live), staff ? x.Allocations.Select(a => new AllocationResponse(a.BookingId, a.Slots, a.EnergyKwh, a.Status)).ToList() : []);
        }).ToList());
    private static double Distance(double lat, double lon, double otherLat, double otherLon)
    {
        const double radians = Math.PI / 180;
        var a = Math.Pow(Math.Sin((otherLat - lat) * radians / 2), 2) + Math.Cos(lat * radians) * Math.Cos(otherLat * radians) * Math.Pow(Math.Sin((otherLon - lon) * radians / 2), 2);
        return 6371 * 2 * Math.Asin(Math.Sqrt(Math.Clamp(a, 0, 1)));
    }
}
