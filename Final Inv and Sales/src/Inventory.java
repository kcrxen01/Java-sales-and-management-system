import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Inventory {
    private JTextField idTextField, nameTextField, qtyTextField, searchTextField, expTextField;
    private JComboBox<String> categoryComboBox;
    private DefaultTableModel model;
    private final String currentUser ;
    private final Map<String, String> originalModifiedBy = new HashMap<>();

    public Inventory(String username, String role) {

        //adds the admin's username and role to the table
        this.currentUser = username.contains("(") ? username : username + " (" + role + ")";

        //frame components
        JFrame frame = new JFrame("Inventory");
        frame.setSize(900, 650);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // rightpanel
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBackground(new Color(1, 68, 33));
        sidebar.setBounds(0, 0, 200, 650);

        //buttons
        JButton btnsales = new JButton("SALES");
        btnsales.setBounds(30, 160, 140, 40);
        sidebar.add(btnsales);
        btnsales.addActionListener(e -> {
            new Sales(currentUser);
            frame.dispose();
        });

        JButton btnsupp = new JButton("SUPPLIER");
        btnsupp.setBounds(30, 220, 140, 40);
        sidebar.add(btnsupp);
        btnsupp.addActionListener(e -> {
            new supplier(currentUser);
            frame.dispose();
        });

        //hides the supplier and sales button if the role is user
        if (role.equalsIgnoreCase("user")) {

            btnsales.setVisible(false);
            btnsupp.setVisible(false);
        }


        JButton btncancel = new JButton("EXIT");
        btncancel.setBounds(30, 280, 140, 40);
        sidebar.add(btncancel);
        btncancel.addActionListener(e -> frame.dispose());

        frame.add(sidebar);

        // Fields and combo box
        JLabel idLabel = new JLabel("Barcode:");
        idLabel.setBounds(220, 30, 100, 25);
        frame.add(idLabel);

        idTextField = new JTextField();
        idTextField.setBounds(310, 30, 150, 25);
        idTextField.setEditable(false);
        idTextField.setFocusable(false);
        frame.add(idTextField);

        JLabel search = new JLabel("Search Bar:");
        search.setBounds(500, 30, 100, 25);
        frame.add(search);

        searchTextField = new JTextField();
        searchTextField.setBounds(580, 30, 150, 25);
        frame.add(searchTextField);

        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setBounds(220, 70, 100, 25);
        frame.add(nameLabel);

        nameTextField = new JTextField();
        nameTextField.setBounds(310, 70, 150, 25);
        frame.add(nameTextField);

        JLabel qtyLabel = new JLabel("On Hand:");
        qtyLabel.setBounds(220, 110, 100, 25);
        frame.add(qtyLabel);

        qtyTextField = new JTextField("0");
        qtyTextField.setBounds(310, 110, 150, 25);
        qtyTextField.setEditable(false);
        qtyTextField.setFocusable(false);
        frame.add(qtyTextField);

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setBounds(220, 150, 100, 25);
        frame.add(categoryLabel);

        categoryComboBox = new JComboBox<>(new String[]{
                " ", "Fruits", "Vegetables", "Meat", "Seafoods", "Dairy", "Bakery", "Dry goods",
                "Pantry items", "Frozen Foods", "Beverages", "Junk foods", "Snacks", "Supplies",
                "Hygiene", "Condiments", "Spices", "Baby products", "Pet Supplies"
        });
        categoryComboBox.setBounds(310, 150, 150, 25);
        frame.add(categoryComboBox);

        JLabel expLabel = new JLabel("Expiration:");
        expLabel.setBounds(220, 190, 100, 30);
        frame.add(expLabel);

        //adds a date "label" to the expTextField
        expTextField = new JTextField();
        expTextField.setText("MM - DD - YYYY");
        expTextField.setForeground(Color.GRAY);

        expTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (expTextField.getText().equals("MM - DD - YYYY")) {
                    expTextField.setText("");
                    expTextField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (expTextField.getText().isEmpty()) {
                    expTextField.setText("MM - DD - YYYY");
                    expTextField.setForeground(Color.GRAY);
                }
            }
        });

        expTextField.setBounds(310, 190, 150, 25);
        frame.add(expTextField);

        //buttons
        JButton btnadd = new JButton("ADD");
        btnadd.setBounds(580, 190, 90, 30);
        frame.add(btnadd);

        JButton btnedit = new JButton("EDIT");
        btnedit.setBounds(675, 190, 90, 30);
        frame.add(btnedit);

        JButton btndel = new JButton("DELETE");
        btndel.setBounds(770, 190, 90, 30);
        frame.add(btndel);

        //for the table labels
        String[] columns = {"ID", "NAME", "ON HAND", "CATEGORY", "STATUS", "MODIFIED BY", "EXP"};
        //prevents the user to edit in the table
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        //table
        JTable table = new JTable(model);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        //search bar purpose
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }

            private void filterTable() {
                String text = searchTextField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1));
                }
            }
        });

        //scrollable panel
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(220, 240, 640, 300);
        frame.add(scrollPane);

        loadInventoryData();

        // Events
        btnadd.addActionListener(e -> addProduct());

        // Inside btnedit action
        btnedit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String name = nameTextField.getText().trim();
                String exp = expTextField.getText().trim();
                String cat = (String) categoryComboBox.getSelectedItem();

                if (name.isEmpty() || cat.equals(" ") || exp.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please fill all fields");
                    return;
                }

                model.setValueAt(name, row, 1);
                model.setValueAt(cat, row, 3);
                model.setValueAt(currentUser, row, 5);  //updates modified by based from the admin
                model.setValueAt(exp, row, 6);

                String editedId = model.getValueAt(row, 0).toString(); // Get selected item's ID
                editProduct(editedId);

                JOptionPane.showMessageDialog(frame, "Edited successfully");
                table.clearSelection();
                clearFields();
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a row to edit");
            }
        });

        //btn delete
        btndel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this item?");
                if (confirm == JOptionPane.YES_OPTION) {
                    model.removeRow(row);
                    writeinvfile();
                    delprod();
                    JOptionPane.showMessageDialog(frame, "Deleted Successfully");
                    clearFields();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a row to delete");
            }
        });

        //automatically filled into the text filed and combo box
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    idTextField.setText(model.getValueAt(row, 0).toString());
                    nameTextField.setText(model.getValueAt(row, 1).toString());
                    qtyTextField.setText(model.getValueAt(row, 2).toString());
                    categoryComboBox.setSelectedItem(model.getValueAt(row, 3).toString());
                    expTextField.setText(model.getValueAt(row, 6).toString());

                }
            }
        });

        frame.setVisible(true);
        //deselect table row and column
        frame.getContentPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component clickedComponent = frame.getContentPane().getComponentAt(e.getPoint());
                if (!(clickedComponent instanceof JTable) && !(SwingUtilities.getAncestorOfClass(JTable.class, clickedComponent) != null)) {
                    table.clearSelection();
                }
                clearFields();
            }

        });

    }

    //loads inventory text field - computation for the on hand item
    private void loadInventoryData() {
        model.setRowCount(0); //clear existing table rows
        originalModifiedBy.clear();

        Map<String, String[]> inventoryMap = new HashMap<>();

        //load data from the txt file
        try (Scanner sc = new Scanner(new File("inventory.txt"))) {
            while (sc.hasNextLine()) {
                String[] data = sc.nextLine().split(",", -1);
                if (data.length >= 7) {
                    inventoryMap.put(data[0], data); //Store item by ID
                    originalModifiedBy.put(data[0], data[5]); //tracker for the admin user
                }
            }
        } catch (IOException ignored) {}

        //loads purchased in the supply txt
        Map<String, Integer> purchased = new HashMap<>();
        try (Scanner sc = new Scanner(new File("supply.txt"))) {
            while (sc.hasNextLine()) {
                String[] s = sc.nextLine().split(",", -1);
                if (s.length >= 6) {
                    String id = s[0];
                    if (!s[5].isEmpty()) {
                        try {
                            int qty = Integer.parseInt(s[5]); //parse purchased column
                            purchased.put(id, purchased.getOrDefault(id, 0) + qty); //sum quantites
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid quantity in supply.txt for ID: " + id);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        //loads sold qty from the sales txt
        Map<String, Integer> sold = new HashMap<>();
        try (Scanner sc = new Scanner(new File("sales.txt"))) {
            while (sc.hasNextLine()) {
                String[] s = sc.nextLine().split(",", -1);
                if (s.length >= 5) {
                    String id = s[1];
                    if (!s[3].isEmpty()) {
                        try {
                            int qty = Integer.parseInt(s[3]); //parse sold qty
                            sold.put(id, sold.getOrDefault(id, 0) + qty); //sum quantities per ID
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid quantity in sales.txt for ID: " + id);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        //computes the data and display the result
        for (String id : inventoryMap.keySet()) {
            String[] data = inventoryMap.get(id);
            String name = data[1];
            String cat = data[4];
            String modifiedBy = data[5];

            //computes on hand item
            int onHand = purchased.getOrDefault(id, 0) - sold.getOrDefault(id, 0);
            onHand = Math.max(0, onHand);

            //stock status
            String status = onHand == 0 ? "Out of Stock" : onHand < 100 ? "Low Stock" : "In Stock";
            String expiration = data.length >= 7 ? data[6] : "";

            //adds the computed data to the table
            model.addRow(new Object[]{id, name, onHand, cat, status, modifiedBy, expiration});
        }

        idTextField.setText(barcode()); //barcode
    }

    //writes and saves current data to the inventory file
    private void writeinvfile() {
        try (FileWriter writer = new FileWriter("inventory.txt")) {
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < model.getColumnCount(); j++) {
                    line.append(model.getValueAt(i, j));
                    if (j < model.getColumnCount() - 1) line.append(",");
                }
                writer.write(line.toString() + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving inventory.txt");
        }
    }

    //add product conditions and notifications
    private void addProduct() {
        String id = idTextField.getText().trim();
        String name = nameTextField.getText().trim();
        String exp = expTextField.getText().trim();
        String cat = (String) categoryComboBox.getSelectedItem();

        if (exp.equals("MM - DD - YYYY") || exp.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Expiration date is empty");
            return;
        }

        if (id.isEmpty() || name.isEmpty() || cat.equals(" ") || exp.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please fill all fields");
            return;
        }

        //Date format validation and restriction
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        LocalDate today = LocalDate.now();
        LocalDate enteredDate;

        try {
            enteredDate = LocalDate.parse(exp, formatter);
            if (enteredDate.isBefore(today)) {
                JOptionPane.showMessageDialog(null, "Expiration date cannot be in the past.");
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(null, "Invalid date format. Please use MM-DD-YYYY.");
            return;
        }

        //add to table and save to the txt
        model.addRow(new Object[]{id, name, 0, cat, "Out of Stock", currentUser, exp});
        try (FileWriter fw = new FileWriter("inventory.txt", true)) {
            fw.write(id + "," + name + ",0,Out of Stock," + cat + "," + currentUser + "," + exp + "\n");
            JOptionPane.showMessageDialog(null, "Product added successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        clearFields();
        idTextField.setText(barcode());
    }

    //generates an unique barcode for every added item
    private String barcode() {
        int max = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            String id = model.getValueAt(i, 0).toString();
            if (id.startsWith("B")) {
                try {
                    int num = Integer.parseInt(id.substring(1));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("B%03d", max + 1);
    }

    //saves the data to the file
    private void editProduct(String editedId) {
        try (FileWriter fw = new FileWriter("inventory.txt")) {
            for (int i = 0; i < model.getRowCount(); i++) {
                String id = model.getValueAt(i, 0).toString();
                String name = model.getValueAt(i, 1).toString();
                int qty = Integer.parseInt(model.getValueAt(i, 2).toString());
                String cat = model.getValueAt(i, 3).toString();
                String status = model.getValueAt(i, 4).toString();
                String exp = model.getValueAt(i, 6).toString();

                //modified by
                String modifiedBy;
                if (id.equals(editedId)) {
                    modifiedBy = currentUser;
                } else {
                    modifiedBy = originalModifiedBy.getOrDefault(id, model.getValueAt(i, 5).toString());
                }

                fw.write(id + "," + name + "," + qty + "," + status + "," + cat + "," + modifiedBy + "," + exp + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void delprod() {
        writeinvfile();
    }

    //resets all input fields
    private void clearFields() {
        idTextField.setText(barcode());

        nameTextField.setText("");
        qtyTextField.setText("0");
        categoryComboBox.setSelectedIndex(0);

        expTextField.setText("MM - DD - YYYY");
        expTextField.setForeground(Color.GRAY);
    }
}
//Created by: Lynn Angela C. Lawagon - A124