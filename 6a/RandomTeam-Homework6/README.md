## Server Setup
1. **Accessing the Server Directory:**

After following the initial setup instructions provided by Ruidi, navigate to the Go server directory:

```bash
cd /home/ec2-user/go-server
```

2. **Editing main.go:**

Open main.go with your favorite editor (e.g., Vim):

```bash
vim main.go
```

Copy and paste the provided code into main.go.

```
package main

import (
        "database/sql"
        "log"
        "io"
        "os"
        "net/http"
        "strconv"
        "encoding/json"

        "github.com/gin-gonic/gin"
        _ "github.com/go-sql-driver/mysql"
)

// We'll store a global *sql.DB for simplicity in a demo.
var db *sql.DB

type Album struct {
        Artist string
        Title string
        Year int
        Image []byte
        ImageSize int64
}

type Profile struct {
        Artist string `json:"artist"`
        Title string `json:"title"`
        Year string `json:"year"`
}

func main() {
        // Read MySQL DSN from environment variable, e.g.:
        // DB_DSN = "mydbuser:mydbpass123@tcp(demo-mysql.ctmce9oladmi.us-west-2.rds.amazonaws.com:3306)/mydemodb"
        dsn := os.Getenv("DB_DSN")
        if dsn == "" {
                log.Fatal("DB_DSN environment variable not set")
        }

        var err error
        db, err = sql.Open("mysql", dsn)
        if err != nil {
                log.Fatalf("Failed to open DB: %v", err)
        }

        // Test the DB connection quickly
        err = db.Ping()
        if err != nil {
                log.Fatalf("Failed to connect to DB: %v", err)
        }

        db.Exec(`
        CREATE TABLE IF NOT EXISTS album (
                id INT AUTO_INCREMENT PRIMARY KEY,
                artist VARCHAR(255) NOT NULL,
                title VARCHAR(255) NOT NULL,
                year INT NOT NULL,
                image LONGBLOB NOT NULL,
                image_size INT NOT NULL
        ) ENGINE=InnoDB;
        `)

        // Setup Gin engine
        r := gin.Default()

        // Health check route
        r.GET("/health", func(c *gin.Context) {
                c.JSON(200, gin.H{"status": "ok"})
        })

        // GET /count -> returns row count in "test_table"
        r.GET("/count", func(c *gin.Context) {
                var cnt int
                row := db.QueryRow("SELECT COUNT(*) FROM test_table")
                if err := row.Scan(&cnt); err != nil {
                        c.JSON(500, gin.H{"error": err.Error()})
                        return
                }
                c.JSON(200, gin.H{"row_count": cnt})
        })

        r.GET("/album/:albumId", func(c *gin.Context) {
                albumId := c.Param("albumId")
                id, err := strconv.Atoi(albumId)
                if err != nil {
                        c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid album value"})
                }

                var albumObj Album
                row := db.QueryRow("SELECT artist, title, year FROM album WHERE id = ?", id)
                if err := row.Scan(&albumObj.Artist, &albumObj.Title, &albumObj.Year); err != nil {
                        if err == sql.ErrNoRows {
                                c.JSON(http.StatusNotFound, gin.H{"error": "Album not found"})
                        } else {
                                c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
                        }
                        return
                }
                c.JSON(200, albumObj)
        })

        // POST /insert -> inserts a row with some random value
        r.POST("/album", func(c *gin.Context) {
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
                        c.JSON(500, gin.H{"error": err.Error()})
                        return
                }
                id, _ := res.LastInsertId()
                c.JSON(200, gin.H{"message": "Album created", "albumId": id, "imageSize": imageSize})
        })

        // Optionally, pass a port via environment variable or default to 8080
        port := os.Getenv("PORT")
        if port == "" {
                port = "8080"
        }

        log.Printf("Server starting on port %s ...", port)
        r.Run(":" + port)
}

```

3. **Terminating any Existing Instances:**

Before compiling, check for any running server instances:

```bash
ps aux | grep CS6650HW6_GO
```

Kill any existing instance using its PID:

```bash
kill <PID>
```

4. **Compiling the Server:**

Build the executable with:

```bash
go build -o CS6650HW6_GO main.go
```

5. **Set the DB_DSN Environment Variable:**
```
export DB_DSN="mydbuser:mydbpass123@tcp(demo-mysql.cfixvo8jjefs.us-west-2.rds.amazonaws.com:3306)/mydemodb"
```

6. **Running the Server:**
```
./CS6650HW6_GO
```

7**Deploy on Multiple Instances:**

Repeat the above steps on any additional demo EC2 instances as needed to simulate a distributed environment.

## Server Details
- Endpoints:

  - GET /health: Returns the server health status.
  - GET /album/:albumId: Retrieves album details (artist, title, and year) for a given album ID.
  - POST /album: Inserts a new album record into the database.
- Database Table:

The following SQL is executed on startup to ensure the necessary table exists: (already in the main.go)

```sql
CREATE TABLE IF NOT EXISTS album (
id INT AUTO_INCREMENT PRIMARY KEY,
artist VARCHAR(255) NOT NULL,
title VARCHAR(255) NOT NULL,
year INT NOT NULL,
image LONGBLOB NOT NULL,
image_size INT NOT NULL
) ENGINE=InnoDB;
```
## Client Setup
1. **Configuration:**

Before running the Java client, make sure to update:

- The IP address to point to the appropriate server.
- The file path if you are uploading an image.

2. **Execution:**

Once the above configurations are updated, compile and run the Java client to interact with the Go server.

