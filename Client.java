import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String filename;

        System.out.println(ANSI_PURPLE + "Client started.\n" + ANSI_RESET);

        while (true) {
            System.out.print("Enter the absolute path of the file to be uploaded.\n" +
                    "\t>> ");
            filename = scanner.nextLine();

            if (filename.equals("")) {
                continue;
            }

            new ClientThread(filename);
        }
    }

    // colors
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_PURPLE = "\u001B[35m";
}
