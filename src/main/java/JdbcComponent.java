import javax.annotation.Nullable;
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
    public void printCars(boolean detailed, @Nullable String city, int price_cat, boolean availableOnly) {
        try (Connection connection = DriverManager.getConnection(uri, user, password)) {

            String sql = "SELECT * FROM car, price_cat, branch WHERE car.price_cat=price_cat.id AND car.branch=branch.id ";
            sql += (city == null) ? "AND city=city " : String.format("AND city='%s' ", city) ;
            sql += (price_cat == 0) ? "AND price_cat.id=price_cat.id " : String.format("AND price_cat.id=%d ", price_cat);
            if(availableOnly) sql += "AND returned=true";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            String header = "";

            if(!resultSet.next())
                System.out.println("INFO: No cars available in this location.");
            else {
                // Create template for displaying each row
                String template = (detailed) ?
                        "| %-10s | %-20s | %-20s | %-20s | %-15s | %8s | %8s | %8s |\n" :
                        "| %-20s | %-20s | %-20s | %-15s |\n" ;
                // Modify table header
                if(detailed) {
                    header = String.format(template, "Plates", "Make", "Model", "Branch", "Category", "24 hrs", "7 days", "1 month");
                }
                else {
                    header = String.format(template, "Make", "Model", "Branch", "Category");
                }
                System.out.printf("%s\n%s%s\n", "-".repeat(header.length()), header, "-".repeat(header.length()));
                do {
                    String regPlate = resultSet.getString("reg_plate");
                    String make = resultSet.getString("make");
                    String model = resultSet.getString("model");
                    String branch = resultSet.getString("branch_name");
                    String category = resultSet.getString("label");
                    float[] price = {
                            resultSet.getFloat("rent_24h"),
                            resultSet.getFloat("rent_7d"),
                            resultSet.getFloat("rent_1m")};
                    if (detailed) {
                        System.out.printf(template, regPlate, make, model, branch, category,
                                String.format("%.2f",price[0]),
                                String.format("%.2f",price[1]),
                                String.format("%.2f",price[2]));
                    } else {
                        System.out.printf(template, make, model, branch, category);
                    }
                } while (resultSet.next());
            }
            System.out.println("-".repeat(header.length()));
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
            PreparedStatement statement = connection.prepareStatement("SELECT email,login FROM customer WHERE email=?");
            statement.setString(1, email);
            ResultSet result = statement.executeQuery();
            if(!result.next()) {
                throw new InvalidParameterException(String.format("ERROR: Customer with email address <%s> does not exist!", email));
            } else {
                if(!(result.getString("login") == null)){
                    throw new InvalidParameterException("ERROR: This user is registered!");
                }
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
            System.out.println("ERROR: Unable to register customer!\n");
            e.printStackTrace();
        }
    }
    public boolean authenticateUser(HashMap<String, String> info){
        try (Connection connection = DriverManager.getConnection(uri, user, password)){
            PreparedStatement statement = connection.prepareStatement("SELECT login FROM customer WHERE login=? AND passwd=?");
            statement.setString(1, info.get("login"));
            statement.setString(2, info.get("password"));
            ResultSet result = statement.executeQuery();
            if(result.next()) {
                System.out.println("INFO: Successfully logged in!");
                return true;
            }
        } catch (Exception e){
            System.out.println("ERROR: Unable to authenticate user!");
        }
        System.out.println("INFO: Username or password are incorrect.");
        return false;
    }
    public boolean isRentalPossible(String login){
        try(Connection connection = DriverManager.getConnection(uri,user,password)){
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT completed FROM customer, booking 
                    WHERE customer.id=booking.customer_id 
                    AND customer.login=? AND completed=0 """);
            statement.setString(1, login);
            if (statement.executeQuery().next()) {
                System.out.println("INFO: You cannot rent a car, because you have at least one car not returned!");
                return false;
            } else {
                System.out.println("INFO: All cars returned, you can rent a new car!");
                return true;
            }
        } catch (Exception e){
            System.out.printf("ERROR: Unable to get rental information for user '%s'!", login);
        }
        return false;
    }
    public void printCities(){
        try (Connection connection = DriverManager.getConnection(uri, user, password)){
            PreparedStatement statement = connection.prepareStatement("SELECT city FROM branch");
            ResultSet result = statement.executeQuery();
            while(result.next())
                System.out.println(result.getString("city"));
        } catch (Exception e) {
            System.out.println("ERROR: Unable to list the cities!");
        }
    }
    public void rentCar(String login, String plateNo){
        try(Connection connection = DriverManager.getConnection(uri,user,password)){
            System.out.println("INFO: Retrieving necessary information...");
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT (SELECT id FROM customer WHERE login=?) AS customerID, 
                    (SELECT agency FROM branch,car WHERE car.branch=branch.id AND reg_plate=?) AS agencyID, 
                    (SELECT id FROM car WHERE reg_plate=?) AS carID """);
            statement.setString(1, login);
            statement.setString(2, plateNo);
            statement.setString(3, plateNo);
            ResultSet result = statement.executeQuery();
            if(result.next()){
                int customerId = result.getInt("customerID");
                int agencyId = result.getInt("agencyID");
                int carId = result.getInt("carID");
                if(carId <= 0) throw new InvalidParameterException("ERROR: Registration plates are invalid!");
                System.out.println("INFO: Booking new rental...");
                statement = connection.prepareStatement("""
                        INSERT INTO booking(customer_id,agency_id,car_id,start_date,completed) VALUES
                        (?, ?, ?, CURRENT_TIMESTAMP(), 0) """);
                statement.setInt(1, customerId);
                statement.setInt(2, agencyId);
                statement.setInt(3, carId);
                statement.executeUpdate();
                statement = connection.prepareStatement("UPDATE car SET returned=false WHERE id=?");
                statement.setInt(1, carId);
                statement.executeUpdate();
                System.out.printf("INFO: Rental started! Registration plates: %s (for user: %s)\n", plateNo, login);
            } else {
                System.out.println("ERROR: Unable to collect data for rental process!");
            }
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Unable to finish the rental process!");
        }
    }
    public void printRentedFor(String login){
        try(Connection connection = DriverManager.getConnection(uri,user,password)){
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT reg_plate,make,model FROM car,booking,customer 
                    WHERE car.id=booking.car_id 
                    AND booking.customer_id=customer.id 
                    AND customer.login=? AND returned=false AND completed=0 """);
            statement.setString(1, login);
            ResultSet result = statement.executeQuery();
            while(result.next()){
                System.out.printf("\t> %s %s (reg. plates: %s)\n",
                        result.getString("make"),
                        result.getString("model"),
                        result.getString("reg_plate"));
            }
        } catch (Exception e){
            System.out.println("ERROR: Unable to list rented cars!");
        }
    }
    public void returnCar(String login){
        try(Connection connection = DriverManager.getConnection(uri,user,password)){
            System.out.println("INFO: Getting customer ID...");
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM customer WHERE login=?");
            statement.setString(1, login);
            ResultSet result = statement.executeQuery(); result.next();
            int customerId = result.getInt("id");
            System.out.println("INFO: Getting car ID...");
            statement = connection.prepareStatement("""
                    SELECT car_id FROM booking WHERE customer_id=? 
                    AND completed=0 """);
            statement.setInt(1, customerId);
            result = statement.executeQuery(); result.next();
            int carId = result.getInt("car_id");
            System.out.println("INFO: Updating booking record...");
            statement = connection.prepareStatement("""
                    UPDATE booking SET return_date=CURRENT_TIMESTAMP(), completed=1 
                    WHERE customer_id=? AND car_id=? AND completed=0 """);
            statement.setInt(1, customerId);
            statement.setInt(2, carId);
            statement.executeUpdate();
            System.out.println("INFO: Updating car status....");
            statement = connection.prepareStatement("UPDATE car SET returned=1 WHERE id=? ");
            statement.setInt(1, carId);
            statement.executeUpdate();
            System.out.println("INFO: Car return successfully completed!");
        } catch (Exception e){
            System.out.println("ERROR: Unable to return car!");
        }
    }
}
