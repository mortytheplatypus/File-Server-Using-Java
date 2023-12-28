import java.io.*;
import java.net.Socket;

public class ClientServerUtil {
    public static final int BUFFER_SIZE = 2048;
    public static final int PORT = 5046;
    private static final String ROOT_DIRECTORY = "F:\\Education\\BUET\\Level3Term2\\CSE322\\Offline 1\\Offline_1 materials\\Offline 1\\root";
    private static final String UPLOAD_DIRECTORY = ROOT_DIRECTORY + "\\upload";
    private static final String LOG_DIRECTORY = "F:\\Education\\BUET\\Level3Term2\\CSE322\\Offline 1\\Offline_1 materials\\Offline 1\\root\\log";

    public void sendFile(Socket socket, File file) {
        int count;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            OutputStream outputStream = socket.getOutputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));

            while ((count=bufferedInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, count);
                outputStream.flush();
            }

            bufferedInputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveFile(Socket socket, String filename) {
        int count;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            InputStream inputStream = socket.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(UPLOAD_DIRECTORY + "\\" + filename);

            while ((count=(inputStream.read(buffer))) > 0) {
                fileOutputStream.write(buffer, 0, count);
            }

            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareLogDirectory() {
        File file = new File(new ClientServerUtil().getLogDirectory());

        if(file.exists()) {
            File[] fileList = file.listFiles();

            for(File f : fileList) {
                f.delete();
            }
        }
    }

    public int getPort() {
        return PORT;
    }

    public String getRootDirectory() {
        return ROOT_DIRECTORY;
    }

    public String getLogDirectory() {
        return LOG_DIRECTORY;
    }

    public boolean checkIfRightFileTypeToUpload(String fileName) {
        String[] allowedExtensions = {"txt", "mkv", "avi", "mp4", "jpg", "jpeg", "png"};
        for (String extension : allowedExtensions) {
            if (fileName.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
