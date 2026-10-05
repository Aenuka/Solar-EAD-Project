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
    private readonly StationService _stationService;

    public ReservationService(IReservationRepository repository, StationService stationService)
    {
        _repository = repository;
        _stationService = stationService;
    }

    // ===== CREATE — 7-day rule + double booking + Chamithu allocation =====
    public async Task<(bool success, string message, EnergyReservation? reservation)> CreateAsync(ReservationInput dto)
    {
        dto.ReservationDate = dto.ReservationDate.ToUniversalTime();
        // Rule 1: Within 7 days
        if (dto.ReservationDate > DateTime.UtcNow.AddDays(7))
            return (false, "Reservation must be within 7 days.", null);

        if (dto.ReservationDate < DateTime.UtcNow)
            return (false, "Reservation date cannot be in the past.", null);

        // Rule 2: No double booking
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
            UpdatedAt = DateTime.UtcNow,
            AllocationSlots = 1
        };

        // ===== Chamithu Integration: Reserve station slot =====
        try
        {
            var stationVersion = await GetStationVersionAsync(dto.StationId);

            await _stationService.ReserveAsync(
                dto.StationId,
                dto.SlotId,
                new AllocationInput
                {
                    Version = stationVersion,
                    BookingId = reservation.ReservationId,
                    Slots = 1,
                    EnergyKwh = dto.EnergyAmountKwh
                },
                CancellationToken.None);

            reservation.StationVersion = stationVersion;
        }
        catch (Exception ex)
        {
            return (false, $"Failed to reserve station slot: {ex.Message}", null);
        }

        await _repository.CreateAsync(reservation);
        return (true, "Reservation created successfully.", reservation);
    }

    // ===== UPDATE — 12-hour rule + 7-day rule =====
    public async Task<(bool success, string message, EnergyReservation? reservation)> UpdateAsync(string id, UpdateReservationInput dto)
    {
        dto.ReservationDate = dto.ReservationDate.ToUniversalTime();
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

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

        try
        {
            var version = await GetStationVersionAsync(existing.StationId);
            var station = await _stationService.ModifyAllocationAsync(existing.StationId, existing.SlotId,
                dto.SlotId, new AllocationInput
                {
                    Version = version,
                    BookingId = existing.ReservationId,
                    Slots = existing.AllocationSlots,
                    EnergyKwh = dto.EnergyAmountKwh
                }, dto.ReservationDate, CancellationToken.None);
            existing.StationVersion = station.Version;
        }
        catch (Exception ex)
        {
            return (false, $"Could not update the energy window: {ex.Message}", null);
        }

        existing.SlotId = dto.SlotId;
        existing.ReservationDate = dto.ReservationDate;
        existing.EnergyAmountKwh = dto.EnergyAmountKwh;
        existing.TradingType = dto.TradingType;
        existing.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(existing);
        return (true, "Reservation updated successfully.", existing);
    }

    // ===== CANCEL — 12-hour rule + Chamithu release =====
    public async Task<(bool success, string message, EnergyReservation? reservation)> CancelAsync(string id, CancelReservationInput dto)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        if (existing.Status == "CANCELLED")
            return (false, "Reservation is already cancelled.", null);

        if (existing.Status == "COMPLETED")
            return (false, "Completed reservations cannot be cancelled.", null);

        var hoursUntil = (existing.ReservationDate - DateTime.UtcNow).TotalHours;
        if (hoursUntil < 12)
            return (false, "Cancellations require at least 12 hours notice.", null);

        // ===== Chamithu Integration: Release station slot =====
        try
        {
            // Fetch CURRENT station version (it changes on every mutation)
            var currentVersion = await GetStationVersionAsync(existing.StationId);

            await _stationService.EndAllocationAsync(
                existing.StationId,
                existing.SlotId,
                existing.ReservationId,
                new StationVersion { Version = currentVersion },
                complete: false,
                CancellationToken.None);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Warning: Failed to release station slot: {ex.Message}");
        }

        existing.Status = "CANCELLED";
        existing.CancellationReason = dto.Reason;
        existing.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(existing);
        return (true, "Reservation cancelled successfully.", existing);
    }

    // ===== HISTORY =====
    public async Task<List<EnergyReservation>> GetHistoryAsync(string nic)
        => await _repository.GetByProsumerNicAsync(nic);

    // ===== DASHBOARD (Operator) =====
    public async Task<(List<EnergyReservation> pending, int approvedFutureCount, List<EnergyReservation> completed)> GetDashboardDataAsync()
    {
        var pending = await _repository.GetPendingAsync();
        var approvedFuture = await _repository.GetApprovedFutureAsync();
        var completed = await _repository.SearchAsync("COMPLETED", null, null, null, null);
        
        return (pending, approvedFuture.Count, completed);
    }

    // ===== PENDING =====
    public async Task<List<EnergyReservation>> GetPendingAsync()
        => await _repository.GetPendingAsync();

    // ===== SEARCH =====
    public async Task<List<EnergyReservation>> SearchAsync(string? status, string? stationId, string? nic, DateTime? from, DateTime? to)
        => await _repository.SearchAsync(status, stationId, nic, from, to);

    // ===== APPROVED FUTURE COUNT =====
    public async Task<int> GetApprovedFutureCountAsync()
    {
        var list = await _repository.GetApprovedFutureAsync();
        return list.Count;
    }

    // ===== TRANSACTION TOKEN (QR) =====
    public async Task<(bool success, string message, EnergyReservation? reservation)> GetTransactionAsync(string id, string nic)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        if (existing.ProsumerNic != nic)
            return (false, "Unauthorized.", null);

        if (existing.Status != "APPROVED")
            return (false, "Only approved reservations can be used for QR verification.", null);

        try
        {
            var station = await _stationService.GetAsync(existing.StationId, isStaff: true, CancellationToken.None);
            if (station == null) return (false, "Station not found.", null);

            var slot = station.Slots.FirstOrDefault(s => s.Id == existing.SlotId);
            if (slot == null) return (false, "Slot not found.", null);

            if (DateTime.UtcNow > slot.EndsAt)
                return (false, "Reservation time has expired.", null);
        }
        catch (Exception ex)
        {
            return (false, $"Failed to verify station/slot: {ex.Message}", null);
        }

        if (string.IsNullOrEmpty(existing.TransactionToken))
        {
            existing.TransactionToken = Guid.NewGuid().ToString("N").ToUpper();
            existing.UpdatedAt = DateTime.UtcNow;
            await _repository.UpdateAsync(existing);
        }

        return (true, "Transaction token retrieved.", existing);
    }

    // ===== APPROVE =====
    /// <summary>
    /// Approves a pending reservation so it can be used for QR verification.
    /// </summary>
    public async Task<(bool success, string message, EnergyReservation? reservation)> ApproveAsync(string id)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        if (existing.Status == "APPROVED")
            return (false, "Reservation is already approved.", null);

        if (existing.Status != "PENDING")
            return (false, $"Cannot approve a {existing.Status} reservation.", null);

        existing.Status = "APPROVED";
        existing.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(existing);
        return (true, "Reservation approved.", existing);
    }

    // ===== VERIFY TOKEN =====
    /// <summary>
    /// Verifies a scanned transaction token against server-side reservation data.
    /// </summary>
    public async Task<(bool success, string message, EnergyReservation? reservation)> VerifyTokenAsync(string token)
    {
        if (string.IsNullOrWhiteSpace(token))
            return (false, "Token is required.", null);

        var existing = await _repository.GetByTokenAsync(token);
        if (existing is null)
            return (false, "Invalid transaction token.", null);

        if (existing.Status == "CANCELLED")
            return (false, "This reservation has been cancelled.", null);

        if (existing.Status == "COMPLETED")
            return (false, "This transaction has already been completed.", null);

        if (existing.Status == "PENDING")
            return (false, "This reservation is not approved.", null);

        if (existing.Status != "APPROVED")
            return (false, $"Reservation is {existing.Status}, cannot be verified.", null);

        try
        {
            var station = await _stationService.GetAsync(existing.StationId, isStaff: true, CancellationToken.None);
            if (station == null)
            {
                return (false, "Station not found.", null);
            }

            var slot = station.Slots.FirstOrDefault(s => s.Id == existing.SlotId);
            if (slot == null)
            {
                return (false, "Slot not found.", null);
            }

            if (DateTime.UtcNow < slot.StartsAt)
            {
                return (false, "Reservation window has not started.", null);
            }

            if (DateTime.UtcNow > slot.EndsAt)
            {
                return (false, "Reservation time has expired.", null);
            }

            var allocation = slot.Allocations.FirstOrDefault(a => a.BookingId == existing.ReservationId);
            if (allocation == null || allocation.Status != "Reserved")
            {
                return (false, "Reservation allocation could not be verified.", null);
            }
        }
        catch (Exception ex)
        {
            return (false, $"Failed to verify station/slot: {ex.Message}", null);
        }

        return (true, "Token verified.", existing);
    }

    // ===== COMPLETE =====
    /// <summary>
    /// Finalizes the energy transfer for an approved reservation.
    /// </summary>
    public async Task<(bool success, string message, EnergyReservation? reservation)> CompleteAsync(string id)
    {
        var existing = await _repository.GetByIdAsync(id);
        if (existing is null)
            return (false, "Reservation not found.", null);

        if (existing.Status == "COMPLETED")
            return (false, "This transaction has already been completed.", null);

        if (existing.Status == "CANCELLED")
            return (false, "This reservation has been cancelled.", null);

        if (existing.Status != "APPROVED")
            return (false, "This reservation is not approved.", null);

        long currentVersion;
        try
        {
            var station = await _stationService.GetAsync(existing.StationId, isStaff: true, CancellationToken.None);
            if (station == null) return (false, "Station information could not be verified.", null);
            currentVersion = station.Version;

            var slot = station.Slots.FirstOrDefault(s => s.Id == existing.SlotId);
            if (slot == null) return (false, "Reservation slot could not be verified.", null);

            var allocation = slot.Allocations.FirstOrDefault(a => a.BookingId == existing.ReservationId);
            if (allocation == null || allocation.Status != "Reserved") return (false, "Reservation allocation could not be verified.", null);

            if (DateTime.UtcNow < slot.StartsAt)
                return (false, "Reservation window has not started.", null);

            if (DateTime.UtcNow > slot.EndsAt)
                return (false, "Reservation time has expired.", null);
        }
        catch (Exception)
        {
            return (false, "Station information could not be verified.", null);
        }

        try
        {
            await _stationService.EndAllocationAsync(
                existing.StationId,
                existing.SlotId,
                existing.ReservationId,
                new StationVersion { Version = currentVersion },
                complete: true,
                CancellationToken.None);
        }
        catch (Exception ex)
        {
            return (false, ex.Message, null);
        }

        existing.Status = "COMPLETED";
        existing.CompletedAt = DateTime.UtcNow;
        existing.UpdatedAt = DateTime.UtcNow;

        await _repository.UpdateAsync(existing);
        return (true, "Energy transfer completed.", existing);
    }

    // ===== Helper: Get CURRENT station version for CAS =====
    /// <summary>
    /// Fetches the current station version from Chamithu's StationService.
    /// Required because the version increments on every station mutation.
    /// </summary>
    private async Task<long> GetStationVersionAsync(string stationId)
    {
        var station = await _stationService.GetAsync(stationId, isStaff: true, CancellationToken.None);
        if (station is null)
            throw new InvalidOperationException($"Station {stationId} not found.");

        return station.Version;
    }
}
