/*
 * File: ReservationRepository.cs
 * Author: Sajith
 * Description: MongoDB implementation for reservation repository.
 */

using MongoDB.Driver;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Repositories;

public class ReservationRepository : IReservationRepository
{
    private readonly IMongoCollection<EnergyReservation> _reservations;

    public ReservationRepository(IMongoDatabase database)
    {
        _reservations = database.GetCollection<EnergyReservation>("energyReservations");
    }

    public async Task<EnergyReservation> CreateAsync(EnergyReservation reservation)
    {
        await _reservations.InsertOneAsync(reservation);
        return reservation;
    }

    public async Task<EnergyReservation?> GetByIdAsync(string id)
    {
        return await _reservations.Find(r => r.Id == id).FirstOrDefaultAsync();
    }

    public async Task<List<EnergyReservation>> GetByProsumerNicAsync(string nic)
    {
        return await _reservations.Find(r => r.ProsumerNic == nic)
            .SortByDescending(r => r.ReservationDate)
            .ToListAsync();
    }

    public async Task<List<EnergyReservation>> GetPendingAsync()
    {
        return await _reservations.Find(r => r.Status == "PENDING")
            .SortBy(r => r.ReservationDate)
            .ToListAsync();
    }

    public async Task<List<EnergyReservation>> GetApprovedFutureAsync()
    {
        var now = DateTime.UtcNow;
        return await _reservations.Find(r => r.Status == "APPROVED" && r.ReservationDate > now)
            .SortBy(r => r.ReservationDate)
            .ToListAsync();
    }

    public async Task<List<EnergyReservation>> SearchAsync(string? status, string? stationId, string? nic, DateTime? from, DateTime? to)
    {
        var builder = Builders<EnergyReservation>.Filter;
        var filters = new List<FilterDefinition<EnergyReservation>>();

        if (!string.IsNullOrEmpty(status))
            filters.Add(builder.Eq(r => r.Status, status));
        if (!string.IsNullOrEmpty(stationId))
            filters.Add(builder.Eq(r => r.StationId, stationId));
        if (!string.IsNullOrEmpty(nic))
            filters.Add(builder.Eq(r => r.ProsumerNic, nic));
        if (from.HasValue)
            filters.Add(builder.Gte(r => r.ReservationDate, from.Value));
        if (to.HasValue)
            filters.Add(builder.Lte(r => r.ReservationDate, to.Value));

        var combined = filters.Count > 0 ? builder.And(filters) : builder.Empty;
        return await _reservations.Find(combined).SortBy(r => r.ReservationDate).ToListAsync();
    }

    public async Task<EnergyReservation?> CheckConflictAsync(string stationId, string slotId, DateTime reservationDate)
    {
        return await _reservations.Find(r =>
            r.StationId == stationId &&
            r.SlotId == slotId &&
            r.ReservationDate == reservationDate &&
            (r.Status == "PENDING" || r.Status == "APPROVED")
        ).FirstOrDefaultAsync();
    }

    public async Task<EnergyReservation?> CheckConflictExcludingAsync(string stationId, string slotId, DateTime reservationDate, string excludeId)
    {
        return await _reservations.Find(r =>
            r.Id != excludeId &&
            r.StationId == stationId &&
            r.SlotId == slotId &&
            r.ReservationDate == reservationDate &&
            (r.Status == "PENDING" || r.Status == "APPROVED")
        ).FirstOrDefaultAsync();
    }

    public async Task UpdateAsync(EnergyReservation reservation)
    {
        await _reservations.ReplaceOneAsync(r => r.Id == reservation.Id, reservation);
    }

    public async Task<EnergyReservation?> GetByTokenAsync(string token)
    {
        return await _reservations.Find(r => r.TransactionToken == token).FirstOrDefaultAsync();
    }
}