package database

import (
	"fmt"

	"github.com/elastic/go-elasticsearch/v8"
	"github.com/rs/zerolog/log"
)

// NewElasticClient — Elasticsearch ulanish
func NewElasticClient(url string) (*elasticsearch.Client, error) {
	cfg := elasticsearch.Config{
		Addresses: []string{url},
	}

	es, err := elasticsearch.NewClient(cfg)
	if err != nil {
		return nil, fmt.Errorf("elasticsearch client: %w", err)
	}

	// Ping — ulanishni tekshirish
	res, err := es.Info()
	if err != nil {
		return nil, fmt.Errorf("elasticsearch ping: %w", err)
	}
	defer res.Body.Close()

	if res.IsError() {
		return nil, fmt.Errorf("elasticsearch info error: %s", res.String())
	}

	log.Info().Str("url", url).Msg("Elasticsearch connected")
	return es, nil
}
