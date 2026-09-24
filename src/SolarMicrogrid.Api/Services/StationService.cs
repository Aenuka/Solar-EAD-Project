using System.ComponentModel.DataAnnotations;
using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

// All inventory changes use the station's version to prevent concurrent overbooking.
// Booking/QR services must use this service rather than keep a second inventory counter.
public sealed class StationService(StationRepository repository)
{
    public async Task<StationResponse> GetAsync(string id, bool isStaff, CancellationToken ct)
    {
        var station = await FindAsync(id, ct);
        if (!isStaff && !station.Active)
        {
            throw NotFound();
        }

        return ToResponse(station, isStaff);
    }

    public async Task<PageResponse<StationResponse>> ListAsync(bool isStaff, bool activeOnly, int page, int pageSize,
        double? latitude, double? longitude, double radiusKm, CancellationToken ct)
    {
        if (latitude.HasValue != longitude.HasValue)
        {
            throw ApiException.Invalid("Supply both latitude and longitude.");
        }

        var stations = await repository.ListAsync(!isStaff || activeOnly, ct);
        IEnumerable<StationResponse> results = stations.Select(station =>
        {
            var response = ToResponse(station, false);
            return response with { Slots = response.Slots.Take(1).ToList() };
        });

        if (latitude.HasValue && longitude.HasValue)
        {
            results = results.Select(station => station with
            {
                DistanceKm = CalculateDistanceKm(latitude.Value, longitude.Value, station.Latitude, station.Longitude)
            })
                .Where(station => station.DistanceKm <= radiusKm)
                .OrderBy(station => station.DistanceKm)
                .ThenBy(station => station.Id);
        }

        var all = results.ToList();
        var items = all.Skip((page - 1) * pageSize).Take(pageSize).ToList();
        return new PageResponse<StationResponse>(items, all.Count, page, pageSize);
    }

    public async Task<StationResponse> CreateAsync(StationInput request, CancellationToken ct)
    {
        Validate(request);
        SolarStation station = new();
        UpdateDetails(station, request);
        await repository.InsertAsync(station, ct);
        return ToResponse(station, true);
    }

    public async Task<StationResponse> UpdateAsync(string id, StationUpdate request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);
        UpdateDetails(station, request);

        foreach (var slot in station.Slots.Where(slot => slot.EndsAt > DateTime.UtcNow || slot.Allocations.Any(IsReserved)))
        {
            CheckLimits(station, slot);
        }

        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> StatusAsync(string id, StationStatusInput request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);

        if (!request.Active && CountActiveReservations(station) > 0)
        {
            throw ApiException.Conflict("This station has active reservations and cannot be deactivated.");
        }

        station.Active = request.Active;
        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> ScheduleAsync(string id, ScheduleInput request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);

        if (request.Days.Any(day => day < 0 || day > 6) || request.Days.Distinct().Count() != request.Days.Length ||
            !TimeOnly.TryParseExact(request.OpensAt, "HH:mm", out var opensAt) ||
            !TimeOnly.TryParseExact(request.ClosesAt, "HH:mm", out var closesAt) || opensAt >= closesAt)
        {
            throw ApiException.Invalid("Choose distinct operating days (0–6) and opening time before closing time. Overnight hours are not supported.");
        }

        station.Days = request.Days;
        station.OpensAt = request.OpensAt;
        station.ClosesAt = request.ClosesAt;

        if (station.Slots.Any(slot =>
            (slot.EndsAt > DateTime.UtcNow || slot.Allocations.Any(IsReserved)) && !WithinSchedule(station, slot)))
        {
            throw ApiException.Conflict("Existing slots fall outside this schedule. Remove unallocated slots first; reserved slots must be resolved by the booking component.");
        }

        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> AddSlotAsync(string id, SlotInput request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);

        if (!station.Active)
        {
            throw ApiException.Conflict("Activate the station before adding slots.");
        }
        if (request.StartsAt <= DateTimeOffset.UtcNow || request.EndsAt <= request.StartsAt ||
            request.EndsAt - request.StartsAt > TimeSpan.FromDays(1))
        {
            throw ApiException.Invalid("Choose a future time window ending after its start, within one day.");
        }
        if (station.Slots.Count >= 500)
        {
            throw ApiException.Conflict("Station has 500 windows. Archive completed windows before adding more.");
        }

        EnergySlot slot = new()
        {
            StartsAt = request.StartsAt.UtcDateTime,
            EndsAt = request.EndsAt.UtcDateTime,
            UsableSlots = request.UsableSlots,
            UsableEnergyKwh = request.UsableEnergyKwh
        };

        if (!WithinSchedule(station, slot))
        {
            throw ApiException.Invalid("The slot must fall within the station's operating days and hours (Asia/Colombo).");
        }
        if (station.Slots.Any(existing => slot.StartsAt < existing.EndsAt && slot.EndsAt > existing.StartsAt))
        {
            throw ApiException.Conflict("Station time windows cannot overlap.");
        }

        CheckLimits(station, slot);
        station.Slots.Add(slot);
        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> AvailabilityAsync(string id, string slotId, AvailabilityInput request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);
        var slot = station.Slots.Find(slot => slot.Id == slotId) ?? throw NotFound();

        if (slot.EndsAt <= DateTime.UtcNow)
        {
            throw ApiException.Conflict("Past slot availability cannot be edited.");
        }

        slot.UsableSlots = request.UsableSlots;
        slot.UsableEnergyKwh = request.UsableEnergyKwh;
        CheckLimits(station, slot);
        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> RemoveSlotAsync(string id, string slotId, StationVersion request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        ResponseMapping.CheckVersion(station.Version, request.Version);
        var slot = station.Slots.Find(slot => slot.Id == slotId) ?? throw NotFound();

        if (slot.Allocations.Any(IsReserved))
        {
            throw ApiException.Conflict("A slot with active reservations cannot be removed.");
        }
        if (slot.Allocations.Count > 0 && slot.EndsAt > DateTime.UtcNow)
        {
            throw ApiException.Conflict("A used window can only be archived after it ends.");
        }

        station.Slots.Remove(slot);
        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> ReserveAsync(string id, string slotId, AllocationInput request, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        var slot = station.Slots.Find(slot => slot.Id == slotId) ?? throw NotFound();
        var existing = station.Slots.SelectMany(slot => slot.Allocations)
            .FirstOrDefault(allocation => allocation.BookingId == request.BookingId);

        if (existing is not null)
        {
            if (slot.Allocations.Contains(existing) && IsReserved(existing) &&
                existing.Slots == request.Slots && existing.EnergyKwh == request.EnergyKwh)
            {
                // Retrying a successful reservation must not deduct inventory twice.
                return ToResponse(station, true);
            }

            throw ApiException.Conflict("Booking ID already used. Use a new allocation ID when modifying a booking.");
        }

        ResponseMapping.CheckVersion(station.Version, request.Version);
        if (!station.Active)
        {
            throw ApiException.Conflict("Station is inactive.");
        }
        if (slot.StartsAt <= DateTime.UtcNow || slot.StartsAt > DateTime.UtcNow.AddDays(7))
        {
            throw ApiException.Conflict("Reservations must start in the next seven days.");
        }
        if (slot.Allocations.Count >= 100)
        {
            throw ApiException.Conflict("This window's allocation limit has been reached.");
        }

        slot.Allocations.Add(new StationAllocation(request.BookingId, request.Slots, request.EnergyKwh, "Reserved"));
        CheckLimits(station, slot);
        return await SaveAsync(station, ct);
    }

    public async Task<StationResponse> EndAllocationAsync(string id, string slotId, string bookingId,
        StationVersion request, bool complete, CancellationToken ct)
    {
        Validate(request);
        var station = await FindAsync(id, ct);
        var slot = station.Slots.Find(slot => slot.Id == slotId) ?? throw NotFound();
        var allocation = slot.Allocations.Find(allocation => allocation.BookingId == bookingId) ?? throw NotFound();
        var status = complete ? "Completed" : "Cancelled";

        if (allocation.Status == status)
        {
            return ToResponse(station, true);
        }

        ResponseMapping.CheckVersion(station.Version, request.Version);
        if (!IsReserved(allocation))
        {
            throw ApiException.Conflict("Allocation has already ended.");
        }
        if (complete && DateTime.UtcNow < slot.StartsAt)
        {
            throw ApiException.Conflict("Transfer cannot be completed before the window starts.");
        }
        if (!complete && slot.StartsAt < DateTime.UtcNow.AddHours(12))
        {
            throw ApiException.Conflict("Cancellation requires at least twelve hours' notice.");
        }

        var allocationIndex = slot.Allocations.IndexOf(allocation);
        slot.Allocations[allocationIndex] = allocation with { Status = status };
        return await SaveAsync(station, ct);
    }

    private async Task<SolarStation> FindAsync(string id, CancellationToken ct) =>
        await repository.FindAsync(id, ct) ?? throw NotFound();

    private async Task<StationResponse> SaveAsync(SolarStation station, CancellationToken ct)
    {
        var previousVersion = station.Version;
        station.Version++;

        // The database only saves this document if no other request changed its version.
        if (!await repository.ReplaceAsync(station, previousVersion, ct))
        {
            throw ApiException.Conflict("Station availability changed. Reload before trying again.");
        }

        return ToResponse(station, true);
    }

    private static void UpdateDetails(SolarStation station, StationInput request)
    {
        station.Name = request.Name.Trim();
        station.Address = request.Address.Trim();
        station.Latitude = request.Latitude;
        station.Longitude = request.Longitude;
        station.CapacityKw = request.CapacityKw;
        station.StorageKwh = request.StorageKwh;
        station.BatterySlots = request.BatterySlots;
    }

    private static void Validate(object input)
    {
        // Keep validation here too: later booking components can call the service directly.
        var errors = new List<ValidationResult>();
        if (!Validator.TryValidateObject(input, new ValidationContext(input), errors, validateAllProperties: true))
        {
            throw ApiException.Invalid(string.Join(" ", errors.Select(error => error.ErrorMessage)));
        }
    }

    private static ApiException NotFound() =>
        new(404, "station_not_found", "The station, slot or allocation was not found.");

    private static bool IsReserved(StationAllocation allocation) => allocation.Status == "Reserved";

    private static int CountActiveReservations(SolarStation station) =>
        station.Slots.Sum(slot => slot.Allocations.Count(IsReserved));

    private static void CheckLimits(SolarStation station, EnergySlot slot)
    {
        // Completed energy remains consumed; only cancellation returns inventory.
        var used = slot.Allocations.Where(allocation => allocation.Status != "Cancelled").ToList();
        if (slot.UsableSlots > station.BatterySlots || slot.UsableEnergyKwh > station.StorageKwh)
        {
            throw ApiException.Conflict("Window availability exceeds the station's battery, storage or power capacity.");
        }

        var hours = (slot.EndsAt - slot.StartsAt).TotalHours;
        var maxEnergy = station.CapacityKw * hours;
        if (slot.UsableEnergyKwh > maxEnergy + 0.000001)
        {
            throw ApiException.Conflict(FormattableString.Invariant(
                $"Usable energy exceeds this window's power capacity. {station.CapacityKw:0.##} kW for {hours:0.##} hour(s) permits at most {maxEnergy:0.##} kWh."));
        }
        if (slot.UsableSlots < used.Sum(allocation => allocation.Slots) ||
            slot.UsableEnergyKwh + 0.000001 < used.Sum(allocation => allocation.EnergyKwh))
        {
            throw ApiException.Conflict("Availability cannot be lower than reserved/completed usage; this request would overbook the window.");
        }
    }

    private static bool WithinSchedule(SolarStation station, EnergySlot slot)
    {
        var colomboOffset = TimeSpan.FromMinutes(330);
        var start = new DateTimeOffset(slot.StartsAt, TimeSpan.Zero).ToOffset(colomboOffset);
        var end = new DateTimeOffset(slot.EndsAt, TimeSpan.Zero).ToOffset(colomboOffset);

        return start.Date == end.Date && station.Days.Contains((int)start.DayOfWeek) &&
            TimeOnly.FromDateTime(start.DateTime) >= TimeOnly.ParseExact(station.OpensAt, "HH:mm") &&
            TimeOnly.FromDateTime(end.DateTime) <= TimeOnly.ParseExact(station.ClosesAt, "HH:mm");
    }

    private static StationResponse ToResponse(SolarStation station, bool isStaff)
    {
        var slots = station.Slots
            .Where(slot => isStaff || slot.EndsAt > DateTime.UtcNow)
            .OrderBy(slot => slot.StartsAt)
            .Select(slot =>
            {
                var used = slot.Allocations.Where(allocation => allocation.Status != "Cancelled").ToList();
                var availableSlots = slot.UsableSlots - used.Sum(allocation => allocation.Slots);
                var availableEnergy = Math.Max(0, slot.UsableEnergyKwh - used.Sum(allocation => allocation.EnergyKwh));
                var allocations = isStaff
                    ? slot.Allocations.Select(allocation => new AllocationResponse(
                        allocation.BookingId, allocation.Slots, allocation.EnergyKwh, allocation.Status)).ToList()
                    : [];

                return new EnergySlotResponse(slot.Id, slot.StartsAt, slot.EndsAt,
                    slot.UsableSlots, slot.UsableEnergyKwh, availableSlots, availableEnergy,
                    slot.Allocations.Count(IsReserved), allocations);
            }).ToList();

        var schedule = new StationSchedule(station.Days, station.OpensAt, station.ClosesAt, "Asia/Colombo");
        return new StationResponse(station.Id, station.Name, station.Address, station.Latitude, station.Longitude,
            station.CapacityKw, station.StorageKwh, station.BatterySlots, station.Active, station.Version,
            schedule, CountActiveReservations(station), slots);
    }

    private static double CalculateDistanceKm(double latitude, double longitude, double otherLatitude, double otherLongitude)
    {
        const double radians = Math.PI / 180;
        const double earthRadiusKm = 6371;
        var latitudeDifference = (otherLatitude - latitude) * radians;
        var longitudeDifference = (otherLongitude - longitude) * radians;
        var arc = Math.Pow(Math.Sin(latitudeDifference / 2), 2) +
            Math.Cos(latitude * radians) * Math.Cos(otherLatitude * radians) * Math.Pow(Math.Sin(longitudeDifference / 2), 2);

        return earthRadiusKm * 2 * Math.Asin(Math.Sqrt(Math.Clamp(arc, 0, 1)));
    }
}
