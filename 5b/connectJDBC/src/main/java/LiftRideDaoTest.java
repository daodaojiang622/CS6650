public class LiftRideDaoTest {
    public static void main(String[] args) {
        LiftRideDao liftRideDao = new LiftRideDao();

        // Create a test LiftRide object
        LiftRide testLiftRide = new LiftRide(9, 2, 3, 5, 500, 20);

        // Insert into database
        liftRideDao.createLiftRide(testLiftRide);

        System.out.println("Test lift ride inserted successfully!");
    }
}
