import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ReviewConsumer {
    private static final String QUEUE_NAME = "likeQueue";
    private static java.sql.Connection dbConnection;

    public static void main(String[] argv) throws Exception {
        // Initialize MySQL connection
        Class.forName("com.mysql.cj.jdbc.Driver");
        dbConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/album_store?useSSL=false&allowPublicKeyRetrieval=true",
                "root",
                ""
        );

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println("✅ Waiting for review messages...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("📥 Received Review: " + message);

            // Parse message format: "albumId,reviewType,userId"
            String[] parts = message.split(",");
            int albumId = Integer.parseInt(parts[0]);
            String reviewType = parts[1];
            String userId = parts[2];

            // Insert into MySQL
            saveReviewToDatabase(albumId, reviewType, userId);

            // Acknowledge message processing
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }

    private static void saveReviewToDatabase(int albumId, String reviewType, String userId) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO album_reviews (album_id, user_id, review_type) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE review_type = VALUES(review_type)")) {
            stmt.setInt(1, albumId);
            stmt.setString(2, userId);
            stmt.setString(3, reviewType);
            stmt.executeUpdate();
            System.out.println("✅ Review saved to database: album_id=" + albumId + ", user_id=" + userId + ", review_type=" + reviewType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
