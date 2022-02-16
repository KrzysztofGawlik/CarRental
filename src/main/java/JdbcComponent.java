import java.sql.*;
import java.util.HashMap;

public class JdbcComponent {
    String uri, user, password;
    JdbcComponent(String uri, String user, String password){
        try {
            this.uri = uri;
            this.user = user;
            this.password = password;
            Connection connection = DriverManager.getConnection(uri, user, password);
            System.out.println("Access to the database granted!");
            connection.close();
        } catch (SQLException sqlE) {
            System.out.println("ERROR: Cannot establish a connection with database!");
            System.exit(1);
        }
    }
    public void listAllCars() {
        try (
            Connection connection = DriverManager.getConnection(uri, user, password);
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM car");
            ResultSet resultSet = preparedStatement.executeQuery())
            {
                System.out.println("-".repeat(54));
                System.out.format("%-4s | %-10s | %-15s | %-15s|\n%s\n", "ID", "PLATES", "MAKE", "MODEL", "-".repeat(54));
                while(resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String regPlate = resultSet.getString("reg_plate");
                    String make = resultSet.getString("make");
                    String model = resultSet.getString("model");
                    System.out.format("%-4d | %-10s | %-15s | %-15s|\n", id, regPlate, make, model);
                }
                System.out.println("-".repeat(54));
            } catch (Exception e) {
            System.out.println("ERROR: Unable to list the cars!");
        }
    }
    public void addCustomer(HashMap<String, String> customerData){
        String name = customerData.get("name");
        String surname = customerData.get("surname");
        String email = customerData.get("email");
        try (
                Connection connection = DriverManager.getConnection(uri, user, password);
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO customer(fName, surname, email) VALUES (?,?,?)")
                ) {
            System.out.println("INFO: Setting parameters...");
            insertStatement.setString(1, name);
            insertStatement.setString(2, surname);
            insertStatement.setString(3, email);
            System.out.println("INFO: Parameters set. Processing query...");
            insertStatement.executeUpdate();
            System.out.printf("INFO: Customer %s %s <%s> has been added to the database!", name, surname, email);
        } catch (Exception e) {
            System.out.println("ERROR: Unable to add a customer!");
        }
    }
}
