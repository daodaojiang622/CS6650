import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SkierServlet", value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        // Check if we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // Validate URL path
        if (isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Invalid URL");
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // Process URL parameters
            res.getWriter().write("GET request received: " + String.join(", ", urlParts));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        // Check if we have a URL
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{\"message\":\"Missing parameters\"}");
            return;
        }

        String[] urlParts = urlPath.split("/");

        // Validate URL path
        if (isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\":\"Invalid URL\"}");
        } else {
            // Read JSON request body
            StringBuilder jsonBuffer = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuffer.append(line);
                }
            }

            String jsonString = jsonBuffer.toString();
            // Process the JSON string (you can add custom processing here)
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"message\":\"POST request processed\", \"data\":" + jsonString + "}");
        }
    }

    private boolean isUrlValid(String[] urlParts) {
        // Validate the URL path according to the API spec
        // Expected format: /<resortID>/seasons/<seasonID>/day/<dayID>/skier/<skierID>
        if (urlParts.length != 8) return true;

        try {
            // Check that specific parts of the URL are valid numbers
            int resortID = Integer.parseInt(urlParts[1]);
            String seasonsKeyword = urlParts[2];
            int seasonID = Integer.parseInt(urlParts[3]);
            String dayKeyword = urlParts[4];
            int dayID = Integer.parseInt(urlParts[5]);
            String skierKeyword = urlParts[6];
            int skierID = Integer.parseInt(urlParts[7]);

            // Ensure proper keywords in the URL
            return !"seasons".equals(seasonsKeyword) || !"day".equals(dayKeyword) || !"skier".equals(skierKeyword) ||
                    resortID <= 0 || seasonID <= 0 || dayID <= 0 || dayID > 366 || skierID <= 0;
        } catch (NumberFormatException e) {
            return true; // Return false if parsing numbers fails
        }
    }
}
