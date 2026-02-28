using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using BenchmarkDotNet.Configs;
using Rento.Application.Common;

namespace Rento.Benchmarks;

[MemoryDiagnoser]
public class PaginatedResultBenchmarks
{
    private List<int> _items = null!;

    [Params(10, 100, 500)]
    public int N { get; set; }

    [GlobalSetup]
    public void Setup()
    {
        _items = Enumerable.Range(0, N).ToList();
    }

    [Benchmark(Baseline = true)]
    public PaginatedResult<int> Create_Manual()
    {
        var total = N * 2;
        var totalPages = (int)Math.Ceiling(total / (double)N);
        return new PaginatedResult<int>
        {
            Items = _items,
            Page = 1,
            PerPage = N,
            Total = total,
            TotalPages = totalPages
        };
    }

    [Benchmark]
    public PaginatedResult<int> Create_StaticFactory()
    {
        return PaginatedResult<int>.Create(_items, 1, N, N * 2);
    }
}

internal static class Program
{
    private static void Main(string[] args)
    {
        BenchmarkRunner.Run<PaginatedResultBenchmarks>(DefaultConfig.Instance);
    }
}
