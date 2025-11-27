import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class supplier {
    private DefaultTableModel model;
    private JTable table;
    private String currentUser;
    private boolean isInternalUpdate = false;

    public supplier(String username) {
        //adds username and role
        this.currentUser  = username.contains("(") ? username : username;

        //frame components
        JFrame frame = new JFrame("Supplier");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);

        //right panel
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBackground(new Color(1, 68, 33));
        sidebar.setBounds(0, 0, 200, 650);

        //btn + action listener
        JButton btnSale = new JButton("SALES");
        btnSale.setBounds(30, 160, 140, 40);
        sidebar.add(btnSale);
        btnSale.addActionListener(e -> {
            new Sales(currentUser);
            frame.dispose();
        });

        JButton btnInv = new JButton("INVENTORY");
        btnInv.setBounds(30, 220, 140, 40);
        sidebar.add(btnInv);

        btnInv.addActionListener(e -> {
            new Inventory(currentUser, "admin");
            frame.dispose();
        });

        JButton btnReport = new JButton("REPORT");
        btnReport.setBounds(30, 550, 140, 40);
        sidebar.add(btnReport);
        btnReport.addActionListener(e -> generateReport());

        //btn, text field
        JButton btnCancel = new JButton("EXIT");
        btnCancel.setBounds(30, 280, 140, 40);
        sidebar.add(btnCancel);
        btnCancel.addActionListener(e -> frame.dispose());

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setBounds(220, 0, 60, 25);
        frame.add(searchLabel);

        JTextField search = new JTextField();
        search.setBounds(270, 3, 150, 25);
        frame.add(search);

        frame.add(sidebar);

        //column names for the table
        String[] columnNames = {"ID", "PRODUCT NAME", "DOP", "SUPPLIER", "USER", "PURCHASED"};
        model = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3 || column == 5;
            }
        };

        //combo box for the supplier column
        table = new JTable(model) {
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 3) {
                    String[] suppliers = {"NCCC", "GMALL", "GMARKET", "FELCRIS", "GAISANO", "CONVENIENCE", "PALENGKE"};
                    return new DefaultCellEditor(new JComboBox<>(suppliers));
                }
                return super.getCellEditor(row, column);
            }
        };

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        //search bar
        search.getDocument().addDocumentListener(new DocumentListener() {
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
                String text = search.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 1));
                }
            }
        });

        //listens to the updates per column
        model.addTableModelListener(e -> {
            if (isInternalUpdate || e.getType() != TableModelEvent.UPDATE) return;

            int row = e.getFirstRow();
            int col = e.getColumn();

            if (col == 2 || col == 3 || col == 5) {
                isInternalUpdate = true;

                model.setValueAt(currentUser, row, 4);

                if (col == 2) {
                    Object dateValue = model.getValueAt(row, 2);
                    String dateStr = dateValue != null ? dateValue.toString().trim() : "";

                    //date format and restrictions
                    if (dateStr.isEmpty()) {
                        String currentDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
                        model.setValueAt(currentDate, row, 2);
                    } else {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
                            sdf.setLenient(false);
                            sdf.parse(dateStr);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Invalid date format. Use MM-dd-yyyy");
                            model.setValueAt("", row, 2);
                        }
                    }
                }
                writeSupplyFile();

                //confirmation per column
                JOptionPane.showMessageDialog(null,
                        (col == 2 ? "DOP" : col == 3 ? "SUPPLIER" : "PURCHASED") +
                                " updated by: " + currentUser);

                isInternalUpdate = false;
            }
        });

        //scrollable panel
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(220, 30, 640, 500);
        frame.add(scrollPane);

        refreshTable();
        frame.setVisible(true);

        //deselect table row and column
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

    //refreshes the table
    private void refreshTable() {
        model.setRowCount(0);
        loadFromSupply();
        lmiItems();
    }

    //loads and clean supply data records
    private void loadFromSupply() {
        Set<String> existingIds = new HashSet<>();
        Map<String, String> productNameToId = new HashMap<>();
        Set<String> validIdsFromInventory = new HashSet<>();

        try (Scanner invScanner = new Scanner(new File("inventory.txt"))) {
            while (invScanner.hasNextLine()) {
                String[] inv = invScanner.nextLine().split(",", -1);
                if (inv.length >= 2) {
                    String id = inv[0].trim();
                    String name = inv[1].trim().toLowerCase();
                    productNameToId.put(name, id);
                    validIdsFromInventory.add(id);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading inventory.txt");
        }

        List<String> cleanedSupplyLines = new ArrayList<>();

        File supplyFile = new File("supply.txt");
        if (supplyFile.exists()) {
            try (Scanner scanner = new Scanner(supplyFile)) {
                while (scanner.hasNextLine()) {
                    String[] data = scanner.nextLine().split(",", -1);
                    if (data.length >= 6) {
                        String id = data[0].trim();
                        if (validIdsFromInventory.contains(id)) {
                            model.addRow(new Object[]{
                                    id, data[1].trim(), data[2].trim(),
                                    data[3].trim(), data[4].trim(), data[5].trim()
                            });
                            cleanedSupplyLines.add(String.join(",", data));
                            existingIds.add(id);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error reading supply.txt");
            }

            try (FileWriter writer = new FileWriter("supply.txt")) {
                for (String line : cleanedSupplyLines) {
                    writer.write(line + "\n");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error updating supply.txt");
            }
        }

        File salesFile = new File("sales.txt");
        if (salesFile.exists()) {
            String today = new SimpleDateFormat("MM-dd-yyyy").format(new Date());

            try (Scanner scanner = new Scanner(salesFile)) {
                while (scanner.hasNextLine()) {
                    String[] data = scanner.nextLine().split(",", -1);
                    if (data.length >= 6) {
                        String productName = data[2].trim().toLowerCase();
                        String quantity = data[4].trim();
                        String realProductId = productNameToId.getOrDefault(productName, null);

                        if (realProductId != null && validIdsFromInventory.contains(realProductId)
                                && !existingIds.contains(realProductId)) {
                            model.addRow(new Object[]{
                                    realProductId, productName, today, "", currentUser, quantity
                            });
                            existingIds.add(realProductId);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error reading sales.txt");
            }
        }
    }

//saves current supply table to file
    private void writeSupplyFile() {
        try (FileWriter writer = new FileWriter("supply.txt")) {
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < model.getColumnCount(); j++) {
                    line.append(model.getValueAt(i, j));
                    if (j < model.getColumnCount() - 1) line.append(",");
                }
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving supply.txt file.");
        }
    }

    //generates the daily report
    private void generateReport() {
        StringBuilder report = new StringBuilder();
        double totalSales = 0;

        // Header
        report.append("DAILY REPORT\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a");
        report.append("Date & Time: ").append(java.time.LocalDateTime.now().format(formatter)).append("\n");
        report.append("Generated by: ").append(currentUser).append("\n\n");

        // -------- INVENTORY --------
        report.append("-------- INVENTORY --------\n");
        report.append(String.format("| %-6s | %-15s | %-7s | %-12s | %-12s | %-15s | %-10s |\n",
                "ID", "Name", "OnHand", "Status", "Category", "Edited By", "EXP"));

        try (Scanner scanner = new Scanner(new File("inventory.txt"))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",", -1);
                if (parts.length >= 7) {
                    report.append(String.format("| %-6s | %-15s | %-7s | %-12s | %-12s | %-15s | %-10s |\n",
                            parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
                }
            }
        } catch (IOException e) {
            report.append("Error loading inventory.txt\n");
        }

        report.append("\n");

        // -------- SALES --------
        report.append("-------- SALES --------\n");
        report.append(String.format("| %-10s | %-8s | %-8s | %-8s | %-10s | %-12s | %-12s | %-15s |\n",
                "Supply ID", "Item ID", "Cost", "SoldQty", "SP", "Subtotal", "Date", "Sold By"));

        try (Scanner scanner = new Scanner(new File("sales.txt"))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",", -1);
                if (parts.length >= 9) {
                    try {
                        int soldQty = Integer.parseInt(parts[3].trim());
                        double cost = Double.parseDouble(parts[4].trim());
                        double sp = Double.parseDouble(parts[5].trim());
                        double subtotal = soldQty * sp;
                        totalSales += subtotal;

                        report.append(String.format("| %-10s | %-8s | %-8.2f | %-8d | %-10.2f | %-12.2f | %-12s | %-15s |\n",
                                parts[0], parts[1], cost, soldQty, sp, subtotal, parts[7], parts[8]));
                    } catch (NumberFormatException e) {
                        report.append(String.format("| %-10s | %-8s | %-8s | %-8s | %-10s | %-12s | %-12s | %-15s |\n",
                                parts[0], parts[1], parts[3], parts[4], parts[5], "ERROR", parts[7], parts[8]));

                    }
                }
            }
        } catch (IOException e) {
            report.append("Error loading sales.txt\n");
        }

        report.append("\nTOTAL SALES: ₱").append(String.format("%.2f", totalSales)).append("\n\n");

        // -------- SUPPLY --------
        report.append("-------- SUPPLY --------\n");
        report.append(String.format("| %-6s | %-15s | %-12s | %-15s | %-15s | %-10s |\n",
                "ID", "Product", "Date", "Supplier", "User", "Qty"));

        try (Scanner scanner = new Scanner(new File("supply.txt"))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",", -1);
                if (parts.length >= 6) {
                    report.append(String.format("| %-6s | %-15s | %-12s | %-15s | %-15s | %-10s |\n",
                            parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                }
            }
        } catch (IOException e) {
            report.append("Error loading supply.txt\n");
        }

        // Save to file
        try (FileWriter writer = new FileWriter("daily_report.txt")) {
            writer.write(report.toString());
            JOptionPane.showMessageDialog(null, "Report saved");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error writing daily_report.txt");
        }

        //design
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        JOptionPane.showMessageDialog(null, scrollPane, "DAILY REPORT", JOptionPane.INFORMATION_MESSAGE);
    }

    //sync w the inventory
    public void lmiItems() {
        Set<String> loadids = new HashSet<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            loadids.add(model.getValueAt(i, 0).toString().trim());
        }

        Set<String> validIdsFromInventory = new HashSet<>();
        try (Scanner invscanner = new Scanner(new File("inventory.txt"))) {
            while (invscanner.hasNextLine()) {
                String[] data = invscanner.nextLine().split(",", -1);
                if (data.length >= 2) {
                    String id = data[0].trim();
                    String name = data[1].trim();
                    validIdsFromInventory.add(id);

                    if (!loadids.contains(id)) {
                        model.addRow(new Object[]{id, name, "", "", "", ""});
                    }
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error reading the text file");
        }
    }
}
//Created by: Lynn Angela C. Lawagon - A124