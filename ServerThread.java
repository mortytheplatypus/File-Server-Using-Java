import java.io.*;
import java.net.Socket;
import java.util.Date;

public class ServerThread implements Runnable {
    private final Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private StringBuilder stringBuilder;
    private FileWriter fileWriter;
    private static int logRequestNo = 0;

    public ServerThread(Socket socket) throws IOException {
        this.socket = socket;
        this.configureStreams();
        new Thread(this).start();
    }

    @Override
    public void run() {
        String httpRequest, httpResponse = "";

        // receive request from client
        try {
            httpRequest = bufferedReader.readLine();
        } catch (IOException e) {
            httpRequest = null;
            e.printStackTrace();
        }

        if (httpRequest == null) {
            this.closeConnection();
        } else if (httpRequest.startsWith("GET")) {
            System.out.println("HTTP request from client: " + ANSI_CYAN + httpRequest + ANSI_RESET);

            this.writeToLog(httpResponse);

            // extract path
            String[] partitions = httpRequest.split("\\s+");
            String[] uri = partitions[1].split("/");
            StringBuilder path = new StringBuilder();

            for (int i=1; i<uri.length; i++) {
                path.append(uri[i]).append("\\");
            }

            // set file
            File file;
            if (path.toString().equals("")) {
                file = new File(ROOT_DIRECTORY);
            } else {
                file = new File(ROOT_DIRECTORY + "\\" + path);
            }

            if (!file.exists()) {
                stringBuilder.append(HTML_NOT_FOUND);

                // send response to client
                httpResponse = "HTTP/1.1 404 NOT FOUND\r\n" +
                        "Server: Java HTTP Server: 1.0\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + stringBuilder.toString().length() +
                        "\r\n";
                System.out.println("HTTP response from server: " + ANSI_RED + "HTTP/1.1 404 NOT FOUND" + ANSI_RESET);

                this.writeToLog(httpResponse);

                printWriter.write(httpResponse);
                printWriter.write("\r\n");
                printWriter.write(stringBuilder.toString());
                printWriter.flush();
            } else {
                if (file.isDirectory()) {
                    File[] fileList = file.listFiles();

                    stringBuilder.append(HTML_START);
                    assert fileList != null;
                    for (File f : fileList) {
                        if (f.isDirectory()) {
                            stringBuilder.append(this.getHTMLBody(f, path.toString(), true));
                        } else if (f.isFile()) {
                            stringBuilder.append(this.getHTMLBody(f, path.toString(), false));
                        }
                    }
                    stringBuilder.append(HTML_END);

                    // send response to client
                    httpResponse = "HTTP/1.1 200 OK\r\n" +
                            "Server: Java HTTP Server: 1.0\r\n" +
                            "Date: " + new Date() + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: " + stringBuilder.toString().length() +
                            "\r\n";
                    System.out.println("HTTP response from server: " + ANSI_GREEN + "HTTP/1.1 200 OK" + ANSI_RESET);

                    this.writeToLog(httpResponse);

                    printWriter.write(httpResponse);
                    printWriter.write("\r\n");
                    printWriter.write(stringBuilder.toString());
                    printWriter.flush();

                } else if (file.isFile()) {
                    String fileName = file.getName();
                    if (!checkIfTextOrImage(fileName)) {
                        // force download
                        httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Server: Java HTTP Server: 1.0\r\n" +
                                "Date: " + new Date() + "\r\n" +
                                "Content-Type: application/force-download\r\n" +
                                "Content-Length: " + file.length() + "\r\n";
                    } else if (getExtension(fileName).equalsIgnoreCase("txt")) {
                        // show preview
                        httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Server: Java HTTP Server: 1.0\r\n" +
                                "Date: " + new Date() + "\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Disposition: inline\r\n" +
                                "Content-Length: " + file.length() + "\r\n";
                    } else {
                        httpResponse = "HTTP/1.1 200 OK\r\n" +
                                "Server: Java HTTP Server: 1.0\r\n" +
                                "Date: " + new Date() + "\r\n" +
                                "Content-Type: image/" + getExtension(fileName) + "\r\n" +
                                "Content-Disposition: inline\r\n" +
                                "Content-Length: " + file.length() + "\r\n";
                    }
                    System.out.println("HTTP response from server: " + ANSI_GREEN + "HTTP/1.1 200 OK" + ANSI_RESET);

                    this.writeToLog(httpResponse);

                    printWriter.write(httpResponse);
                    printWriter.write("\r\n");
                    printWriter.flush();

                    new ClientServerUtil().sendFile(this.socket, file); // send the file in chunks using socket
                }
            }
        } else if (httpRequest.startsWith("UPLOAD")) {
            System.out.println("HTTP request from client: " + ANSI_CYAN + httpRequest + ANSI_RESET);

            // check filename validity
            try {
                String isValid = bufferedReader.readLine();

                if (isValid.startsWith("NOT_FOUND")) {
                    System.out.println("HTTP response from server: " + ANSI_RED + httpRequest.substring(7) + " >> FILE NOT FOUND" + ANSI_RESET);
                    this.writeToLog("HTTP response from server: " + httpRequest.substring(7) + " >> FILE NOT FOUND");
                    this.closeConnection();
                } else if (isValid.startsWith("INVALID_TYPE")) {
                    System.out.println("HTTP response from server: " + ANSI_RED + httpRequest.substring(7) + " >> FILE-TYPE NOT ALLOWED" + ANSI_RESET);
                    this.writeToLog("HTTP response from server: " + httpRequest.substring(7) + " >> FILE NOT FOUND");
                    this.closeConnection();
                } else {
                    String fileName = httpRequest.substring(7);
                    if (new ClientServerUtil().checkIfRightFileTypeToUpload(fileName)) {
                        // receive file from client
                        new ClientServerUtil().receiveFile(socket, fileName);
                        System.out.println("HTTP response from server: " + ANSI_BLUE + httpRequest.substring(7) + " >> FILE UPLOADED SUCCESSFULLY" + ANSI_RESET);
                        this.writeToLog("HTTP response from server: " + httpRequest.substring(7) + " >> FILE UPLOADED SUCCESSFULLY");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.closeConnection();
    }

    private void configureStreams() throws IOException {
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.printWriter = new PrintWriter(this.socket.getOutputStream());
        this.stringBuilder = new StringBuilder();
        this.fileWriter = new FileWriter(new ClientServerUtil().getLogDirectory() + "\\http_log_" + logRequestNo++ + ".log");
    }

    private void closeConnection() {
        try {
            bufferedReader.close();
            printWriter.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToLog(String httpResponse) {
        try {
            fileWriter.write("HTTP response from server: " + httpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getHTMLBody(File file, String path, boolean isDirectory) {
        if (isDirectory) {
            return "<font size=\"8\"><b><i><a href=\"http://localhost:" + PORT + "/" + path.replace("\\", "/") + file.getName() + "\"> " + file.getName() + "</i></b></font><br>\n";
        }

        return "<font size=\"8\"><a target=\"_blank\" href=\"http://localhost:" + PORT + "/" + path.replace("\\", "/") + file.getName() + "\"> " + file.getName() + "</font><br>\n";
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf(".");

        return fileName.substring(index+1);
    }

    private boolean checkIfTextOrImage(String fileName) {
        if (getExtension(fileName).equalsIgnoreCase("txt")) {
            return true;
        } else if (getExtension(fileName).equalsIgnoreCase("jpeg")) {
            return true;
        } else if (getExtension(fileName).equalsIgnoreCase("jpg")) {
            return true;
        } else return getExtension(fileName).equalsIgnoreCase("png");
    }

    private static final String HTML_START = "<!DOCTYPE html>\n\n" +
            "<html lang=\"en\">\n" +
            "  <head>\n" +
            "      <meta charset=\"UTF-8\">\n" +
            "  </head>\n\n<body>\n";
    private static final String HTML_END = "\n  </body>\n</html>";
    private static final String HTML_NOT_FOUND = HTML_START + "<center><h1>404: PAGE NOT FOUND</h1></center>" + HTML_END;

    private static final int PORT = new ClientServerUtil().getPort();
    private static final String ROOT_DIRECTORY = new ClientServerUtil().getRootDirectory();

    // colors
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_GREEN = "\u001B[32m";
    static final String ANSI_BLUE = "\u001B[34m";
    static final String ANSI_CYAN = "\u001B[36m";
}
