/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Represents a staff account with its username and protected bootstrap-account flag.
 */

using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

[BsonIgnoreExtraElements]
public sealed class StaffUser : AccountDocument
{
    public string Username { get; set; } = "";

    public bool IsProtected { get; set; }
}
