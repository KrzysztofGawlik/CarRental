import java.io.Console;
import java.util.Scanner;
import java.util.HashMap;

public class Application {

    enum customerOperation {
        ADD, REGISTER, LOGIN
    }

    static HashMap<String, String> collectCustomerInfo(customerOperation co){
        System.out.println("--- CUSTOMER DATA ---");
        HashMap<String, String> data = new HashMap<>();
        Scanner scan = new Scanner(System.in);
        if(co == customerOperation.ADD){
            System.out.print("Name: ");
            data.put("name", scan.nextLine());
            System.out.print("Surname: ");
            data.put("surname", scan.nextLine());
            System.out.print("Email: ");
            data.put("email", scan.nextLine());
        } else if (co == customerOperation.REGISTER) {
            System.out.print("Customer email: ");
            data.put("email", scan.nextLine());
            System.out.print("Login: ");
            data.put("login", scan.nextLine());
        } else if (co == customerOperation.LOGIN) {
            System.out.print("Login: ");
            data.put("login", scan.nextLine());
            Console console = System.console();
            if (console != null) {
                data.put("password", new String(console.readPassword("Password: ")));
            } else {
                System.out.print("No console available - password will be VISIBlE on the screen!\nPassword: ");
                data.put("password", scan.nextLine());
            }
        }
        return data;
    }

    static void pause(String alt){
        try {
            System.out.print("\n" + alt);
            System.in.read();
        } catch (Exception e) {
            System.exit(1);
        }
    }

    public static void main(String[] args){
        boolean noLogging = true;
        String username, passwd;
        final String uri = "jdbc:mysql://localhost:3306/car-rental-db";
        Scanner scan = new Scanner(System.in);
        Console console = System.console();

        System.out.println("*** Chris's Car Rental System ***");
        if(noLogging){
            username = "root";
            passwd = "root";
        } else {
            System.out.print("username: ");
            username = scan.nextLine();
            if (console != null) {
                passwd = new String(console.readPassword("password: "));
            } else {
                System.out.print("No console available - password will be VISIBlE on the screen!\npassword: ");
                passwd = scan.nextLine();
            }
        }

        JdbcComponent db = new JdbcComponent(uri, username, passwd);

        int userInput = -1;
        HashMap<String, String> info = new HashMap<>();
        while(userInput != 0){
            pause("Press Enter to continue...");
            System.out.println("""
                            --- MENU ---
                            [1] List all cars
                            [2] Add a customer
                            [3] Register customer
                            [4] Rent a car
                            [0] Quit"""
                    );
            try{
                userInput = Integer.parseInt(scan.nextLine());
            } catch (Exception e) {
                System.out.println("ERROR: Unrecognizable input, please provide a valid number...");
            }
            switch (userInput) {
                case 0 -> {}
                case 1 -> db.printCars(false, null, 0, false);
                case 2 -> {
                    info = collectCustomerInfo(customerOperation.ADD);
                    db.addCustomer(info);
                }
                case 3 -> {
                    info = collectCustomerInfo(customerOperation.REGISTER);
                    db.registerCustomer(info);
                }
                case 4 -> {
                    info = collectCustomerInfo(customerOperation.LOGIN);
                    if(db.authenticateUser(info) && db.isRentalPossible(info.get("login"))){
                        boolean processing = true;
                        String city, plateNo;
                        while(processing){
                            System.out.println("--- CITIES ---");
                            db.printCities();
                            System.out.print("Choose the city: ");
                            city = scan.nextLine();
                            db.printCars(true, city, 0, true);
                            System.out.print("Input plate number of the car you want to rent (no input - abort): ");
                            plateNo = scan.nextLine();
                            if(plateNo.isEmpty()){
                                processing = false;
                            } else {
                                db.rentCar(info.get("login"), plateNo);
                            }
                        }
                    }
                }
                default -> System.out.println("INFO: No such option, please check the menu options.");
            }
            info.clear();
        }

    }
}
