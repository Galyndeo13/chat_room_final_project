import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 3355;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            // Receive server greeting and input name
            System.out.println(in.readUTF());
            String name = scanner.nextLine();
            out.writeUTF(name);

            // Start thread to read messages from server
            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        String serverMsg = in.readUTF();
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            readThread.start();

            // Main loop to send messages to server
            while (true) {
                String msg = scanner.nextLine();
                out.writeUTF(msg);
                if (msg.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }
}
