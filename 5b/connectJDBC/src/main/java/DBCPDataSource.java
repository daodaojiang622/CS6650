import org.apache.commons.dbcp2.*;

public class DBCPDataSource {
    private static BasicDataSource dataSource;

    // NEVER store sensitive information below in plain text!
    private static final String HOST_NAME = "100.20.231.186/connectJDBC-1.0-SNAPSHOT/skiers/";
    private static final String PORT = "3306";
    private static final String DATABASE = "LiftRides";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "MySecure@2025";


    static {
        System.out.println("DEBUG: MySQL_IP_ADDRESS = " + HOST_NAME);

        // https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
        dataSource = new BasicDataSource();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);

        // Debugging: Print DB connection details (EXCEPT PASSWORD!)
        System.out.println("Connecting to MySQL at: " + url);
        System.out.println("Using username: " + USERNAME);

        dataSource.setUrl(url);
        System.out.println("DEBUG: URL = " + url);
        dataSource.setUsername(USERNAME);
        System.out.println("DEBUG: USERNAME = " + USERNAME);
        dataSource.setPassword(PASSWORD);
        System.out.println("DEBUG: PASSWORD = " + PASSWORD);
        dataSource.setInitialSize(10);
        dataSource.setMaxTotal(60);
        System.out.println("DEBUG: DBCPDataSource initialized.");
    }

    public static BasicDataSource getDataSource() {
        return dataSource;
    }
}