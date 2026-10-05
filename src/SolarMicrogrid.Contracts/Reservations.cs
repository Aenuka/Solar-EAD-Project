/*
 * File: Reservations.cs
 * Author: Sajith
 * Description: DTOs for reservation operations.
 */

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class ReservationInput
{
    [Required, StringLength(20, MinimumLength = 10)]
    public string ProsumerNic { get; set; } = string.Empty;

    [Required]
    public string StationId { get; set; } = string.Empty;

    [Required]
    public string SlotId { get; set; } = string.Empty;

    [JsonRequired]
    public DateTime ReservationDate { get; set; }

    [Range(0.01, 1000000)]
    public double EnergyAmountKwh { get; set; }

    [Required, StringLength(50)]
    public string TradingType { get; set; } = string.Empty;
}

public class UpdateReservationInput
{
    [Required]
    public string SlotId { get; set; } = string.Empty;

    [JsonRequired]
    public DateTime ReservationDate { get; set; }

    [Range(0.01, 1000000)]
    public double EnergyAmountKwh { get; set; }

    [Required, StringLength(50)]
    public string TradingType { get; set; } = string.Empty;
}

public class CancelReservationInput
{
    [StringLength(500)]
    public string? Reason { get; set; }
}

public class ReservationResponse
{
    public string Id { get; set; } = string.Empty;
    public string ReservationId { get; set; } = string.Empty;
    public string ProsumerNic { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string SlotId { get; set; } = string.Empty;
    public string? StationName { get; set; }
    public string? StationAddress { get; set; }
    public string? ProsumerName { get; set; }
    public DateTime? SlotStartsAt { get; set; }
    public DateTime? SlotEndsAt { get; set; }
    public DateTime ReservationDate { get; set; }
    public double EnergyAmountKwh { get; set; }
    public string TradingType { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public string? TransactionToken { get; set; }
    public string? CancellationReason { get; set; }
    public DateTime? CompletedAt { get; set; }
    public long? StationVersion { get; set; }
    public int AllocationSlots { get; set; }
}

public class ApprovedFutureCountResponse
{
    public int Count { get; set; }
}

public class OperatorDashboardResponse
{
    public int PendingCount { get; set; }
    public int ApprovedFutureCount { get; set; }
    public int CompletedCount { get; set; }
    public List<ReservationResponse> PendingReservations { get; set; } = new();
    public List<ReservationResponse> RecentCompletedReservations { get; set; } = new();
}
