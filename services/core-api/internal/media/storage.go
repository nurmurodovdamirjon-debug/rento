package media

import (
	"context"
	"fmt"
	"io"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"github.com/rs/zerolog/log"
)

const (
	BucketListings = "listings"
	BucketAvatars  = "avatars"
)

// Storage — MinIO fayl saqlash
type Storage struct {
	client   *minio.Client
	endpoint string
	useSSL   bool
}

// NewStorage — MinIO ulanish
func NewStorage(endpoint, accessKey, secretKey string, useSSL bool) (*Storage, error) {
	client, err := minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(accessKey, secretKey, ""),
		Secure: useSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("minio connect: %w", err)
	}

	return &Storage{
		client:   client,
		endpoint: endpoint,
		useSSL:   useSSL,
	}, nil
}

// EnsureBuckets — barcha kerakli bucketlarni yaratish
func (s *Storage) EnsureBuckets(ctx context.Context) error {
	buckets := []string{BucketListings, BucketAvatars}

	for _, bucket := range buckets {
		exists, err := s.client.BucketExists(ctx, bucket)
		if err != nil {
			return fmt.Errorf("check bucket %s: %w", bucket, err)
		}
		if !exists {
			if err := s.client.MakeBucket(ctx, bucket, minio.MakeBucketOptions{}); err != nil {
				return fmt.Errorf("create bucket %s: %w", bucket, err)
			}
			log.Info().Str("bucket", bucket).Msg("MinIO bucket created")

			// Public read policy
			policy := fmt.Sprintf(`{
				"Version": "2012-10-17",
				"Statement": [{
					"Effect": "Allow",
					"Principal": {"AWS": ["*"]},
					"Action": ["s3:GetObject"],
					"Resource": ["arn:aws:s3:::%s/*"]
				}]
			}`, bucket)
			if err := s.client.SetBucketPolicy(ctx, bucket, policy); err != nil {
				log.Warn().Err(err).Str("bucket", bucket).Msg("Failed to set public policy")
			}
		}
	}

	return nil
}

// Upload — faylni MinIO ga yuklash
func (s *Storage) Upload(ctx context.Context, bucket, objectName string, reader io.Reader, size int64, contentType string) (string, error) {
	_, err := s.client.PutObject(ctx, bucket, objectName, reader, size, minio.PutObjectOptions{
		ContentType: contentType,
	})
	if err != nil {
		return "", fmt.Errorf("upload %s/%s: %w", bucket, objectName, err)
	}

	url := s.GetURL(bucket, objectName)
	return url, nil
}

// Delete — faylni o'chirish
func (s *Storage) Delete(ctx context.Context, bucket, objectName string) error {
	return s.client.RemoveObject(ctx, bucket, objectName, minio.RemoveObjectOptions{})
}

// GetURL — fayl URL sini qaytarish
func (s *Storage) GetURL(bucket, objectName string) string {
	scheme := "http"
	if s.useSSL {
		scheme = "https"
	}
	return fmt.Sprintf("%s://%s/%s/%s", scheme, s.endpoint, bucket, objectName)
}
