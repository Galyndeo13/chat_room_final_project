import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 3355;
    // Map of room name to Room object
    private static Map<String, ChatRoom> rooms = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket).start();
        }
    }

    // Room class to manage per-room data
    static class ChatRoom {
        String name;
        String owner;
        Set<ClientHandler> members = Collections.synchronizedSet(new HashSet<>());

        ChatRoom(String name, String owner) {
            this.name = name;
            this.owner = owner;
        }

        void broadcast(String message, ClientHandler sender) {
            synchronized (members) {
                for (ClientHandler client : members) {
                    if (client != sender) {
                        client.sendMessage("[" + name + "] " + sender.clientName + ": " + message);
                    }
                }
            }
        }

        void notifyAll(String message) {
            synchronized (members) {
                for (ClientHandler client : members) {
                    client.sendMessage("[" + name + "] " + message);
                }
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String clientName;
        private ChatRoom currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            try {
                out.writeUTF(message);
            } catch (IOException e) {
                System.out.println("Error sending message to " + clientName);
            }
        }

        public void run() {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                out.writeUTF("Enter your name:");
                clientName = in.readUTF();
                out.writeUTF("Welcome, " + clientName + ". Type /help for options.");

                while (true) {
                    String msg = in.readUTF();

                    if (msg.startsWith("/create ")) {
                        String roomName = msg.substring(8).trim();
                        if (!rooms.containsKey(roomName)) {
                            ChatRoom room = new ChatRoom(roomName, clientName);
                            room.members.add(this);
                            rooms.put(roomName, room);
                            currentRoom = room;
                            sendMessage("Room '" + roomName + "' created and joined.");
                        } else {
                            sendMessage("Room already exists.");
                        }
                    } else if (msg.startsWith("/join ")) {
                        String roomName = msg.substring(6).trim();
                        if (rooms.containsKey(roomName)) {
                            ChatRoom room = rooms.get(roomName);
                            room.members.add(this);
                            currentRoom = room;
                            room.notifyAll(clientName + " joined the room.");
                        } else {
                            sendMessage("Room not found.");
                        }
                    } else if (msg.equals("/rooms")) {
                        StringBuilder sb = new StringBuilder("Available rooms:\n");
                        for (String room : rooms.keySet()) {
                            sb.append("- ").append(room).append(" (Owner: ").append(rooms.get(room).owner).append(")\n");
                        }
                        sendMessage(sb.toString());
                    } else if (msg.equals("/leave")) {
                        if (currentRoom != null) {
                            currentRoom.members.remove(this);
                            currentRoom.notifyAll(clientName + " left the room.");
                            sendMessage("You left the room.");
                            if (currentRoom.members.isEmpty()) {
                                rooms.remove(currentRoom.name);
                            }
                            currentRoom = null;
                        } else {
                            sendMessage("You are not in any room.");
                        }
                    } else if (msg.startsWith("/kick ")) {
                        if (currentRoom != null && clientName.equals(currentRoom.owner)) {
                            String userToKick = msg.substring(6).trim();
                            Optional<ClientHandler> target = currentRoom.members.stream().filter(c -> c.clientName.equals(userToKick)).findFirst();
                            if (target.isPresent()) {
                                ClientHandler kicked = target.get();
                                currentRoom.members.remove(kicked);
                                kicked.currentRoom = null;
                                kicked.sendMessage("You have been kicked from room '" + currentRoom.name + "'.");
                                currentRoom.notifyAll(userToKick + " was kicked by owner.");
                            } else {
                                sendMessage("User not found in room.");
                            }
                        } else {
                            sendMessage("You are not the owner or not in a room.");
                        }
                    } else if (msg.equals("/close")) {
                        if (currentRoom != null && clientName.equals(currentRoom.owner)) {
                            currentRoom.notifyAll("Room is closed by owner.");
                            for (ClientHandler ch : currentRoom.members) {
                                if (ch != this) {
                                    ch.currentRoom = null;
                                    ch.sendMessage("Disconnected. Room closed.");
                                }
                            }
                            rooms.remove(currentRoom.name);
                            currentRoom = null;
                        } else {
                            sendMessage("You are not the owner or not in a room.");
                        }
                    } else if (msg.equalsIgnoreCase("exit")) {
                        break;
                    } else {
                        if (currentRoom != null) {
                            currentRoom.broadcast(msg, this);
                        } else {
                            sendMessage("Join a room to chat. Use /rooms or /join <name>.");
                        }
                    }
                }

                if (currentRoom != null) {
                    currentRoom.members.remove(this);
                    currentRoom.notifyAll(clientName + " left the room.");
                    if (currentRoom.members.isEmpty()) {
                        rooms.remove(currentRoom.name);
                    }
                }
                socket.close();
            } catch (IOException e) {
                System.out.println(clientName + " disconnected unexpectedly.");
                if (currentRoom != null) {
                    currentRoom.members.remove(this);
                    currentRoom.notifyAll(clientName + " disconnected.");
                    if (currentRoom.members.isEmpty()) {
                        rooms.remove(currentRoom.name);
                    }
                }
            }
        }
    }
}
