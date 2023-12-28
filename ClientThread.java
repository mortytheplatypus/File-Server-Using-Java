import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientThread implements Runnable {
    private final Socket socket;
    private final File file;
    private final PrintWriter printWriter;

    public ClientThread(String filename) throws IOException {
        file = new File(filename);
        socket = new Socket("localhost", new ClientServerUtil().getPort());

        printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.flush();
        new Thread(this).start();
    }

    @Override
    public void run() {
        printWriter.write("UPLOAD " + file.getName() + "\r\n");

        if (!file.exists()) {
            printWriter.write("NOT_FOUND\r\n");
            printWriter.flush();

        } else {
            if (new ClientServerUtil().checkIfRightFileTypeToUpload(file.getName())) {
                printWriter.write("VALID\r\n");
                printWriter.flush();

                new ClientServerUtil().sendFile(socket, file);
            } else {
                printWriter.write("INVALID_TYPE\r\n");
                printWriter.flush();
            }

        }

        this.closeConnection();
    }

    private void closeConnection() {
        try {
            printWriter.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int PORT = new ClientServerUtil().getPort();
}
