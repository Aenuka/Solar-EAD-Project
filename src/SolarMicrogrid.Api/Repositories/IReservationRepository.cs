/*
 * File: IReservationRepository.cs
 * Author: Sajith
 * Description: Repository interface for reservation data access.
 */

using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Repositories;

public interface IReservationRepository
{
    Task<EnergyReservation> CreateAsync(EnergyReservation reservation);
    Task<EnergyReservation?> GetByIdAsync(string id);
    Task<List<EnergyReservation>> GetByProsumerNicAsync(string nic);
    Task<List<EnergyReservation>> GetPendingAsync();
    Task<List<EnergyReservation>> GetApprovedFutureAsync();
    Task<List<EnergyReservation>> SearchAsync(string? status, string? stationId, string? nic, DateTime? from, DateTime? to);
    Task<EnergyReservation?> CheckConflictAsync(string stationId, string slotId, DateTime reservationDate);
    Task<EnergyReservation?> CheckConflictExcludingAsync(string stationId, string slotId, DateTime reservationDate, string excludeId);
    Task UpdateAsync(EnergyReservation reservation);
    Task<EnergyReservation?> GetByTokenAsync(string token);
}