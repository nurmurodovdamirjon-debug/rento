package listing

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/elastic/go-elasticsearch/v8"
	"github.com/redis/go-redis/v9"
	"github.com/rs/zerolog/log"
)

const (
	esIndex         = "listings"
	searchCacheTTL  = 2 * time.Minute // QONUN 14 §2: 2 daqiqa TTL
)

// SearchRepo — Elasticsearch qidiruv repository
type SearchRepo struct {
	es    *elasticsearch.Client
	redis *redis.Client
}

// NewSearchRepo — yangi search repository
func NewSearchRepo(es *elasticsearch.Client, redis *redis.Client) *SearchRepo {
	return &SearchRepo{es: es, redis: redis}
}

// ===== INDEX MANAGEMENT =====

// EnsureIndex — listings indeksini yaratish (agar mavjud bo'lmasa)
func (s *SearchRepo) EnsureIndex(ctx context.Context) error {
	res, err := s.es.Indices.Exists([]string{esIndex})
	if err != nil {
		return fmt.Errorf("check index exists: %w", err)
	}
	defer res.Body.Close()

	// Indeks allaqachon mavjud
	if !res.IsError() {
		log.Info().Str("index", esIndex).Msg("Elasticsearch index already exists")
		return nil
	}

	// Mapping yaratish
	mapping := s.buildIndexMapping()
	body, _ := json.Marshal(mapping)

	res, err = s.es.Indices.Create(
		esIndex,
		s.es.Indices.Create.WithBody(bytes.NewReader(body)),
	)
	if err != nil {
		return fmt.Errorf("create index: %w", err)
	}
	defer res.Body.Close()

	if res.IsError() {
		bodyBytes, _ := io.ReadAll(res.Body)
		return fmt.Errorf("create index error: %s", string(bodyBytes))
	}

	log.Info().Str("index", esIndex).Msg("Elasticsearch index created")
	return nil
}

// buildIndexMapping — ES indeks xaritasi (mapping)
func (s *SearchRepo) buildIndexMapping() map[string]interface{} {
	return map[string]interface{}{
		"settings": map[string]interface{}{
			"number_of_shards":   1,
			"number_of_replicas": 0,
			"analysis": map[string]interface{}{
				"analyzer": map[string]interface{}{
					"uzbek_analyzer": map[string]interface{}{
						"type":      "custom",
						"tokenizer": "standard",
						"filter":    []string{"lowercase", "asciifolding"},
					},
				},
			},
		},
		"mappings": map[string]interface{}{
			"properties": map[string]interface{}{
				"id":       map[string]interface{}{"type": "keyword"},
				"user_id":  map[string]interface{}{"type": "keyword"},
				"title": map[string]interface{}{
					"type":     "text",
					"analyzer": "uzbek_analyzer",
					"fields": map[string]interface{}{
						"keyword": map[string]interface{}{"type": "keyword"},
					},
				},
				"description": map[string]interface{}{
					"type":     "text",
					"analyzer": "uzbek_analyzer",
				},
				"type":      map[string]interface{}{"type": "keyword"},
				"deal_type": map[string]interface{}{"type": "keyword"},
				"city": map[string]interface{}{
					"type": "text",
					"fields": map[string]interface{}{
						"keyword": map[string]interface{}{"type": "keyword"},
					},
				},
				"district": map[string]interface{}{
					"type": "text",
					"fields": map[string]interface{}{
						"keyword": map[string]interface{}{"type": "keyword"},
					},
				},
				"address": map[string]interface{}{
					"type":     "text",
					"analyzer": "uzbek_analyzer",
				},
				"landmark": map[string]interface{}{
					"type":     "text",
					"analyzer": "uzbek_analyzer",
				},
				"location":       map[string]interface{}{"type": "geo_point"},
				"rooms":          map[string]interface{}{"type": "integer"},
				"floor":          map[string]interface{}{"type": "integer"},
				"total_floors":   map[string]interface{}{"type": "integer"},
				"area_sqm":       map[string]interface{}{"type": "float"},
				"price":          map[string]interface{}{"type": "float"},
				"currency":       map[string]interface{}{"type": "keyword"},
				"has_furniture":  map[string]interface{}{"type": "boolean"},
				"has_appliances": map[string]interface{}{"type": "boolean"},
				"has_internet":   map[string]interface{}{"type": "boolean"},
				"has_parking":    map[string]interface{}{"type": "boolean"},
				"has_conditioner": map[string]interface{}{"type": "boolean"},
				"allows_pets":    map[string]interface{}{"type": "boolean"},
				"allows_children": map[string]interface{}{"type": "boolean"},
				"status":         map[string]interface{}{"type": "keyword"},
				"is_premium":     map[string]interface{}{"type": "boolean"},
				"premium_until":  map[string]interface{}{"type": "date"},
				"views_count":    map[string]interface{}{"type": "integer"},
				"favorites_count": map[string]interface{}{"type": "integer"},
				"image_count":    map[string]interface{}{"type": "integer"},
				"published_at":   map[string]interface{}{"type": "date"},
				"created_at":     map[string]interface{}{"type": "date"},
			},
		},
	}
}

// ===== DOCUMENT INDEXING =====

// ListingDocument — ES ga saqlanadigan hujjat
type ListingDocument struct {
	ID              string      `json:"id"`
	UserID          string      `json:"user_id"`
	Title           string      `json:"title"`
	Description     string      `json:"description,omitempty"`
	Type            string      `json:"type"`
	DealType        string      `json:"deal_type"`
	City            string      `json:"city"`
	District        string      `json:"district,omitempty"`
	Address         string      `json:"address,omitempty"`
	Landmark        string      `json:"landmark,omitempty"`
	Location        *GeoPoint   `json:"location,omitempty"`
	Rooms           *int        `json:"rooms,omitempty"`
	Floor           *int        `json:"floor,omitempty"`
	TotalFloors     *int        `json:"total_floors,omitempty"`
	AreaSqm         *float64    `json:"area_sqm,omitempty"`
	Price           float64     `json:"price"`
	Currency        string      `json:"currency"`
	HasFurniture    bool        `json:"has_furniture"`
	HasAppliances   bool        `json:"has_appliances"`
	HasInternet     bool        `json:"has_internet"`
	HasParking      bool        `json:"has_parking"`
	HasConditioner  bool        `json:"has_conditioner"`
	AllowsPets      bool        `json:"allows_pets"`
	AllowsChildren  bool        `json:"allows_children"`
	Status          string      `json:"status"`
	IsPremium       bool        `json:"is_premium"`
	PremiumUntil    *time.Time  `json:"premium_until,omitempty"`
	ViewsCount      int         `json:"views_count"`
	FavoritesCount  int         `json:"favorites_count"`
	ImageCount      int         `json:"image_count"`
	PublishedAt     *time.Time  `json:"published_at,omitempty"`
	CreatedAt       time.Time   `json:"created_at"`
}

// GeoPoint — ES geo_point
type GeoPoint struct {
	Lat float64 `json:"lat"`
	Lon float64 `json:"lon"`
}

// ListingToDocument — Listing modelini ES hujjatiga aylantirish
func ListingToDocument(l *Listing, imageCount int) *ListingDocument {
	doc := &ListingDocument{
		ID:             l.ID.String(),
		UserID:         l.UserID.String(),
		Title:          l.Title,
		Type:           l.Type,
		DealType:       l.DealType,
		City:           l.City,
		Price:          l.Price,
		Currency:       l.Currency,
		HasFurniture:   l.HasFurniture,
		HasAppliances:  l.HasAppliances,
		HasInternet:    l.HasInternet,
		HasParking:     l.HasParking,
		HasConditioner: l.HasConditioner,
		AllowsPets:     l.AllowsPets,
		AllowsChildren: l.AllowsChildren,
		Status:         l.Status,
		IsPremium:      l.IsPremium,
		ViewsCount:     l.ViewsCount,
		FavoritesCount: l.FavoritesCount,
		ImageCount:     imageCount,
		CreatedAt:      l.CreatedAt,
	}

	if l.Description.Valid {
		doc.Description = l.Description.String
	}
	if l.District.Valid {
		doc.District = l.District.String
	}
	if l.Address.Valid {
		doc.Address = l.Address.String
	}
	if l.Landmark.Valid {
		doc.Landmark = l.Landmark.String
	}
	if l.Latitude.Valid && l.Longitude.Valid {
		doc.Location = &GeoPoint{Lat: l.Latitude.Float64, Lon: l.Longitude.Float64}
	}
	if l.Rooms.Valid {
		v := int(l.Rooms.Int16)
		doc.Rooms = &v
	}
	if l.Floor.Valid {
		v := int(l.Floor.Int16)
		doc.Floor = &v
	}
	if l.TotalFloors.Valid {
		v := int(l.TotalFloors.Int16)
		doc.TotalFloors = &v
	}
	if l.AreaSqm.Valid {
		doc.AreaSqm = &l.AreaSqm.Float64
	}
	if l.PremiumUntil.Valid {
		doc.PremiumUntil = &l.PremiumUntil.Time
	}
	if l.PublishedAt.Valid {
		doc.PublishedAt = &l.PublishedAt.Time
	}

	return doc
}

// IndexListing — e'lonni ES ga indekslash
func (s *SearchRepo) IndexListing(ctx context.Context, doc *ListingDocument) error {
	body, err := json.Marshal(doc)
	if err != nil {
		return fmt.Errorf("marshal listing doc: %w", err)
	}

	res, err := s.es.Index(
		esIndex,
		bytes.NewReader(body),
		s.es.Index.WithDocumentID(doc.ID),
		s.es.Index.WithRefresh("false"),
	)
	if err != nil {
		return fmt.Errorf("index listing: %w", err)
	}
	defer res.Body.Close()

	if res.IsError() {
		bodyBytes, _ := io.ReadAll(res.Body)
		return fmt.Errorf("index listing error: %s", string(bodyBytes))
	}

	return nil
}

// DeleteDocument — e'lonni ES dan o'chirish
func (s *SearchRepo) DeleteDocument(ctx context.Context, id string) error {
	res, err := s.es.Delete(esIndex, id)
	if err != nil {
		return fmt.Errorf("delete document: %w", err)
	}
	defer res.Body.Close()
	return nil
}

// ===== SEARCH =====

// SearchFilter — qidiruv filtri
type SearchFilter struct {
	Q            string   `form:"q"`
	City         string   `form:"city"`
	District     string   `form:"district"`
	Type         string   `form:"type"`
	DealType     string   `form:"deal_type"`
	RoomsMin     *int     `form:"rooms_min"`
	RoomsMax     *int     `form:"rooms_max"`
	PriceMin     *float64 `form:"price_min"`
	PriceMax     *float64 `form:"price_max"`
	Currency     string   `form:"currency"`
	HasFurniture *bool    `form:"has_furniture"`
	HasParking   *bool    `form:"has_parking"`
	AllowsPets   *bool    `form:"allows_pets"`
	Lat          *float64 `form:"lat"`
	Lng          *float64 `form:"lng"`
	RadiusKm     *int     `form:"radius_km"`
	Sort         string   `form:"sort"`
	Page         int      `form:"page,default=1"`
	PerPage      int      `form:"per_page,default=20"`
}

// SearchResult — qidiruv natijasi
type SearchResult struct {
	IDs   []string `json:"ids"`
	Total int      `json:"total"`
}

// Search — Elasticsearch da qidiruv (text + filter + geo + scoring)
func (s *SearchRepo) Search(ctx context.Context, filter *SearchFilter) (*SearchResult, error) {
	// Cache kalit
	cacheKey := s.buildCacheKey(filter)
	if cached, err := s.getFromCache(ctx, cacheKey); err == nil {
		return cached, nil
	}

	// Pagination
	if filter.Page < 1 {
		filter.Page = 1
	}
	if filter.PerPage < 1 || filter.PerPage > 50 {
		filter.PerPage = 20
	}
	from := (filter.Page - 1) * filter.PerPage

	// Query qurilishi
	query := s.buildSearchQuery(filter)

	var buf bytes.Buffer
	if err := json.NewEncoder(&buf).Encode(query); err != nil {
		return nil, fmt.Errorf("encode search query: %w", err)
	}

	res, err := s.es.Search(
		s.es.Search.WithContext(ctx),
		s.es.Search.WithIndex(esIndex),
		s.es.Search.WithBody(&buf),
		s.es.Search.WithFrom(from),
		s.es.Search.WithSize(filter.PerPage),
		s.es.Search.WithTrackTotalHits(true),
	)
	if err != nil {
		return nil, fmt.Errorf("elasticsearch search: %w", err)
	}
	defer res.Body.Close()

	if res.IsError() {
		bodyBytes, _ := io.ReadAll(res.Body)
		return nil, fmt.Errorf("search error: %s", string(bodyBytes))
	}

	// Natijalarni o'qish
	var esResp struct {
		Hits struct {
			Total struct {
				Value int `json:"value"`
			} `json:"total"`
			Hits []struct {
				ID     string  `json:"_id"`
				Score  float64 `json:"_score"`
			} `json:"hits"`
		} `json:"hits"`
	}

	if err := json.NewDecoder(res.Body).Decode(&esResp); err != nil {
		return nil, fmt.Errorf("decode search response: %w", err)
	}

	ids := make([]string, len(esResp.Hits.Hits))
	for i, hit := range esResp.Hits.Hits {
		ids[i] = hit.ID
	}

	result := &SearchResult{
		IDs:   ids,
		Total: esResp.Hits.Total.Value,
	}

	// Cache ga saqlash (2 min TTL)
	s.saveToCache(ctx, cacheKey, result)

	return result, nil
}

// buildSearchQuery — ES qidiruv so'rovi (scoring API docs asosida)
func (s *SearchRepo) buildSearchQuery(filter *SearchFilter) map[string]interface{} {
	must := []map[string]interface{}{}
	filterClauses := []map[string]interface{}{
		{"term": map[string]interface{}{"status": "active"}},
	}
	should := []map[string]interface{}{}

	// Matnli qidiruv (q parametri)
	if filter.Q != "" {
		must = append(must, map[string]interface{}{
			"multi_match": map[string]interface{}{
				"query":    filter.Q,
				"fields":   []string{"title^3", "description^1", "city^2", "district^2", "address", "landmark"},
				"type":     "best_fields",
				"fuzziness": "AUTO",
			},
		})
	}

	// === FILTRLAR ===
	if filter.City != "" {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"city.keyword": strings.ToLower(filter.City)},
		})
	}
	if filter.District != "" {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"district.keyword": filter.District},
		})
	}
	if filter.Type != "" {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"type": filter.Type},
		})
	}
	if filter.DealType != "" {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"deal_type": filter.DealType},
		})
	}
	if filter.Currency != "" {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"currency": filter.Currency},
		})
	}

	// Narx diapazoni
	if filter.PriceMin != nil || filter.PriceMax != nil {
		priceRange := map[string]interface{}{}
		if filter.PriceMin != nil {
			priceRange["gte"] = *filter.PriceMin
		}
		if filter.PriceMax != nil {
			priceRange["lte"] = *filter.PriceMax
		}
		filterClauses = append(filterClauses, map[string]interface{}{
			"range": map[string]interface{}{"price": priceRange},
		})
	}

	// Xonalar diapazoni
	if filter.RoomsMin != nil || filter.RoomsMax != nil {
		roomsRange := map[string]interface{}{}
		if filter.RoomsMin != nil {
			roomsRange["gte"] = *filter.RoomsMin
		}
		if filter.RoomsMax != nil {
			roomsRange["lte"] = *filter.RoomsMax
		}
		filterClauses = append(filterClauses, map[string]interface{}{
			"range": map[string]interface{}{"rooms": roomsRange},
		})
	}

	// Boolean filtrlar
	if filter.HasFurniture != nil {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"has_furniture": *filter.HasFurniture},
		})
	}
	if filter.HasParking != nil {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"has_parking": *filter.HasParking},
		})
	}
	if filter.AllowsPets != nil {
		filterClauses = append(filterClauses, map[string]interface{}{
			"term": map[string]interface{}{"allows_pets": *filter.AllowsPets},
		})
	}

	// Geo filtr
	if filter.Lat != nil && filter.Lng != nil {
		radiusKm := 5
		if filter.RadiusKm != nil && *filter.RadiusKm > 0 {
			radiusKm = *filter.RadiusKm
		}
		filterClauses = append(filterClauses, map[string]interface{}{
			"geo_distance": map[string]interface{}{
				"distance": fmt.Sprintf("%dkm", radiusKm),
				"location": map[string]interface{}{
					"lat": *filter.Lat,
					"lon": *filter.Lng,
				},
			},
		})
	}

	// === SCORING (API docs asosida) ===
	// Premium e'lonlar → +1000 ball
	should = append(should, map[string]interface{}{
		"term": map[string]interface{}{
			"is_premium": map[string]interface{}{
				"value": true,
				"boost": 1000,
			},
		},
	})

	// Rasm soni > 5 → +50 ball
	should = append(should, map[string]interface{}{
		"range": map[string]interface{}{
			"image_count": map[string]interface{}{
				"gt":    5,
				"boost": 50,
			},
		},
	})

	// Yangi (7 kun ichida) → +30 ball
	should = append(should, map[string]interface{}{
		"range": map[string]interface{}{
			"created_at": map[string]interface{}{
				"gte":   "now-7d/d",
				"boost": 30,
			},
		},
	})

	// Ko'rishlar soni → boost (views > 100 → +10 ball)
	should = append(should, map[string]interface{}{
		"range": map[string]interface{}{
			"views_count": map[string]interface{}{
				"gt":    100,
				"boost": 10,
			},
		},
	})

	// Bool query
	boolQuery := map[string]interface{}{
		"filter": filterClauses,
		"should": should,
	}
	if len(must) > 0 {
		boolQuery["must"] = must
	}

	query := map[string]interface{}{
		"query": map[string]interface{}{
			"bool": boolQuery,
		},
	}

	// === SARALASH ===
	switch filter.Sort {
	case "price_asc":
		query["sort"] = []map[string]interface{}{
			{"is_premium": map[string]interface{}{"order": "desc"}},
			{"price": map[string]interface{}{"order": "asc"}},
		}
	case "price_desc":
		query["sort"] = []map[string]interface{}{
			{"is_premium": map[string]interface{}{"order": "desc"}},
			{"price": map[string]interface{}{"order": "desc"}},
		}
	case "date_desc":
		query["sort"] = []map[string]interface{}{
			{"is_premium": map[string]interface{}{"order": "desc"}},
			{"created_at": map[string]interface{}{"order": "desc"}},
		}
	default:
		// "relevance" — ES _score bo'yicha (default)
		if filter.Lat != nil && filter.Lng != nil {
			// Geo yaqinlik bo'yicha ham saralash
			query["sort"] = []map[string]interface{}{
				{"is_premium": map[string]interface{}{"order": "desc"}},
				{"_score": map[string]interface{}{"order": "desc"}},
				{"_geo_distance": map[string]interface{}{
					"location": map[string]interface{}{
						"lat": *filter.Lat,
						"lon": *filter.Lng,
					},
					"order": "asc",
					"unit":  "km",
				}},
			}
		}
	}

	return query
}

// ===== CACHE =====

// buildCacheKey — qidiruv filtri uchun cache kaliti
func (s *SearchRepo) buildCacheKey(filter *SearchFilter) string {
	data, _ := json.Marshal(filter)
	return fmt.Sprintf("search:listings:%x", data)
}

// getFromCache — cache dan olish
func (s *SearchRepo) getFromCache(ctx context.Context, key string) (*SearchResult, error) {
	data, err := s.redis.Get(ctx, key).Bytes()
	if err != nil {
		return nil, err
	}

	var result SearchResult
	if err := json.Unmarshal(data, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

// saveToCache — cache ga saqlash
func (s *SearchRepo) saveToCache(ctx context.Context, key string, result *SearchResult) {
	data, err := json.Marshal(result)
	if err != nil {
		return
	}
	s.redis.Set(ctx, key, data, searchCacheTTL)
}
