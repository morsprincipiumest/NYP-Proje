import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.geom.RoundRectangle2D;

public class ChatClient {
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Chatty");
    private JTextField textField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(10, 40);
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);

    public ChatClient() {

        // GUI kısmı
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Çerçevesiz ekran yapma
        frame.setUndecorated(true);

        // Yuvarlak Kenarlar
        frame.setShape(new RoundRectangle2D.Double(0, 0, 800, 600, 20, 20));

        // Başlık Chatty
        frame.getContentPane().add(createTitleBar(), BorderLayout.NORTH);

        // Component ayarları
        Font font = new Font("Ubuntu", Font.BOLD, 16);
        Color backgroundColor = new Color(32, 34, 37);
        Color foregroundColor = new Color(220, 221, 222);
        Color textFieldColor = new Color(54, 57, 62);

        frame.getContentPane().setBackground(backgroundColor);
        textField.setBackground(textFieldColor);
        textField.setForeground(foregroundColor);
        textField.setFont(font);
        messageArea.setBackground(backgroundColor);
        messageArea.setForeground(foregroundColor);
        messageArea.setFont(font);
        userList.setBackground(backgroundColor);
        userList.setForeground(foregroundColor);
        userList.setFont(font);

        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(200, 0));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(backgroundColor);
        bottomPanel.add(textField, BorderLayout.CENTER);
        JButton sendButton = new JButton("Gönder");
        sendButton.setBackground(textFieldColor);
        sendButton.setForeground(foregroundColor);
        sendButton.setFont(font);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(messageScrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(userScrollPane, BorderLayout.EAST);

        frame.setVisible(true);

        // Textfieldda yazı yazdığında güncellenmesi
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });

        // Send butonuna basınca göndermesi
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private JPanel createTitleBar() {

        // Jpanel başlık ayarları
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(47, 49, 54));
        titleBar.setPreferredSize(new Dimension(frame.getWidth(), 40));

        JLabel titleLabel = new JLabel("Chatty");
        titleLabel.setFont(new Font("Ubuntu", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Mouse ile sürüklemek için (çerçevesiz ekranda bug oluyordu, muhtemelen başka
        // bir yöntemi daha vardır)
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                frame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }

            public void mouseReleased(MouseEvent e) {
                frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                frame.setLocation(frame.getLocation().x + e.getX() - frame.getWidth() / 2,
                        frame.getLocation().y + e.getY() - frame.getHeight() / 2);
            }
        });

        titleBar.add(titleLabel, BorderLayout.CENTER);
        return titleBar;
    }

    // Sunucu ile ilgili alanlar
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Bağlanmak istediğiniz sunucunun IP adresini girin:",
                "Hoş geldiniz...",
                JOptionPane.QUESTION_MESSAGE);
    }

    // Simple authentication alanı
    private String[] getCredentials() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JLabel userLabel = new JLabel("Kullanıcı Adı:");
        JTextField username = new JTextField();
        JLabel passLabel = new JLabel("Şifre:");
        JPasswordField password = new JPasswordField();
        panel.add(userLabel);
        panel.add(username);
        panel.add(passLabel);
        panel.add(password);

        int option = JOptionPane.showConfirmDialog(frame, panel, "Giriş yap", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            return new String[] { username.getText(), new String(password.getPassword()) };
        }
        return null;
    }

    // Sunucu bağlanma ayarları (port kısmı input yapılabilir)
    private void run() throws IOException {
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 12345);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Sunucudan bilgi paketi geldiği anda yapılacaklar
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                String[] credentials = getCredentials();
                if (credentials != null) {
                    out.println(credentials[0]);
                    out.println(credentials[1]);
                }
            } else if (line.startsWith("INVALIDCREDENTIALS")) {
                JOptionPane.showMessageDialog(frame, "Bilgiler doğrulanamadı tekrar deneyin.");
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("USERLIST")) {
                String[] users = line.substring(9).split(" ");
                userListModel.clear();
                for (String user : users) {
                    userListModel.addElement(user);
                }
            } else if (line.startsWith("PRIVATE")) {
                messageArea.append("Özel mesaj " + line.substring(8) + "\n");
            } else if (line.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(frame, line.substring(6));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.run();
    }
}
