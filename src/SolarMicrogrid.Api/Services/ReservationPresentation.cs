using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

/// <summary>Resolves booking labels in batches, including inactive stations and historical windows.</summary>
public sealed class ReservationPresentation(StationRepository stations, ProsumerRepository prosumers)
{
    public async Task<ReservationResponse> MapAsync(EnergyReservation reservation, CancellationToken ct) =>
        (await MapAsync([reservation], ct))[0];

    public async Task<List<ReservationResponse>> MapAsync(IEnumerable<EnergyReservation> source, CancellationToken ct)
    {
        var bookings = source.ToList();
        if (bookings.Count == 0) return [];
        var stationTask = stations.FindManyAsync(bookings.Select(b => b.StationId).Distinct(), ct);
        var prosumerTask = prosumers.FindManyAsync(bookings.Select(b => b.ProsumerNic).Distinct(), ct);
        await Task.WhenAll(stationTask, prosumerTask);
        var stationLookup = (await stationTask).ToDictionary(s => s.Id);
        var prosumerLookup = (await prosumerTask).ToDictionary(p => p.Id);
        return bookings.Select(r =>
        {
            stationLookup.TryGetValue(r.StationId, out var station);
            prosumerLookup.TryGetValue(r.ProsumerNic, out var prosumer);
            var slot = station?.Slots.Find(s => s.Id == r.SlotId);
            return new ReservationResponse
            {
                Id = r.Id ?? string.Empty,
                ReservationId = r.ReservationId,
                ProsumerNic = r.ProsumerNic,
                StationId = r.StationId,
                SlotId = r.SlotId,
                StationName = station?.Name,
                StationAddress = station?.Address,
                ProsumerName = prosumer?.FullName,
                SlotStartsAt = slot?.StartsAt,
                SlotEndsAt = slot?.EndsAt,
                ReservationDate = r.ReservationDate,
                EnergyAmountKwh = r.EnergyAmountKwh,
                TradingType = r.TradingType,
                Status = r.Status,
                CreatedAt = r.CreatedAt,
                UpdatedAt = r.UpdatedAt,
                TransactionToken = r.TransactionToken,
                CancellationReason = r.CancellationReason,
                CompletedAt = r.CompletedAt,
                StationVersion = r.StationVersion,
                AllocationSlots = r.AllocationSlots
            };
        }).ToList();
    }
}
