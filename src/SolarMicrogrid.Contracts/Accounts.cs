using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public static class Roles
{
    public const string Backoffice = "Backoffice";
    public const string GridOperator = "GridOperator";
    public const string Prosumer = "Prosumer";
    public const string Staff = Backoffice + "," + GridOperator;
}

public enum StaffRole { Backoffice, GridOperator }
public enum AccountStatus { Active, Inactive }
public enum RequestStatus { Pending, Approved, Rejected }
public enum Decision { Approved, Rejected }

public sealed class StaffLoginRequest
{
    public StaffLoginRequest() { }
    public StaffLoginRequest(string username, string password)
    {
        Username = username;
        Password = password;
    }

    [Required, StringLength(50)]
    public string Username { get; set; } = "";
    [Required, StringLength(128)]
    public string Password { get; set; } = "";
}

public sealed class ProsumerLoginRequest
{
    public ProsumerLoginRequest() { }
    public ProsumerLoginRequest(string nic, string password)
    {
        Nic = nic;
        Password = password;
    }

    [Required, StringLength(12)]
    public string Nic { get; set; } = "";
    [Required, StringLength(128)]
    public string Password { get; set; } = "";
}

public sealed class RegisterProsumerRequest
{
    public RegisterProsumerRequest() { }
    public RegisterProsumerRequest(string nic, string fullName, string email, string phone, string address, string password)
    {
        Nic = nic;
        FullName = fullName;
        Email = email;
        Phone = phone;
        Address = address;
        Password = password;
    }

    [Required, StringLength(12)]
    public string Nic { get; set; } = "";
    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";
    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")]
    public string Phone { get; set; } = "";
    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";
    [Required, StringLength(128, MinimumLength = 12)]
    public string Password { get; set; } = "";
}

public sealed class UpdateProfileRequest
{
    public UpdateProfileRequest() { }
    public UpdateProfileRequest(string fullName, string email, string phone, string address, long version)
    {
        FullName = fullName;
        Email = email;
        Phone = phone;
        Address = address;
        Version = version;
    }

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";
    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")]
    public string Phone { get; set; } = "";
    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}

public sealed class CreateStaffRequest
{
    public CreateStaffRequest() { }
    public CreateStaffRequest(string username, string fullName, string email, string password, StaffRole? role)
    {
        Username = username;
        FullName = fullName;
        Email = email;
        Password = password;
        Role = role;
    }

    [Required, RegularExpression(@"^[a-zA-Z0-9._-]{3,50}$")]
    public string Username { get; set; } = "";
    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";
    [Required, StringLength(128, MinimumLength = 12)]
    public string Password { get; set; } = "";
    [Required]
    public StaffRole? Role { get; set; }
}

public sealed class UpdateStaffRequest
{
    public UpdateStaffRequest() { }
    public UpdateStaffRequest(string fullName, string email, StaffRole? role, AccountStatus? status, long version)
    {
        FullName = fullName;
        Email = email;
        Role = role;
        Status = status;
        Version = version;
    }

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";
    [Required]
    public StaffRole? Role { get; set; }
    [Required]
    public AccountStatus? Status { get; set; }
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}

public sealed class DeactivationRequestInput
{
    public DeactivationRequestInput() { }
    public DeactivationRequestInput(string reason, long version)
    {
        Reason = reason;
        Version = version;
    }

    [Required, StringLength(500, MinimumLength = 5)]
    public string Reason { get; set; } = "";
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}

public sealed class DecisionRequest
{
    public DecisionRequest() { }
    public DecisionRequest(Decision? decision, string note, long version)
    {
        Decision = decision;
        Note = note;
        Version = version;
    }

    [Required]
    public Decision? Decision { get; set; }
    [Required, StringLength(500, MinimumLength = 5)]
    public string Note { get; set; } = "";
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}

public sealed class ReactivateRequest
{
    public ReactivateRequest() { }
    public ReactivateRequest(string note, long version)
    {
        Note = note;
        Version = version;
    }

    [Required, StringLength(500, MinimumLength = 5)]
    public string Note { get; set; } = "";
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}

public sealed record AuthResponse(string AccessToken, DateTimeOffset ExpiresAt, string TokenType,
    string Id, string FullName, string Role);
public sealed record CurrentUserResponse(string Id, string FullName, string Role);
public sealed record StaffResponse(string Id, string Username, string FullName, string Email,
    string Role, AccountStatus Status, bool IsProtected, long Version, DateTimeOffset CreatedAt);
public sealed record DeactivationResponse(string Id, string Reason, RequestStatus Status,
    DateTimeOffset RequestedAt, string? ReviewedBy, DateTimeOffset? ReviewedAt, string? DecisionNote);
public sealed record AccountEventResponse(string Action, string ActorId, string? Note, DateTimeOffset At);
public sealed record ProsumerResponse(string Nic, string FullName, string Email, string Phone,
    string Address, AccountStatus Status, long Version, DateTimeOffset CreatedAt,
    DeactivationResponse? DeactivationRequest, IReadOnlyList<AccountEventResponse> RecentEvents);
public sealed record PageResponse<T>(IReadOnlyList<T> Items, long Total, int Page, int PageSize);
public sealed record DashboardResponse(long ActiveProsumers, long InactiveProsumers, long PendingRequests, long StaffUsers);
