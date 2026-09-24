/*
 * File: ReservationService.cs
 * Author: Sajith
 * Description: Business logic for reservations (7-day rule, 12-hour rule, double booking).
 */

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

    private async Task SaveAsync(EnergyReservation reservation, long expectedVersion)
    {
        reservation.Version = expectedVersion + 1;
        if (!await _repository.ReplaceAsync(reservation, expectedVersion))
            throw ApiException.Conflict("Reservation changed. Reload before trying again.");
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> CreateAsync(ReservationInput dto)
    {
        if (dto.ReservationDate > DateTime.UtcNow.AddDays(7))
            return (false, "Reservation must be within 7 days.", null);

        if (dto.ReservationDate < DateTime.UtcNow)
            return (false, "Reservation date cannot be in the past.", null);

        var existing = await _repository.CheckConflictAsync(dto.StationId, dto.SlotId, dto.ReservationDate);
        if (existing != null)
            return (false, "This slot is already booked for that time.", null);

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

    public async Task<(bool success, string message, EnergyReservation? reservation)> UpdateAsync(string id, UpdateReservationInput dto)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        ResponseMapping.CheckVersion(existing.Version, dto.Version);

        if (existing.Status != "PENDING" && existing.Status != "APPROVED")
            return (false, "Only pending or approved reservations can be updated.", null);

        var hoursUntil = (existing.ReservationDate - DateTime.UtcNow).TotalHours;
        if (hoursUntil < 12)
            return (false, "Updates require at least 12 hours notice.", null);

        if (dto.ReservationDate > DateTime.UtcNow.AddDays(7))
            return (false, "New reservation date must be within 7 days.", null);

        if (dto.ReservationDate < DateTime.UtcNow)
            return (false, "Reservation date cannot be in the past.", null);

        var conflict = await _repository.CheckConflictExcludingAsync(
            existing.StationId, dto.SlotId, dto.ReservationDate, id);
        if (conflict != null)
            return (false, "This slot is already booked for that time.", null);

        existing.SlotId = dto.SlotId;
        existing.ReservationDate = dto.ReservationDate;
        existing.EnergyAmountKwh = dto.EnergyAmountKwh;
        existing.TradingType = dto.TradingType;
        existing.UpdatedAt = DateTime.UtcNow;

        await SaveAsync(existing, dto.Version);
        return (true, "Reservation updated successfully.", existing);
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> CancelAsync(string id, CancelReservationInput dto)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        ResponseMapping.CheckVersion(existing.Version, dto.Version);

        if (existing.Status == "CANCELLED")
            return (false, "Reservation is already cancelled.", null);

        if (existing.Status == "COMPLETED")
            return (false, "Completed reservations cannot be cancelled.", null);

        var hoursUntil = (existing.ReservationDate - DateTime.UtcNow).TotalHours;
        if (hoursUntil < 12)
            return (false, "Cancellations require at least 12 hours notice.", null);

        existing.Status = "CANCELLED";
        existing.CancellationReason = dto.Reason;
        existing.UpdatedAt = DateTime.UtcNow;

        // TODO: Chamithu's API — release the slot
        await SaveAsync(existing, dto.Version);
        return (true, "Reservation cancelled successfully.", existing);
    }

    public async Task<List<EnergyReservation>> GetHistoryAsync(string nic)
        => await _repository.GetByProsumerNicAsync(nic);

    public async Task<List<EnergyReservation>> GetPendingAsync()
        => await _repository.GetPendingAsync();

    public async Task<List<EnergyReservation>> SearchAsync(string? status, string? stationId, string? nic, DateTime? from, DateTime? to)
        => await _repository.SearchAsync(status, stationId, nic, from, to);

    public async Task<int> GetApprovedFutureCountAsync()
    {
        var list = await _repository.GetApprovedFutureAsync();
        return list.Count;
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> GetTransactionAsync(string id)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        if (existing.Status != "APPROVED")
            return (false, "Only approved reservations can be used for QR verification.", null);

        if (string.IsNullOrEmpty(existing.TransactionToken))
        {
            existing.TransactionToken = Guid.NewGuid().ToString("N").ToUpper();
            existing.UpdatedAt = DateTime.UtcNow;
            await SaveAsync(existing, existing.Version);
        }

        return (true, "Transaction token retrieved.", existing);
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> ApproveAsync(string id, long version)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null) return (false, "Reservation not found.", null);
        
        ResponseMapping.CheckVersion(existing.Version, version);
        
        if (existing.Status != "PENDING") return (false, "Only pending reservations can be approved.", null);
        
        existing.Status = "APPROVED";
        existing.UpdatedAt = DateTime.UtcNow;
        
        await SaveAsync(existing, version);
        return (true, "Reservation approved successfully.", existing);
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> VerifyAsync(string token)
    {
        var list = await _repository.SearchAsync(null, null, null, null, null);
        var existing = list.FirstOrDefault(r => r.TransactionToken == token);
        
        if (existing is null) return (false, "Invalid transaction token.", null);
        if (existing.Status == "CANCELLED") return (false, "Reservation is cancelled.", null);
        if (existing.Status == "COMPLETED") return (false, "Reservation is already completed.", null);
        if (existing.Status != "APPROVED") return (false, "Reservation is not approved.", null);
        
        return (true, "Token verified successfully.", existing);
    }

    public async Task<(bool success, string message, EnergyReservation? reservation)> CompleteAsync(string id, CompleteReservationInput dto)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null) return (false, "Reservation not found.", null);
        
        if (existing.TransactionToken != dto.TransactionToken) return (false, "Invalid transaction token.", null);
        if (existing.Status == "CANCELLED") return (false, "Cannot complete a cancelled reservation.", null);
        if (existing.Status == "COMPLETED") return (false, "Reservation is already completed.", null);
        if (existing.Status != "APPROVED") return (false, "Only approved reservations can be completed.", null);
        
        existing.Status = "COMPLETED";
        existing.CompletedAt = DateTime.UtcNow;
        existing.UpdatedAt = DateTime.UtcNow;
        
        await SaveAsync(existing, existing.Version);
        return (true, "Reservation completed successfully.", existing);
    }
}