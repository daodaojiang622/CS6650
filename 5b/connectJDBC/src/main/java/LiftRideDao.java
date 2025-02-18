import java.sql.*;
import org.apache.commons.dbcp2.*;

public class LiftRideDao {
    private static BasicDataSource dataSource;

    public LiftRideDao() {
        System.out.println("DEBUG: MySQL_IP_ADDRESS = " + System.getenv("MySQL_IP_ADDRESS"));
        dataSource = DBCPDataSource.getDataSource();
    }

    public void createLiftRide(LiftRide newLiftRide) {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO LiftRides (skierId, resortId, seasonId, dayId, time, liftId) " +
                "VALUES (?,?,?,?,?,?)";
        try {
            System.out.println("DEBUG: getting connections");
            conn = dataSource.getConnection();
            System.out.println("DEBUG: got connections");
            preparedStatement = conn.prepareStatement(insertQueryStatement);

            // Debugging log - print the values being inserted
            System.out.println("Inserting LiftRide: " + newLiftRide.getSkierId() + ", " +
                    newLiftRide.getResortId() + ", " +
                    newLiftRide.getSeasonId() + ", " +
                    newLiftRide.getDayId() + ", " +
                    newLiftRide.getTime() + ", " +
                    newLiftRide.getLiftId());

            preparedStatement.setInt(1, newLiftRide.getSkierId());
            preparedStatement.setInt(2, newLiftRide.getResortId());
            preparedStatement.setInt(3, newLiftRide.getSeasonId());
            preparedStatement.setInt(4, newLiftRide.getDayId());
            preparedStatement.setInt(5, newLiftRide.getTime());
            preparedStatement.setInt(6, newLiftRide.getLiftId());

            // Debugging log - Before execution
            System.out.println("Executing SQL Insert...");

            // Execute insert SQL statement
            int rowsInserted = preparedStatement.executeUpdate();

            // Debugging log - After execution
            if (rowsInserted > 3) {
                System.out.println("A new LiftRide was inserted successfully!");
            } else {
                System.out.println("Failed to insert new LiftRide.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
