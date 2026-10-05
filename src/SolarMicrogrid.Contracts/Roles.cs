/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines shared Backoffice, GridOperator, and Prosumer role names used for authorization.
 */

namespace SolarMicrogrid.Contracts;

public static class Roles
{
    public const string Backoffice = "Backoffice";
    public const string GridOperator = "GridOperator";
    public const string Prosumer = "Prosumer";
    public const string Staff = Backoffice + "," + GridOperator;
}
