using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public class ReservationService
{
    private readonly IReservationRepository _repository;

    public ReservationService(IReservationRepository repository)
    {
        _repository = repository;
    }

    public async Task<(bool success, string message, EnergyReservation reservation)> CreateAsync(ReservationInput dto)
    {
        // Rule 1: Within 7 days
        if (dto.ReservationDate > DateTime.UtcNow.AddDays(7))
            return (false, "Reservation must be within 7 days.", null);

        if (dto.ReservationDate < DateTime.UtcNow)
            return (false, "Reservation date cannot be in the past.", null);

        // Rule 2: No double booking
        var existing = await _repository.CheckConflictAsync(dto.StationId, dto.SlotId, dto.ReservationDate);
        if (existing != null)
            return (false, "This slot is already booked.", null);

        var reservation = new EnergyReservation
        {
            ReservationId = "RES-" + DateTime.UtcNow.Ticks,
            ProsumerNic = dto.ProsumerNic,
            StationId = dto.StationId,
            SlotId = dto.SlotId,
            ReservationDate = dto.ReservationDate,
            EnergyAmountKwh = dto.EnergyAmountKwh,
            TradingType = dto.TradingType,
            Status = "PENDING",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _repository.CreateAsync(reservation);
        return (true, "Reservation created successfully.", reservation);
    }
}