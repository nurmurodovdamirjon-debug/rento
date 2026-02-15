import { NextFunction, Request, Response } from 'express';

/**
 * Simple Prometheus-compatible metrics collector
 * prom-client kutilmagan dependency qo'shmaslik uchun o'zimiz yozamiz
 */

interface MetricBucket {
  le: number;
  count: number;
}

class SimpleMetrics {
  private requestCount: Record<string, number> = {};
  private requestDuration: Record<string, MetricBucket[]> = {};
  private errorCount: Record<string, number> = {};
  private startTime: number;

  constructor() {
    this.startTime = Date.now();
  }

  private buckets(): number[] {
    return [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10];
  }

  recordRequest(method: string, path: string, status: number, durationSec: number): void {
    const key = `${method}:${this.normalizePath(path)}:${status}`;
    this.requestCount[key] = (this.requestCount[key] || 0) + 1;

    const histKey = `${method}:${this.normalizePath(path)}`;
    if (!this.requestDuration[histKey]) {
      this.requestDuration[histKey] = this.buckets().map((le) => ({ le, count: 0 }));
    }
    for (const bucket of this.requestDuration[histKey]) {
      if (durationSec <= bucket.le) {
        bucket.count++;
      }
    }

    if (status >= 500) {
      const errKey = `${method}:${this.normalizePath(path)}`;
      this.errorCount[errKey] = (this.errorCount[errKey] || 0) + 1;
    }
  }

  private normalizePath(path: string): string {
    return path
      .replace(/\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi, '/:id')
      .replace(/\/\d+/g, '/:id');
  }

  toPrometheus(serviceName: string): string {
    const lines: string[] = [];
    const uptimeSec = (Date.now() - this.startTime) / 1000;

    lines.push('# HELP process_uptime_seconds Process uptime in seconds');
    lines.push('# TYPE process_uptime_seconds gauge');
    lines.push(`process_uptime_seconds{service="${serviceName}"} ${uptimeSec.toFixed(2)}`);
    lines.push('');

    const mem = process.memoryUsage();
    lines.push('# HELP process_resident_memory_bytes Resident memory size in bytes');
    lines.push('# TYPE process_resident_memory_bytes gauge');
    lines.push(`process_resident_memory_bytes{service="${serviceName}"} ${mem.rss}`);
    lines.push('');

    lines.push('# HELP process_heap_used_bytes Heap used size in bytes');
    lines.push('# TYPE process_heap_used_bytes gauge');
    lines.push(`process_heap_used_bytes{service="${serviceName}"} ${mem.heapUsed}`);
    lines.push('');

    lines.push('# HELP http_requests_total Total HTTP requests');
    lines.push('# TYPE http_requests_total counter');
    for (const [key, count] of Object.entries(this.requestCount)) {
      const [method, path, status] = key.split(':');
      lines.push(
        `http_requests_total{service="${serviceName}",method="${method}",path="${path}",status="${status}"} ${count}`
      );
    }
    lines.push('');

    lines.push('# HELP http_errors_total Total HTTP 5xx errors');
    lines.push('# TYPE http_errors_total counter');
    for (const [key, count] of Object.entries(this.errorCount)) {
      const [method, path] = key.split(':');
      lines.push(
        `http_errors_total{service="${serviceName}",method="${method}",path="${path}"} ${count}`
      );
    }
    lines.push('');

    lines.push('# HELP http_request_duration_seconds HTTP request duration in seconds');
    lines.push('# TYPE http_request_duration_seconds histogram');
    for (const [key, buckets] of Object.entries(this.requestDuration)) {
      const [method, path] = key.split(':');
      for (const bucket of buckets) {
        lines.push(
          `http_request_duration_seconds_bucket{service="${serviceName}",method="${method}",path="${path}",le="${bucket.le}"} ${bucket.count}`
        );
      }
      lines.push(
        `http_request_duration_seconds_bucket{service="${serviceName}",method="${method}",path="${path}",le="+Inf"} ${this.requestCount[`${key}:200`] || 0}`
      );
    }

    return lines.join('\n') + '\n';
  }
}

export const metrics = new SimpleMetrics();

export function metricsMiddleware(req: Request, res: Response, next: NextFunction): void {
  const start = process.hrtime.bigint();

  res.on('finish', () => {
    const durationNs = Number(process.hrtime.bigint() - start);
    const durationSec = durationNs / 1e9;
    metrics.recordRequest(req.method, req.originalUrl, res.statusCode, durationSec);
  });

  next();
}

export function metricsHandler(serviceName: string) {
  return (_req: Request, res: Response): void => {
    res.set('Content-Type', 'text/plain; version=0.0.4; charset=utf-8');
    res.send(metrics.toPrometheus(serviceName));
  };
}
