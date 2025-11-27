import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Sales {
    private DefaultTableModel model;
    private JTable table;
    private JLabel totalLabel;
    private int supplyIdCounter = 1;
    private boolean isUpdating = false;
    private String currentUser;
    private String currentRole;
    private TableRowSorter<DefaultTableModel> sorter;
    private boolean isInternalUpdate = false;


    public Sales(String username) {
        //adds the username and role
        this.currentUser  = username.contains("(") ? username : username;

        //frame components
        JFrame frame = new JFrame("Sales");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);

        //right panel
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBackground(new Color(1, 68, 33));
        sidebar.setBounds(0, 0, 200, 650);

        //btn inevtnory
        JButton btninv = new JButton("INVENTORY");
        btninv.setBounds(30, 160, 140, 40);
        sidebar.add(btninv);
        btninv.addActionListener(e -> {
            new Inventory(currentUser, "admin");
            frame.dispose();
        });

        //search field
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setBounds(220, 0, 60, 25);
        frame.add(searchLabel);

        JTextField search = new JTextField();
        search.setBounds(270, 3, 150, 25);
        frame.add(search);

        //btn supp amd exit - w/ action listener
        JButton btnsupp = new JButton("SUPPLIER");
        btnsupp.setBounds(30, 220, 140, 40);
        sidebar.add(btnsupp);
        btnsupp.addActionListener(e -> {
            new supplier(currentUser);
            frame.dispose();
        });

        JButton btncancel = new JButton("EXIT");
        btncancel.setBounds(30, 280, 140, 40);
        sidebar.add(btncancel);
        btncancel.addActionListener(e -> frame.dispose());

        frame.add(sidebar);

        //total sales label at the bottom
        totalLabel = new JLabel("TOTAL SALES: ₱0.00");
        totalLabel.setBounds(250, 550, 400, 30);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 24));
        frame.add(totalLabel);

        //column names for the table
        String[] columnNames = {"SUPPLY ID", "ITEM ID", "ITEM NAME", "SOLD QTY", "COST", "SP", "SUBTOTAL", "SOLD DATE", "SOLD BY"};

        //can only edit sold qty, cost, sp, and sold date
        model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4 || column == 5 || column == 7;
            }
        };

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        //changes to the table
        model.addTableModelListener(e -> {
            if (isInternalUpdate || e.getType() != TableModelEvent.UPDATE) return;
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();

                //checks column
                if (col == 3 || col == 4 || col == 5 || col == 7) {
                    isInternalUpdate = true;
                    model.setValueAt(currentUser, row, 8); //sold by - column 8

                    Object soldDateValue = model.getValueAt(row, 7);
                    String dateStr = soldDateValue != null ? soldDateValue.toString().trim() : "";

                    //date format and restrictions
                    if (dateStr.isEmpty()) {
                        String currentDate = new java.text.SimpleDateFormat("MM-dd-yyyy").format(new java.util.Date());
                        model.setValueAt(currentDate, row, 7);
                    } else if (!dateStr.matches("[0-9/\\-\\.]+")) {
                        JOptionPane.showMessageDialog(null, "Invalid SOLD DATE. Please use Numeric Date Format");
                        model.setValueAt("", row, 7);
                    }
                    writeSalesFile();

                    //notify who changed the item
                    JOptionPane.showMessageDialog(null,
                            (col == 3 ? "SOLD QTY" : col == 4 ? "COST" : col == 5 ? "SELLING PRICE" : "SOLD DATE") +
                                    " updated by: " + currentUser);
                    isInternalUpdate = false;
                }
            }
        });

        //search bar
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }

            private void filterTable() {
                String text = search.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2));
                }
            }
        });

        //scrollable panel
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(220, 30, 640, 500);
        frame.add(scrollPane);

        //listens to the changes
        table.getModel().addTableModelListener(e -> {
            if (!isUpdating && e.getColumn() >= 3 && e.getColumn() <= 5) {
                int row = e.getFirstRow();
                updateSubtotals(row);
            }
        });

        loadSalesFile();
        updateTotalSales();

        frame.setVisible(true);

        //clears table selection
        frame.getContentPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component clickedComponent = frame.getContentPane().getComponentAt(e.getPoint());
                if (!(clickedComponent instanceof JTable) && !(SwingUtilities.getAncestorOfClass(JTable.class, clickedComponent) != null)) {
                    table.clearSelection();
                }
            }
        });
    }

    //calculates or recalculates the subtotal
    private void updateSubtotals(int rowToUpdate) {
        if (isUpdating) return;
        isUpdating = true;

        try {
            int quantity = Integer.parseInt(model.getValueAt(rowToUpdate, 3).toString());
            double sellingPrice = Double.parseDouble(model.getValueAt(rowToUpdate, 5).toString());
            double subtotal = quantity * sellingPrice;

            model.setValueAt(String.format("%.2f", subtotal), rowToUpdate, 6);

            String itemId = model.getValueAt(rowToUpdate, 1).toString();
            updateInventoryQuantity(itemId);

            model.setValueAt(currentUser, rowToUpdate, 8);

        } catch (Exception ex) {
            model.setValueAt("", rowToUpdate, 6);
        }

        updateTotalSales();
        writeSalesFile();
        isUpdating = false;
    }

    //updates on hand and status in the inv txt
    private void updateInventoryQuantity(String itemId) {
        File inventoryFile = new File("inventory.txt");
        File supplyFile = new File("supply.txt");
        File salesFile = new File("sales.txt");

        int totalPurchased = 0;
        int totalSold = 0;

        try (Scanner scanner = new Scanner(supplyFile)) {
            while (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(",", -1);
                if (data.length >= 6 && data[0].equalsIgnoreCase(itemId)) {
                    totalPurchased += Integer.parseInt(data[5].trim());
                }
            }
        } catch (IOException ignored) {}

        try (Scanner scanner = new Scanner(salesFile)) {
            while (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(",", -1);
                if (data.length >= 4 && data[1].equalsIgnoreCase(itemId)) {
                    totalSold += Integer.parseInt(data[3].trim());
                }
            }
        } catch (IOException ignored) {}

        int newQuantity = totalPurchased - totalSold;
        String newStatus = newQuantity == 0 ? "Out of Stock" : newQuantity < 100 ? "Low Stock" : "In Stock";

        List<String> updatedLines = new ArrayList<>();
        try (Scanner scanner = new Scanner(inventoryFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",", -1);
                if (parts.length >= 5 && parts[0].trim().equalsIgnoreCase(itemId)) {
                    parts[2] = String.valueOf(newQuantity);
                    parts[3] = newStatus;
                    line = String.join(",", parts);
                }
                updatedLines.add(line);
            }
        } catch (IOException ignored) {}

        try (FileWriter writer = new FileWriter(inventoryFile)) {
            for (String l : updatedLines) {
                writer.write(l + "\n");
            }
        } catch (IOException ignored) {}
    }

    //saves the sales data to the sales txt
    private void writeSalesFile() {
        try (FileWriter writer = new FileWriter("sales.txt")) {
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < model.getColumnCount(); j++) {
                    line.append(model.getValueAt(i, j));
                    if (j < model.getColumnCount() - 1) line.append(",");
                }
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //load sales from the sales txt but only is the product exists in the inventory txt
    private void loadSalesFile() {
        File salesFile = new File("sales.txt");
        File inventoryFile = new File("inventory.txt");

        Set<String> validItemIds = new HashSet<>();
        Map<String, String> itemIdToName = new HashMap<>();
        Map<String, String[]> salesData = new HashMap<>();

        if (inventoryFile.exists()) {
            try (Scanner inventoryScanner = new Scanner(inventoryFile)) {
                while (inventoryScanner.hasNextLine()) {
                    String[] data = inventoryScanner.nextLine().split(",", -1);
                    if (data.length >= 2) {
                        String itemId = data[0].trim();
                        String itemName = data[1].trim();
                        validItemIds.add(itemId);
                        itemIdToName.put(itemId, itemName);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error reading inventory.txt");
                return;
            }
        }

        if (salesFile.exists()) {
            try (Scanner salesScanner = new Scanner(salesFile)) {
                while (salesScanner.hasNextLine()) {
                    String[] data = salesScanner.nextLine().split(",", -1);
                    if (data.length >= 9) {
                        String itemId = data[1].trim();
                        if (validItemIds.contains(itemId)) {
                            salesData.put(itemId, data);
                            try {
                                int currentId = Integer.parseInt(data[0].substring(1));
                                supplyIdCounter = Math.max(supplyIdCounter, currentId + 1);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error reading sales.txt");
                return;
            }

            try (FileWriter writer = new FileWriter("sales.txt")) {
                for (String[] validRow : salesData.values()) {
                    writer.write(String.join(",", validRow) + "\n");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error updating sales.txt");
            }
        }

        for (String itemId : validItemIds) {
            if (salesData.containsKey(itemId)) {
                String[] sale = salesData.get(itemId);
                model.addRow(new Object[]{
                        sale[0], sale[1], sale[2], sale[3], sale[4], sale[5], sale[6], sale[7], sale[8]
                });
            } else {
                String supplyId = String.format("S%03d", supplyIdCounter++);
                model.addRow(new Object[]{
                        supplyId, itemId, itemIdToName.get(itemId), "", "", "", "", ""
                });
            }
        }
    }

    //recalculates total sales and saves to the file
    private void updateTotalSales() {
        double total = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                total += Double.parseDouble(model.getValueAt(i, 6).toString());
            } catch (Exception ignored) {}
        }
        //label
        totalLabel.setText("TOTAL SALES: ₱" + String.format("%.2f", total));
        //saves to file
        try (FileWriter writer = new FileWriter("total_sales.txt")) {
            writer.write(String.format("%.2f", total));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
//Created by: Lynn Angela C. Lawagon - A124