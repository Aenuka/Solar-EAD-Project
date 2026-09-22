/*
 * File: ReservationService.cs
 * Author: Sajith
 * Description: Business logic for reservations (7-day rule, double booking check).
 */

using MongoDB.Driver;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public class ReservationService
{
    private readonly IMongoCollection<EnergyReservation> _reservations;

    public ReservationService(IMongoDatabase database)
    {
        _reservations = database.GetCollection<EnergyReservation>("energyReservations");
    }

    public async Task<(bool success, string message, EnergyReservation reservation)> CreateAsync(ReservationInput dto)
    {
        // Rule 1: Within 7 days
        if (dto.ReservationDate > DateTime.UtcNow.AddDays(7))
            return (false, "Reservation must be within 7 days.", null);

        if (dto.ReservationDate < DateTime.UtcNow)
            return (false, "Reservation date cannot be in the past.", null);

        // Rule 2: No double booking
        var existing = await _reservations.Find(r =>
            r.StationId == dto.StationId &&
            r.SlotId == dto.SlotId &&
            r.ReservationDate == dto.ReservationDate &&
            (r.Status == "PENDING" || r.Status == "APPROVED")
        ).FirstOrDefaultAsync();

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

        await _reservations.InsertOneAsync(reservation);
        return (true, "Reservation created successfully.", reservation);
    }
}