package middleware

import (
	"fmt"
	"regexp"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

// metricsStore — Prometheus-compatible metric yig'uvchi
type metricsStore struct {
	mu             sync.RWMutex
	requestCount   map[string]int64
	errorCount     map[string]int64
	durationBuckets map[string][]bucket
	startTime      time.Time
}

type bucket struct {
	Le    float64
	Count int64
}

var store = &metricsStore{
	requestCount:   make(map[string]int64),
	errorCount:     make(map[string]int64),
	durationBuckets: make(map[string][]bucket),
	startTime:      time.Now(),
}

var uuidRegex = regexp.MustCompile(`/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`)
var numericRegex = regexp.MustCompile(`/\d+`)

func defaultBuckets() []float64 {
	return []float64{0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10}
}

func normalizePath(path string) string {
	p := uuidRegex.ReplaceAllString(path, "/:id")
	p = numericRegex.ReplaceAllString(p, "/:id")
	return p
}

// MetricsMiddleware — har bir request uchun metrics yig'adi
func MetricsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()

		c.Next()

		duration := time.Since(start).Seconds()
		method := c.Request.Method
		path := normalizePath(c.FullPath())
		if path == "" {
			path = normalizePath(c.Request.URL.Path)
		}
		status := c.Writer.Status()

		store.mu.Lock()
		defer store.mu.Unlock()

		// Request count
		countKey := fmt.Sprintf("%s:%s:%d", method, path, status)
		store.requestCount[countKey]++

		// Error count
		if status >= 500 {
			errKey := fmt.Sprintf("%s:%s", method, path)
			store.errorCount[errKey]++
		}

		// Duration histogram
		histKey := fmt.Sprintf("%s:%s", method, path)
		if _, ok := store.durationBuckets[histKey]; !ok {
			buckets := make([]bucket, 0, len(defaultBuckets()))
			for _, le := range defaultBuckets() {
				buckets = append(buckets, bucket{Le: le, Count: 0})
			}
			store.durationBuckets[histKey] = buckets
		}
		for i := range store.durationBuckets[histKey] {
			if duration <= store.durationBuckets[histKey][i].Le {
				store.durationBuckets[histKey][i].Count++
			}
		}
	}
}

// MetricsHandler — /metrics endpoint uchun Prometheus format
func MetricsHandler() gin.HandlerFunc {
	return func(c *gin.Context) {
		store.mu.RLock()
		defer store.mu.RUnlock()

		var sb strings.Builder
		serviceName := "core-api"

		// Uptime
		uptime := time.Since(store.startTime).Seconds()
		sb.WriteString("# HELP process_uptime_seconds Process uptime in seconds\n")
		sb.WriteString("# TYPE process_uptime_seconds gauge\n")
		sb.WriteString(fmt.Sprintf("process_uptime_seconds{service=\"%s\"} %.2f\n\n", serviceName, uptime))

		// Request count
		sb.WriteString("# HELP http_requests_total Total HTTP requests\n")
		sb.WriteString("# TYPE http_requests_total counter\n")
		keys := sortedKeys(store.requestCount)
		for _, key := range keys {
			parts := strings.SplitN(key, ":", 3)
			if len(parts) == 3 {
				sb.WriteString(fmt.Sprintf(
					"http_requests_total{service=\"%s\",method=\"%s\",path=\"%s\",status=\"%s\"} %d\n",
					serviceName, parts[0], parts[1], parts[2], store.requestCount[key],
				))
			}
		}
		sb.WriteString("\n")

		// Error count
		sb.WriteString("# HELP http_errors_total Total HTTP 5xx errors\n")
		sb.WriteString("# TYPE http_errors_total counter\n")
		errKeys := sortedKeys(store.errorCount)
		for _, key := range errKeys {
			parts := strings.SplitN(key, ":", 2)
			if len(parts) == 2 {
				sb.WriteString(fmt.Sprintf(
					"http_errors_total{service=\"%s\",method=\"%s\",path=\"%s\"} %d\n",
					serviceName, parts[0], parts[1], store.errorCount[key],
				))
			}
		}
		sb.WriteString("\n")

		// Duration histogram
		sb.WriteString("# HELP http_request_duration_seconds HTTP request duration in seconds\n")
		sb.WriteString("# TYPE http_request_duration_seconds histogram\n")
		histKeys := sortedKeys(store.durationBuckets)
		for _, key := range histKeys {
			parts := strings.SplitN(key, ":", 2)
			if len(parts) == 2 {
				for _, b := range store.durationBuckets[key] {
					sb.WriteString(fmt.Sprintf(
						"http_request_duration_seconds_bucket{service=\"%s\",method=\"%s\",path=\"%s\",le=\"%.3f\"} %d\n",
						serviceName, parts[0], parts[1], b.Le, b.Count,
					))
				}
				sb.WriteString(fmt.Sprintf(
					"http_request_duration_seconds_bucket{service=\"%s\",method=\"%s\",path=\"%s\",le=\"+Inf\"} %d\n",
					serviceName, parts[0], parts[1], totalCount(store.requestCount, key),
				))
			}
		}

		c.Data(200, "text/plain; version=0.0.4; charset=utf-8", []byte(sb.String()))
	}
}

func sortedKeys[V any](m map[string]V) []string {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return keys
}

func totalCount(counts map[string]int64, prefix string) int64 {
	var total int64
	for key, count := range counts {
		if strings.HasPrefix(key, prefix+":") {
			total += count
		}
	}
	return total
}
