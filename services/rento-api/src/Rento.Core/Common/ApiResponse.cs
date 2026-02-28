namespace Rento.Core.Common;

/// <summary>
/// Standard API response wrapper. JSON: success, data, error (code, message, details), meta.
/// </summary>
public class ApiResponse
{
    public bool Success { get; set; }
    public object? Data { get; set; }
    public ApiError? Error { get; set; }
    public ApiMeta? Meta { get; set; }
}

public class ApiError
{
    public string Code { get; set; } = "";
    public string Message { get; set; } = "";
    public object? Details { get; set; }
}

public class ApiMeta
{
    public int Page { get; set; }
    public int PerPage { get; set; }
    public int Total { get; set; }
    public int TotalPages { get; set; }
}

/// <summary>
/// Paginated data: items + meta inside data.
/// </summary>
public class PaginatedData<T>
{
    public List<T> Items { get; set; } = new();
    public ApiMeta Meta { get; set; } = new();
}
