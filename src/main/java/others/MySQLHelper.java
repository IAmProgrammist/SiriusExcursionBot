package others;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySQLHelper {

    private static String hostName;
    private static String dbName;
    private static String userName;
    private static String password;

    public static void init(String hostName, String dbName, String userName,
                            String password) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        MySQLHelper.hostName = hostName;
        MySQLHelper.dbName = dbName;
        MySQLHelper.userName = userName;
        MySQLHelper.password = password;
        Connection conn = getMySQLConnection();
        Places[] places = Places.values();
        List<String> sumQuery = new ArrayList<>();
        for(int i = 0; i < places.length; i++){
            String query = "SELECT * from states where id='" + i + "' AND codename='" + places[i].toString() + "';";
            ResultSet set = conn.createStatement().executeQuery(query);
            boolean foundFirst = set.next();
            query = "SELECT * from states where id = '" + i +"';";
            ResultSet setc = conn.createStatement().executeQuery(query);
            boolean foundSecond = setc.next();
            if(!foundFirst && !foundSecond){
                sumQuery.add("INSERT INTO states (id, codename) VALUES ('" + i +"', '"+places[i].toString()+"');");
            }else if(!foundFirst && foundSecond){
                sumQuery.add("UPDATE states SET codename='"+places[i].toString()+"' WHERE id=" + i + ";");
            }
        }
        for(String a : sumQuery){
            conn.createStatement().execute(a);
        }
        conn.close();
    }

    private static Connection getMySQLConnection() throws SQLException, ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String connectionURL = "jdbc:mysql://" + hostName + ":3306/" + dbName;
        Connection conn = DriverManager.getConnection(connectionURL, userName, password);
        return conn;
    }

    public static Map<Integer, ArrayList<Places>> loadVisits() throws SQLException, ClassNotFoundException {
        Connection connection = getMySQLConnection();
        Statement statement = connection.createStatement();
        Map<Integer, ArrayList<Places>> ans = new HashMap<>();
        String query = "Select * from dependencies";
        ResultSet result = statement.executeQuery(query);
        while (result.next()){
            int userid = result.getInt("userid");
            int stateid = result.getInt("state");
            query = "Select * from states where id = " + stateid;
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();
            Places place = Places.valueOf(resultSet.getString("codename"));
            ArrayList<Places> tmp = ans.getOrDefault(userid, new ArrayList<>());
            tmp.add(place);
            ans.put(userid, tmp);
        }
        connection.close();
        return ans;
    }

    public static void writeVisit(Integer userId, Places place) throws SQLException, ClassNotFoundException {
        Connection connection = getMySQLConnection();
        String query = "INSERT IGNORE INTO userslist (id) VALUES (" + userId + ");";
        connection.createStatement().execute(query);
        ResultSet tmp = connection.createStatement().executeQuery("Select * from states where codename='" + place.toString() + "'");
        tmp.next();
        int placeId = tmp.getInt("id");
        query = "INSERT INTO dependencies (userid, state) VALUE (" + userId + ", '" + placeId + "');";
        connection.createStatement().execute(query);
        connection.close();
    }
}
