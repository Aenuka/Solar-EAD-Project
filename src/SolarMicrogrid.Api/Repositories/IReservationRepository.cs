using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Repositories;

public interface IReservationRepository
{
    Task<EnergyReservation> CreateAsync(EnergyReservation reservation);
    Task<EnergyReservation?> GetByIdAsync(string id);
    Task<List<EnergyReservation>> GetByProsumerNicAsync(string nic);
    Task<List<EnergyReservation>> GetPendingAsync();
    Task<EnergyReservation?> CheckConflictAsync(string stationId, string slotId, DateTime reservationDate);
    Task UpdateAsync(EnergyReservation reservation);
}