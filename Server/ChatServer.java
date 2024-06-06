import java.io.*;
import java.net.*;
import java.util.*;

// Sunucu bilgileri
public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, String> userCredentials = new HashMap<>();
    private static Map<String, PrintWriter> clientWriters = new HashMap<>();

    // Main fonksiyon ve önden belirlenmiş credentiallar, daha yüksek bir versiyonda
    // database gerekli.
    public static void main(String[] args) throws IOException {

        userCredentials.put("kullanıcı", "parola");
        userCredentials.put("kullanıcı2", "parola");
        userCredentials.put("kullanıcı3", "parola");

        System.out.println("Chat sunucusu başlatıldı...");
        ServerSocket serverSocket = new ServerSocket(PORT);
        try {
            // Sunucu başarıyla çalıştığında yapılacaklar
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Kullanıcı doğrulaması
                while (true) {
                    out.println("SUBMITNAME");
                    username = in.readLine();
                    out.println("SUBMITPASSWORD");
                    String password = in.readLine();
                    if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
                        synchronized (clientWriters) {
                            if (!clientWriters.containsKey(username)) {
                                clientWriters.put(username, out);
                                break;
                            }
                        }
                    } else {
                        out.println("INVALIDCREDENTIALS");
                    }
                }
                out.println("NAMEACCEPTED " + username);
                updateClientsUserList();

                // Mesajlar
                String message;

                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/w ")) {
                        int spaceIndex = message.indexOf(' ', 3);
                        if (spaceIndex != -1) {
                            String targetUser = message.substring(3, spaceIndex);
                            String privateMessage = message.substring(spaceIndex + 1);
                            sendPrivateMessage(username, targetUser, privateMessage);
                        }
                    } else {
                        broadcastMessage(username, message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                if (username != null) {
                    clientWriters.remove(username);
                    updateClientsUserList();
                    broadcastMessage("SUNUCU", username + " sunucudan ayrıldı.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        // Herkese yayınlanacak mesaj
        private void broadcastMessage(String sender, String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println("MESSAGE " + sender + ": " + message);
                }
            }
        }

        // Özel Mesaj
        private void sendPrivateMessage(String sender, String targetUser, String message) {
            synchronized (clientWriters) {
                PrintWriter writer = clientWriters.get(targetUser);
                if (writer != null) {
                    writer.println("PRIVATE " + sender + ": " + message);
                    out.println("PRIVATE " + sender + " -> " + targetUser + ": " + message);
                } else {
                    out.println("Hata: Kullanıcı " + targetUser + " bulunamadı.");
                }
            }
        }

        private void updateClientsUserList() {
            synchronized (clientWriters) {
                StringBuilder userList = new StringBuilder("USERLIST");
                for (String user : clientWriters.keySet()) {
                    userList.append(" ").append(user);
                }
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(userList.toString());
                }
            }
        }
    }
}
