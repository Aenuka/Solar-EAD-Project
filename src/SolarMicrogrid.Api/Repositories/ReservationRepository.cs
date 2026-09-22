using MongoDB.Driver;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

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
        return await _reservations.Find(r => r.ProsumerNic == nic).ToListAsync();
    }

    public async Task<List<EnergyReservation>> GetPendingAsync()
    {
        return await _reservations.Find(r => r.Status == "PENDING").ToListAsync();
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

    public async Task UpdateAsync(EnergyReservation reservation)
    {
        await _reservations.ReplaceOneAsync(r => r.Id == reservation.Id, reservation);
    }
}