package main

import (
	"context"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/gin-gonic/gin"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
)

// Constants for AWS S3
const (
	useS3      = false              // Set to true to enable S3 uploads
	bucketName = "my-album-images"  // Replace with your actual S3 bucket name
)

// ErrorMessage represents the JSON error response
type ErrorMessage struct {
	Msg string `json:"msg"`
}

// AlbumInfo represents album details
type AlbumInfo struct {
	AlbumID   string `json:"albumID"`
	Artist    string `json:"artist"`
	Title     string `json:"title"`
	Year      string `json:"year"`
	ImagePath string `json:"imagePath,omitempty"` // Public URL or local path
}

// Uploads the image to Amazon S3 and returns the public URL
func uploadToS3(filename string, file multipart.File) (string, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		return "", err
	}

	s3Client := s3.NewFromConfig(cfg)

	bucket := bucketName // Assign to a variable to get its address

	_, err = s3Client.PutObject(context.TODO(), &s3.PutObjectInput{
		Bucket: &bucket,  // Now using a variable instead of a string constant
		Key:    &filename,
		Body:   file,
		ACL:    "public-read", // Allows public access
	})

	if err != nil {
		return "", err
	}

	return "https://" + bucket + ".s3.amazonaws.com/" + filename, nil
}

// Handles album creation (with optional image upload)
func createAlbum(c *gin.Context) {
	c.Writer.Header().Set("Content-Type", "application/json")

	albumID := c.Param("id")
	if albumID == "" {
		c.JSON(http.StatusBadRequest, ErrorMessage{Msg: "Album ID is required"})
		return
	}

	var imagePath string
	file, err := c.FormFile("image")

	if err == nil {
		if useS3 {
			// Upload to S3
			src, _ := file.Open()
			defer src.Close()

			imagePath, err = uploadToS3(file.Filename, src)
			if err != nil {
				c.JSON(http.StatusInternalServerError, ErrorMessage{Msg: "Failed to upload image to S3"})
				return
			}
		} else {
			// Save locally
			uploadDir := "./uploads"
			os.MkdirAll(uploadDir, os.ModePerm)

			savePath := filepath.Join(uploadDir, file.Filename)
			if err := c.SaveUploadedFile(file, savePath); err != nil {
				c.JSON(http.StatusInternalServerError, ErrorMessage{Msg: "Failed to save file"})
				return
			}

			imagePath = "http://" + c.Request.Host + "/uploads/" + file.Filename
		}
	}

	// Read form values
	artist := c.PostForm("profile[artist]")
	title := c.PostForm("profile[title]")
	year := c.PostForm("profile[year]")

	if artist == "" || title == "" || year == "" {
		c.JSON(http.StatusBadRequest, ErrorMessage{Msg: "Artist, Title, and Year are required"})
		return
	}

	// Create album response
	album := AlbumInfo{
		AlbumID:   albumID,
		Artist:    artist,
		Title:     title,
		Year:      year,
		ImagePath: imagePath,
	}

	c.JSON(http.StatusOK, album)
}

// Handles album retrieval
func getAlbum(c *gin.Context) {
	c.Writer.Header().Set("Content-Type", "application/json")

	albumID := c.Param("id")
	if albumID == "" {
		c.JSON(http.StatusBadRequest, ErrorMessage{Msg: "Album ID required"})
		return
	}

	// Return fixed album data for testing
	if albumID != "12345" {
		c.JSON(http.StatusNotFound, ErrorMessage{Msg: "Album not found"})
		return
	}

	album := AlbumInfo{
		AlbumID:   "12345",
		Artist:    "Sex Pistols",
		Title:     "Never Mind The Bollocks!",
		Year:      "1977",
		ImagePath: "http://" + c.Request.Host + "/uploads/test_image.jpg",
	}

	c.JSON(http.StatusOK, album)
}

func main() {
	r := gin.Default()

	// Serve uploaded images (if using local storage)
	if !useS3 {
		r.Static("/uploads", "./uploads")
	}

	// Routes
	r.POST("/albums/:id", createAlbum)
	r.GET("/albums/:id", getAlbum)

	// Start server on EC2 (binds to all interfaces)
	r.Run("0.0.0.0:9090")
}

