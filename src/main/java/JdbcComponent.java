import java.security.InvalidParameterException;
import java.sql.*;
import java.util.HashMap;

public class JdbcComponent {
    private String uri, user, password;
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
    public StringBuilder generatePassword(){
        StringBuilder password = new StringBuilder();
        int min = 48, max = 125; // ASCII
        for(int i = 0; i < 10; i++){
            password.append((char) Math.round(((Math.random() * (max - min)) + min)));
        }
        return password;
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
    public void addCustomer(HashMap<String, String> customerData) {
        String name = customerData.get("name");
        String surname = customerData.get("surname");
        String email = customerData.get("email");
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT email FROM customer WHERE email=?");
            statement.setString(1, email);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                throw new InvalidParameterException(String.format("ERROR: Customer with email <%s> already exists!", email));
            } else {
                statement = connection.prepareStatement("INSERT INTO customer(fName, surname, email) VALUES (?,?,?)");
                System.out.println("INFO: Setting parameters...");
                statement.setString(1, name);
                statement.setString(2, surname);
                statement.setString(3, email);
                System.out.println("INFO: Parameters set. Processing query...");
                statement.executeUpdate();
                System.out.printf("INFO: Customer %s %s <%s> has been added to the database!", name, surname, email);
            }
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Unable to add a customer!");
        }
    }
    public void registerCustomer(HashMap<String, String> customerData){
        String email = customerData.get("email");
        String login = customerData.get("login");
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {
            PreparedStatement statement = connection.prepareStatement("SELECT email FROM customer WHERE email=?");
            statement.setString(1, email);
            ResultSet result = statement.executeQuery();
            if(!result.next()) {
                throw new InvalidParameterException(String.format("ERROR: Customer with email address <%s> does not exist!", email));
            } else {
                System.out.printf("INFO: Customer with email <%s> found. Checking username availability...\n", email);
                statement = connection.prepareStatement("SELECT login FROM customer WHERE login=?");
                statement.setString(1, login);
                result = statement.executeQuery();
                if(result.next()){
                    throw new InvalidParameterException(String.format("ERROR: Username '%s' is already taken!", login));
                } else {
                    System.out.printf("INFO: Username '%s' is available. Registering...\n", login);
                    statement = connection.prepareStatement(
                            "UPDATE customer SET login=?, passwd=? WHERE email=?");
                    statement.setString(1, login);
                    String genPass = generatePassword().toString();
                    statement.setString(2, genPass);
                    statement.setString(3, email);
                    //System.out.printf("USERNAME: %s\nPASSWORD: %s\nEMAIL: %s\n", login, genPass, email);
                    statement.executeUpdate();
                    System.out.printf("INFO: Username '%s' registered under <%s> - password generated: %s\n", login, email, genPass);
                }
            }
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Unable to register customer!");
        }
    }
}
