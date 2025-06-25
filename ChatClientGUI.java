import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton createRoomButton, joinRoomButton, leaveRoomButton, kickButton, closeRoomButton, refreshRoomsButton;
    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private String clientName;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI().start());
    }

    public void start() {
        try {
            socket = new Socket("localhost", 3355);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            clientName = JOptionPane.showInputDialog("Enter your name:");
            out.writeUTF(clientName);

            createGUI();

            // Listen for server messages
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        chatArea.append(msg + "\n");
                    }
                } catch (IOException e) {
                    chatArea.append("Disconnected from server.\n");
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection error: " + e.getMessage());
        }
    }

    private void createGUI() {
        frame = new JFrame("Chat Client - " + clientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        frame.add(chatScroll, BorderLayout.CENTER);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Room list and control buttons
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setPreferredSize(new Dimension(200, 0));

        createRoomButton = new JButton("Create Room");
        joinRoomButton = new JButton("Join Room");
        leaveRoomButton = new JButton("Leave Room");
        kickButton = new JButton("Kick User");
        closeRoomButton = new JButton("Close Room");
        refreshRoomsButton = new JButton("Refresh Rooms");

        createRoomButton.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog("Enter new room name:");
            if (roomName != null && !roomName.isBlank()) sendCommand("/create " + roomName);
        });
        joinRoomButton.addActionListener(e -> {
            String selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null) sendCommand("/join " + selectedRoom);
        });
        leaveRoomButton.addActionListener(e -> sendCommand("/leave"));
        kickButton.addActionListener(e -> {
            String user = JOptionPane.showInputDialog("Enter username to kick:");
            if (user != null && !user.isBlank()) sendCommand("/kick " + user);
        });
        closeRoomButton.addActionListener(e -> sendCommand("/close"));
        refreshRoomsButton.addActionListener(e -> {
            sendCommand("/rooms");
            // Note: room list update requires parsing output, kept simple for now
        });

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.add(new JLabel("Rooms:"));
        sidePanel.add(roomScroll);
        sidePanel.add(createRoomButton);
        sidePanel.add(joinRoomButton);
        sidePanel.add(leaveRoomButton);
        sidePanel.add(kickButton);
        sidePanel.add(closeRoomButton);
        sidePanel.add(refreshRoomsButton);

        frame.add(sidePanel, BorderLayout.EAST);
        frame.setVisible(true);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            sendCommand(msg);
            inputField.setText("");
        }
    }

    private void sendCommand(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            chatArea.append("Failed to send message.\n");
        }
    }
}