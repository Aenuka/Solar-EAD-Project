using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

public sealed class ScheduleInput : StationVersion
{
    [Required, MinLength(1), MaxLength(7)]
    public int[] Days { get; set; } = [];

    [Required, RegularExpression("^([01][0-9]|2[0-3]):[0-5][0-9]$")]
    public string OpensAt { get; set; } = "08:00";

    [Required, RegularExpression("^([01][0-9]|2[0-3]):[0-5][0-9]$")]
    public string ClosesAt { get; set; } = "18:00";
}
