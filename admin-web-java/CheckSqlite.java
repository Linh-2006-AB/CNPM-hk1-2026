import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckSqlite {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        String url = "jdbc:sqlite:./data/he_thong_thuyet_minh.db";

        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("PRAGMA table_info(media_tasks)")) {

            while (r.next()) {
                System.out.println(r.getString("name"));
            }
        }
    }
}