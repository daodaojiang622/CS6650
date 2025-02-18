import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/dbtest")
public class DBTestServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBCPDataSource.getDataSource().getConnection()) {
            if (conn != null) {
                out.println("✅ SUCCESS: Tomcat connected to MySQL!");
            } else {
                out.println("❌ ERROR: Connection is null.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("❌ ERROR: Unable to connect to MySQL: " + e.getMessage());
        }
    }
}
