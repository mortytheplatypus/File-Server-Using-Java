import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// DO NOT FORGET TO UPDATE ROOT AND UPLOAD DIRECTORIES

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println(ANSI_PURPLE + "Server started.\n" +
                "Listening for connections on port " + PORT + "...\n" + ANSI_RESET);

        new ClientServerUtil().prepareLogDirectory();

        while (true) {
            Socket socket = serverSocket.accept();
            new ServerThread(socket);
        }
    }

    private static final int PORT = new ClientServerUtil().getPort();

    // colors
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_PURPLE = "\u001B[35m";
}
