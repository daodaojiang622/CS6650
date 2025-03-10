import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(name = "Server", value = "/*")
@MultipartConfig
public class Server extends HttpServlet {
  private java.sql.Connection dbConnection;
  private static final String QUEUE_NAME = "likeQueue";

  @Override
  public void init() throws ServletException {
    try {
      // Initialize JDBC MySQL connection
      Class.forName("com.mysql.cj.jdbc.Driver");
      dbConnection = DriverManager.getConnection(
              "jdbc:mysql://localhost:3306/album_store?useSSL=false&allowPublicKeyRetrieval=true",
              "root",
              ""  // Empty password
      );

      createTables();
      System.out.println("Tables created successfully.");
    } catch (Exception e) {
      throw new ServletException("Database connection failed", e);
    }
  }

  private void createTables() throws SQLException {
    String createAlbumsTable = "CREATE TABLE IF NOT EXISTS albums ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "artist VARCHAR(255) NOT NULL,"
            + "title VARCHAR(255) NOT NULL,"
            + "year INT NOT NULL,"
            + "image LONGBLOB NOT NULL,"
            + "image_size BIGINT NOT NULL"
            + ")";

    String createReviewsTable = "CREATE TABLE IF NOT EXISTS album_reviews ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "album_id INT NOT NULL,"
            + "user_id VARCHAR(255) NOT NULL,"
            + "review_type ENUM('like', 'dislike'),"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE (album_id, user_id),"
            + "FOREIGN KEY (album_id) REFERENCES albums(id)"
            + ")";

    try (Statement stmt = dbConnection.createStatement()) {
      stmt.execute(createAlbumsTable);
      stmt.execute(createReviewsTable);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    response.setContentType("application/json");
    String url = request.getPathInfo();

    response.getWriter().write("DEBUG: Received URL path: " + url + "\n");

    if (url == null || url.equals("/albums")) {
      // Case 1: Get all albums
      getAllAlbums(response);
    } else {
      String[] urlParts = url.split("/");

      response.getWriter().write("DEBUG: Split URL parts: " + Arrays.toString(urlParts) + "\n");

      if (urlParts.length == 3) {
        // Case 2: Get specific album by ID
        getAlbumById(urlParts[2], response);
      } else if (urlParts.length == 4 && urlParts[3].equals("reviews")) {
        // Case 3: Get album reviews (like/dislike count)
        getAlbumReviews(urlParts[2], response);
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"error\": \"Invalid request\"}");
      }
    }
  }

  private void getAllAlbums(HttpServletResponse response) throws IOException {
    try (Statement stmt = dbConnection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT id, artist, title, year FROM albums")) {

      List<AlbumInfo> albums = new ArrayList<>();
      while (rs.next()) {
        albums.add(new AlbumInfo(
                rs.getString("artist"),
                rs.getString("title"),
                rs.getInt("year"),
                rs.getInt("id")
        ));
      }
      response.getWriter().write(new Gson().toJson(albums));

    } catch (SQLException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"error\": \"Database error: " + e.getMessage() + "\"}");
    }
  }

  private void getAlbumById(String albumId, HttpServletResponse response) throws IOException {
    try (PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT id, artist, title, year FROM albums WHERE id = ?")) {
      stmt.setInt(1, Integer.parseInt(albumId));
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        AlbumInfo album = new AlbumInfo(
                rs.getString("artist"),
                rs.getString("title"),
                rs.getInt("year"),
                rs.getInt("id")
        );
        response.getWriter().write(new Gson().toJson(album));
      } else {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("{\"error\": \"Album not found\"}");
      }

    } catch (SQLException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"error\": \"Database error: " + e.getMessage() + "\"}");
    }
  }

  private void getAlbumReviews(String albumId, HttpServletResponse response) throws IOException {
    try (PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT review_type, COUNT(*) as count FROM album_reviews WHERE album_id = ? GROUP BY review_type")) {
      stmt.setInt(1, Integer.parseInt(albumId));
      ResultSet rs = stmt.executeQuery();

      int likes = 0, dislikes = 0;
      while (rs.next()) {
        if ("like".equals(rs.getString("review_type"))) {
          likes = rs.getInt("count");
        } else if ("dislike".equals(rs.getString("review_type"))) {
          dislikes = rs.getInt("count");
        }
      }

      // Return JSON response
      String jsonResponse = "{\"albumId\": " + albumId + ", \"likes\": " + likes + ", \"dislikes\": " + dislikes + "}";
      response.getWriter().write(jsonResponse);

    } catch (SQLException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("{\"error\": \"Database error: " + e.getMessage() + "\"}");
    }
  }


  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    String url = request.getPathInfo();
    if (url == null || url.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("missing parameters");
      return;
    }

    if (url.equals("/IGORTON/AlbumStore/1.0.0/albums")) {
      handleAlbumUpload(request, response);
    } else if (url.startsWith("/review/")) {
      handleLikeDislike(request, response);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("invalid parameters");
    }
  }

  private void handleAlbumUpload(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    String artist = "", title = "", yearStr = "";
    byte[] imageData = null;
    long imageSize = 0;

    for (Part p : request.getParts()) {
      if (p.getName().equals("image")) {
        try (InputStream s = p.getInputStream()) {
          imageData = s.readAllBytes();
          imageSize = imageData.length;
        }
      }
      if (p.getName().equals("profile[artist]")) {
        artist = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }
      if (p.getName().equals("profile[title]")) {
        title = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }
      if (p.getName().equals("profile[year]")) {
        yearStr = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }
    }

    System.out.println("DEBUG: Received artist: " + artist);
    System.out.println("DEBUG: Received title: " + title);
    System.out.println("DEBUG: Received year: " + yearStr);

    try {
      PreparedStatement stmt = dbConnection.prepareStatement(
              "INSERT INTO albums (artist, title, year, image, image_size) VALUES (?, ?, ?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS
      );
      stmt.setString(1, artist);
      stmt.setString(2, title);
      stmt.setInt(3, Integer.parseInt(yearStr));
      stmt.setBytes(4, imageData);
      stmt.setLong(5, imageSize);
      stmt.executeUpdate();

      ResultSet generatedKeys = stmt.getGeneratedKeys();
      int albumId = generatedKeys.next() ? generatedKeys.getInt(1) : -1;

      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getWriter().write(new Gson().toJson(new AlbumInfo(artist, title, Integer.parseInt(yearStr), albumId)));
    } catch (SQLException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("Database error: " + e.getMessage());
    }
  }

  private void handleLikeDislike(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] parts = request.getPathInfo().split("/");
    if (parts.length != 4) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("Invalid like/dislike request format.");
      return;
    }

    String reviewType = parts[2]; // like or dislike
    int albumId = Integer.parseInt(parts[3]);

    // Extract user_id from request (for now, using a dummy user_id)
    String userId = request.getParameter("user_id"); // Example: ?user_id=12345
    if (userId == null || userId.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("Missing user_id");
      return;
    }

    try {
      // Publish to RabbitMQ
      publishToQueue(albumId, reviewType, userId);
      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getWriter().write("Review submitted.");
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write("Failed to publish review.");
    }
  }

  private void publishToQueue(int albumId, String reviewType, String userId) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      String message = albumId + "," + reviewType + "," + userId;
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Override
  public void destroy() {
    try {
      if (dbConnection != null) dbConnection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
