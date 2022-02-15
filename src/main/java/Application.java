import java.io.Console;
import java.util.Scanner;

public class Application {

    static String[] collectCustomerInfo(){
        System.out.println("--- CUSTOMER DATA ---");
        String[] data = new String[3];
        Scanner scan = new Scanner(System.in);
        System.out.print("Name: ");
        data[0] = scan.nextLine();
        System.out.print("Surname: ");
        data[1] = scan.nextLine();
        System.out.print("Email: ");
        data[2] = scan.nextLine();
        return data;
    }

    static void pause(){
        try {
            System.in.read();
        } catch (Exception e) {
            System.exit(1);
        }
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

        int userInput = 1;
        while(userInput != 0){
            pause("Press Enter to continue...");
            System.out.println("""
                            --- MENU ---
                            [1] List all cars
                            [2] Add a customer
                            [3] Register customer
                            [0] Quit"""
                    );
            userInput = scan.nextInt();
            switch (userInput) {
                case 1 -> db.listAllCars();
                case 2 -> {
                    String[] info = collectCustomerInfo();
                    db.addCustomer(info);
                }
            }
        }

    }
}
