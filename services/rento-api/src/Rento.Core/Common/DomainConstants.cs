namespace Rento.Core.Common;

public static class ListingStatus
{
    public const string Active = "active";
    public const string Pending = "pending";
    public const string Rejected = "rejected";
    public const string Inactive = "inactive";
    public const string Rented = "rented";
    public const string Archived = "archived";
}

public static class ListingDealType
{
    public const string Rent = "rent";
    public const string Sale = "sale";
    public const string Daily = "daily";
}

public static class Currency
{
    public const string Uzs = "UZS";
    public const string Usd = "USD";
}

public static class UserRole
{
    public const string Tenant = "tenant";
    public const string Landlord = "landlord";
    public const string Admin = "admin";
}

public static class UserLanguage
{
    public const string Uzbek = "uz";
    public const string Russian = "ru";
    public const string English = "en";
}

public static class UserSubscription
{
    public const string Free = "free";
    public const string Premium = "premium";
}

public static class MessageType
{
    public const string Text = "text";
    public const string Image = "image";
    public const string File = "file";
    public const string Location = "location";
    public const string Contact = "contact";
}

public static class DeviceType
{
    public const string Android = "android";
    public const string Ios = "ios";
}

public static class ReportStatus
{
    public const string Pending = "pending";
    public const string Resolved = "resolved";
    public const string Dismissed = "dismissed";
}

public static class MediaBucket
{
    public const string Listings = "listings";
    public const string Avatars = "avatars";
}
