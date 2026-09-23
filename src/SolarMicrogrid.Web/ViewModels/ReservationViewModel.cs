namespace SolarMicrogrid.Web.ViewModels;

public class ReservationViewModel
{
    public string Id { get; set; } = string.Empty;
    public string ReservationId { get; set; } = string.Empty;
    public string ProsumerNic { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string SlotId { get; set; } = string.Empty;
    public DateTime ReservationDate { get; set; }
    public double EnergyAmountKwh { get; set; }
    public string Status { get; set; } = string.Empty;
}