package main

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/sony/gobreaker" // Circuit breaker library
	"github.com/streadway/amqp"
	_ "github.com/go-sql-driver/mysql"
)

// Global database connection and RabbitMQ variables
var db *sql.DB
var rabbitConn *amqp.Connection
var rabbitChannel *amqp.Channel
var queue amqp.Queue

// Circuit Breakers
var dbBreaker *gobreaker.CircuitBreaker
var rabbitBreaker *gobreaker.CircuitBreaker
var failureCount int
var circuitOpen bool
var lastFailureTime time.Time
const failureThreshold = 5              // Number of failures before circuit opens
const circuitResetTime = 10 * time.Second // Time to wait before trying again


// Album struct represents an album record in the database
type Album struct {
	Artist    string
	Title     string
	Year      int
	Image     []byte
	ImageSize int64
}

// Profile struct represents album information received in API requests
type Profile struct {
	Artist string `json:"artist"`
	Title  string `json:"title"`
	Year   string `json:"year"`
}

// Initialize RabbitMQ connection with a circuit breaker
func setupRabbitMQ() {
	var err error

	rabbitBreaker = gobreaker.NewCircuitBreaker(gobreaker.Settings{
		Name:        "RabbitMQ",
		MaxRequests: 5,
		Interval:    10 * time.Second,
		Timeout:     30 * time.Second,
		ReadyToTrip: func(counts gobreaker.Counts) bool {
			return counts.ConsecutiveFailures > 3
		},
	})

	_, err = rabbitBreaker.Execute(func() (interface{}, error) {
		rabbitConn, err = amqp.Dial("amqp://guest:guest@localhost:5672/")
		if err != nil {
			return nil, err
		}

		rabbitChannel, err = rabbitConn.Channel()
		if err != nil {
			return nil, err
		}

		queue, err = rabbitChannel.QueueDeclare(
			"album_queue", false, false, false, false, nil,
		)
		if err != nil {
			return nil, err
		}

		log.Println("RabbitMQ is set up and ready.")
		return nil, nil
	})

	if err != nil {
		log.Fatalf("Failed to initialize RabbitMQ: %v", err)
	}
}

// Publish message to RabbitMQ with circuit breaker
func publishToRabbitMQ(message string) {
	_, err := rabbitBreaker.Execute(func() (interface{}, error) {
		err := rabbitChannel.Publish(
			"", queue.Name, false, false,
			amqp.Publishing{
				ContentType: "text/plain",
				Body:        []byte(message),
			},
		)
		if err != nil {
			return nil, err
		}

		log.Printf("Published message to RabbitMQ: %s", message)
		return nil, nil
	})

	if err != nil {
		log.Printf("Circuit Breaker Open - Failed to publish message: %v", err)
	}
}

func main() {
	// Initialize database circuit breaker
	dbBreaker = gobreaker.NewCircuitBreaker(gobreaker.Settings{
		Name:        "MySQL",
		MaxRequests: 5,
		Interval:    10 * time.Second,
		Timeout:     30 * time.Second,
		ReadyToTrip: func(counts gobreaker.Counts) bool {
			return counts.ConsecutiveFailures > 3
		},
	})

	// Retrieve MySQL connection string
	dsn := os.Getenv("DB_DSN")
	if dsn == "" {
		log.Fatal("DB_DSN environment variable not set")
	}

	var err error
	_, err = dbBreaker.Execute(func() (interface{}, error) {
		db, err = sql.Open("mysql", dsn)
		if err != nil {
			return nil, err
		}

		err = db.Ping()
		if err != nil {
			return nil, err
		}

		_, err = db.Exec(`
			CREATE TABLE IF NOT EXISTS album (
				id INT AUTO_INCREMENT PRIMARY KEY,
				artist VARCHAR(255) NOT NULL,
				title VARCHAR(255) NOT NULL,
				year INT NOT NULL,
				image LONGBLOB NOT NULL,
				image_size INT NOT NULL
			) ENGINE=InnoDB;
		`)
		return nil, err
	})

	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}

	// Setup RabbitMQ
	setupRabbitMQ()
	defer rabbitConn.Close()
	defer rabbitChannel.Close()

	// Initialize Gin router
	r := gin.Default()

	// Health check endpoint
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok"})
	})

	// GET /album/:id - Retrieve an album by ID with circuit breaker
	r.GET("/album/:id", func(c *gin.Context) {
        if circuitOpen {
            if time.Since(lastFailureTime) > circuitResetTime {
                log.Println("Circuit breaker: transitioning to half-open state")
                circuitOpen = false  // Allow some requests
                failureCount = 0      // Reset failure count
            } else {
                c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Circuit breaker is open, try again later"})
                return
            }
        }

        id := c.Param("id")
        var album Album
        err := db.QueryRow("SELECT artist, title, year, image, image_size FROM album WHERE id = ?", id).
            Scan(&album.Artist, &album.Title, &album.Year, &album.Image, &album.ImageSize)

        if err != nil {
            failureCount++
            lastFailureTime = time.Now()

            if failureCount >= failureThreshold {
                log.Println("Circuit breaker: opened due to repeated failures")
                circuitOpen = true
            }
            c.JSON(http.StatusNotFound, gin.H{"error": "Album not found"})
            return
        }

        // Reset failure count on success
        failureCount = 0

        // Convert image to Base64 before returning
        albumBase64 := base64.StdEncoding.EncodeToString(album.Image)

        // Return album details in JSON format
        c.JSON(http.StatusOK, gin.H{
            "id":         id,
            "artist":     album.Artist,
            "title":      album.Title,
            "year":       album.Year,
            "image":      albumBase64,
            "image_size": album.ImageSize,
        })
    })


	// POST /album - Insert a new album with circuit breaker
	r.POST("/album", func(c *gin.Context) {
        if circuitOpen {
            if time.Since(lastFailureTime) > circuitResetTime {
                log.Println("Circuit breaker: transitioning to half-open state for POST")
                circuitOpen = false
                failureCount = 0
            } else {
                c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Circuit breaker is open, try again later"})
                return
            }
        }

        profileStr := c.PostForm("profile")
        if profileStr == "" {
            c.JSON(http.StatusBadRequest, gin.H{"error": "Missing profile field"})
            return
        }

        var profile Profile
        if err := json.Unmarshal([]byte(profileStr), &profile); err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid profile JSON"})
            return
        }

        file, err := c.FormFile("image")
        if err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": "Missing image file"})
            return
        }

        src, err := file.Open()
        if err != nil {
            c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to open image file"})
            return
        }
        defer src.Close()

        imageData, err := io.ReadAll(src)
        if err != nil {
            c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read image file"})
            return
        }
        imageSize := len(imageData)

        yearInt, err := strconv.Atoi(profile.Year)
        if err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid year value"})
            return
        }

        res, err := db.Exec(`
            INSERT INTO album (artist, title, year, image, image_size)
            VALUES (?, ?, ?, ?, ?)`,
            profile.Artist, profile.Title, yearInt, imageData, imageSize)

        if err != nil {
            failureCount++
            lastFailureTime = time.Now()

            if failureCount >= failureThreshold {
                log.Println("Circuit breaker: opened due to repeated failures on POST")
                circuitOpen = true
            }

            c.JSON(500, gin.H{"error": err.Error()})
            return
        }

        id, _ := res.LastInsertId()

        // Publish a message to RabbitMQ
        message := fmt.Sprintf("New Album Added: %s - %s (%d)", profile.Artist, profile.Title, yearInt)
        publishToRabbitMQ(message)

        // Reset failure count on success
        failureCount = 0

        c.JSON(200, gin.H{"message": "Album created", "albumId": id, "imageSize": imageSize})
    })


	// Start the server
    port := os.Getenv("PORT")
    if port == "" {
        port = "9090"
    }
    log.Printf("Server starting on port %s ...", port)

    // Make sure this is correctly formatted:
    r.Run(":" + port)
}
