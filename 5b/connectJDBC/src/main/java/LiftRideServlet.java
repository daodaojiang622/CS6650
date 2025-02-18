import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * LiftRideServlet: Handles HTTP requests to insert lift ride data.
 */
@WebServlet("/skiers/*")
public class LiftRideServlet extends HttpServlet {
    private LiftRideDao liftRideDao;
    private Gson gson = new Gson();


    @Override
    public void init() throws ServletException {
        System.out.println("before init");
        liftRideDao = new LiftRideDao();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            System.out.println("Received POST request at LiftRideServlet");
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.split("/").length != 8) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid URL format. Expected /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}");
                return;
            }

            String[] pathParts = pathInfo.split("/");
            int resortId = Integer.parseInt(pathParts[1]);
            int seasonId = Integer.parseInt(pathParts[3]);
            int dayId = Integer.parseInt(pathParts[5]);
            int skierId = Integer.parseInt(pathParts[7]);

            System.out.println("resortId: " + resortId);
            System.out.println("seasonId: " + seasonId);
            System.out.println("dayId: " + dayId);
            System.out.println("skierId: " + skierId);

            BufferedReader reader = req.getReader();
            LiftRide requestLiftRide = gson.fromJson(reader, LiftRide.class);
            reader.close();

            LiftRide newLiftRide = new LiftRide(skierId, resortId, seasonId, dayId, requestLiftRide.getTime(), requestLiftRide.getLiftId());

            System.out.println("Inserting LiftRide: " + newLiftRide.getSkierId() + ", " +
                    newLiftRide.getResortId() + ", " +
                    newLiftRide.getSeasonId() + ", " +
                    newLiftRide.getDayId() + ", " +
                    newLiftRide.getTime() + ", " +
                    newLiftRide.getLiftId());

            liftRideDao.createLiftRide(newLiftRide);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("LiftRide inserted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error processing request: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("LiftRideServlet is running! Use POST to insert data.");
    }
}
