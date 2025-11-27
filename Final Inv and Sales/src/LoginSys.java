import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LoginSys {

    LoginSys() {
        //frame components
        JFrame frame = new JFrame("Login Page");
        frame.setSize(500, 450);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.getContentPane().setBackground(new Color(200, 200, 200));

        //text fields
        JLabel welcome = new JLabel("SARITINDA!");
        welcome.setBounds(140, 60, 300, 50);
        welcome.setFont(new Font("Serif", Font.BOLD, 32));
        welcome.setForeground(Color.black);
        frame.add(welcome);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(90, 150, 100, 30);
        userLabel.setForeground(Color.black);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(userLabel);

        JTextField userTextField = new JTextField();
        userTextField.setBounds(190, 150, 200, 30);
        userTextField.setBackground(new Color(211, 211, 211)); // Light Grey
        frame.add(userTextField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(90, 200, 100, 30);
        passLabel.setForeground(Color.black);
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(passLabel);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(190, 200, 200, 30);
        passwordField.setBackground(new Color(211, 211, 211)); // Light Grey
        frame.add(passwordField);

        //see password
        JCheckBox showPassword = new JCheckBox("Show Password");
        showPassword.setBounds(190, 235, 150, 20);
        showPassword.setBackground(new Color(200, 200, 200));
        frame.add(showPassword);

        char defaultEcho = passwordField.getEchoChar();

        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar(defaultEcho);
            }
        });

        //btn for enter and register
        JButton btnenter = new JButton("ENTER");
        btnenter.setBounds(100, 270, 120, 40);
        btnenter.setBackground(new Color(169, 169, 169)); // Medium Grey
        btnenter.setForeground(Color.BLACK);
        btnenter.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(btnenter);

        JButton btnregister = new JButton("REGISTER");
        btnregister.setBounds(250, 270, 120, 40);
        btnregister.setBackground(new Color(169, 169, 169)); // Medium Grey
        btnregister.setForeground(Color.BLACK);
        btnregister.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(btnregister);

        // Button Events
        btnregister.addActionListener(e -> {
            frame.dispose();
            new Registrar();
        });

        //brings you to your designated window based from the role
        btnenter.addActionListener(e -> {
            String username = userTextField.getText();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Invalid Credentials");
                return;
            }

            boolean admin = checkCredentials("admin.txt", username, password);
            boolean user = checkCredentials("user.txt", username, password);

            if (admin) {
                new Inventory(username, "admin");
                frame.dispose();
            } else if (user) {
                new Inventory(username, "user");
                frame.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Wrong password or username");
            }
        });

        frame.setVisible(true);
    }

    //verifies if the username and password exists in the file, for login authenitcation
    private boolean checkCredentials(String file, String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
//Created by: Lynn Angela C. Lawagon - A124