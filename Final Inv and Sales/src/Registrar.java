import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;

public class Registrar {
    Registrar() {
        //frame components
        JFrame reg = new JFrame("Registration");
        reg.setSize(400, 300);
        reg.setLayout(null);
        reg.setResizable(false);
        reg.setLocationRelativeTo(null);
        reg.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        reg.getContentPane().setBackground(new Color(200, 200, 200));

        //text fields, label, combobox
        JLabel userLabel = new JLabel("Enter Username:");
        userLabel.setBounds(50, 50, 120, 25);
        reg.add(userLabel);

        JTextField userTextField = new JTextField();
        userTextField.setBounds(180, 50, 150, 25);
        userTextField.setBackground(new Color(211, 211, 211));
        reg.add(userTextField);

        JLabel passLabel = new JLabel("Enter Password:");
        passLabel.setBounds(50, 100, 120, 25);
        reg.add(passLabel);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(180, 100, 150, 25);
        passwordField.setBackground(new Color(211, 211, 211));
        reg.add(passwordField);

        JComboBox select= new JComboBox();
        select.addItem("Select a role");
        select.addItem("Admin");
        select.addItem("User");
        select.setBounds(50, 140, 120, 25);
        select.setBackground(new Color(169, 169, 169));
        reg.add(select);

        //see password
        JCheckBox seepass = new JCheckBox("show password");
        char defaultEchoChar = passwordField.getEchoChar();
        seepass.setBounds(180, 130, 150, 20);
        seepass.setBackground(new Color(200, 200, 200));
        reg.add(seepass);

        seepass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (seepass.isSelected()){
                    passwordField.setEchoChar((char) 0);
                } else {
                    passwordField.setEchoChar(defaultEchoChar);
                }
            }
        });

        //btn ok button and action listener + saves the credentials based from the role
        JButton okButton = new JButton("OK");
        okButton.setBounds(150, 190, 100, 30);
        okButton.setBackground(new Color(169, 169, 169));
        reg.add(okButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userTextField.getText();
                String password = new String(passwordField.getPassword());
                String role = (String) select.getSelectedItem();

                if("Select".equals(role)){
                    JOptionPane.showMessageDialog(reg,"Please select a role.");
                    return;
                }

                if(username.isEmpty() || password.isEmpty() || role.isEmpty()){
                    JOptionPane.showMessageDialog(reg, "Please fill in all fields.");
                    return;
                }
                String filename = role.equals("Admin") ? "admin.txt" : "user.txt";

                try(FileWriter write = new FileWriter(filename, true)){
                    write.write(username + "," +  password + "\n");
                    JOptionPane.showMessageDialog(reg, "You have successfully registered!");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(reg, "Error 101...");
                    ex.printStackTrace();;
                }
                reg.dispose();
                new LoginSys();
            }
        });
        reg.setVisible(true);
    }
}
//Created by: Lynn Angela C. Lawagon - A124