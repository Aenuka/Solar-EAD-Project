namespace SolarMicrogrid.Contracts;

public static class Roles
{
    public const string Backoffice = "Backoffice";
    public const string GridOperator = "GridOperator";
    public const string Prosumer = "Prosumer";
    public const string Staff = Backoffice + "," + GridOperator;
}
