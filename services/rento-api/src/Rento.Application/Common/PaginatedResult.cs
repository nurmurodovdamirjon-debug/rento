namespace Rento.Application.Common;

/// <summary>
/// Represents a paginated result set with items and paging metadata.
/// </summary>
/// <typeparam name="T">Item type.</typeparam>
public class PaginatedResult<T>
{
    /// <summary>Items on the current page.</summary>
    public List<T> Items { get; set; } = [];

    /// <summary>Current 1-based page number.</summary>
    public int Page { get; set; }

    /// <summary>Page size.</summary>
    public int PerPage { get; set; }

    /// <summary>Total item count.</summary>
    public int Total { get; set; }

    /// <summary>Total page count.</summary>
    public int TotalPages { get; set; }

    /// <summary>
    /// Creates a paginated result from items and total count.
    /// </summary>
    public static PaginatedResult<T> Create(IReadOnlyList<T> items, int page, int perPage, int total)
    {
        var totalPages = perPage > 0 ? (int)Math.Ceiling(total / (double)perPage) : 0;
        return new PaginatedResult<T>
        {
            Items = items as List<T> ?? items.ToList(),
            Page = page,
            PerPage = perPage,
            Total = total,
            TotalPages = totalPages
        };
    }
}
