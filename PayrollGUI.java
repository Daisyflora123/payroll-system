import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PayrollGUI extends JFrame {

    private JTextField tfId, tfName, tfDesignation, tfContact, tfBasic, tfHRA, tfAllowances, tfDeductions;
    private JButton btnAdd, btnUpdate, btnDelete, btnCalculate, btnPayslip, btnDownloadPDF, btnClear, btnRefresh;
    private JTable table;
    private DefaultTableModel model;
    private Connection conn;

    public PayrollGUI() {
        super("Employee Payroll System");
        initDatabase();
        initUI();
        loadEmployees();
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initDatabase() {
        try {
            // Load MySQL JDBC Driver (for MySQL Connector 9.x)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // MySQL Connection 
            String url = "jdbc:mysql://localhost:3306/payroll_db";
            String user = "root";
            String password = "Selvaraj@27";
            
            conn = DriverManager.getConnection(url, user, password);
            
            Statement stmt = conn.createStatement();
            
            // Create Employee table
            String createEmployeeTable = "CREATE TABLE IF NOT EXISTS employee (" +
                    "employee_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "designation VARCHAR(100), " +
                    "contact_number VARCHAR(20))";
            stmt.executeUpdate(createEmployeeTable);
            
            // Create Salary table with foreign key
            String createSalaryTable = "CREATE TABLE IF NOT EXISTS salary (" +
                    "employee_id INT PRIMARY KEY, " +
                    "allowances DOUBLE DEFAULT 0, " +
                    "deductions DOUBLE DEFAULT 0, " +
                    "hra DOUBLE DEFAULT 0, " +
                    "net_pay DOUBLE DEFAULT 0, " +
                    "basic_pay DOUBLE DEFAULT 0, " +
                    "FOREIGN KEY (employee_id) REFERENCES employee(employee_id) ON DELETE CASCADE)";
            stmt.executeUpdate(createSalaryTable);
            
            stmt.close();
            
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                "MySQL JDBC Driver not found!\n\n" +
                "Please ensure mysql-connector-java JAR is in classpath.\n" +
                "Download from: https://dev.mysql.com/downloads/connector/j/",
                "Driver Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Database Connection Error: " + e.getMessage() + 
                "\n\nPlease ensure:\n" +
                "1. MySQL is running\n" +
                "2. Database 'payroll_db' exists\n" +
                "3. Credentials are correct\n" +
                "4. MySQL JDBC driver is in classpath",
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Employee ID:"), c);
        c.gridx = 1; tfId = new JTextField(15); tfId.setEditable(false); form.add(tfId, c);

        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Name:"), c);
        c.gridx = 1; tfName = new JTextField(15); form.add(tfName, c);

        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Designation:"), c);
        c.gridx = 1; tfDesignation = new JTextField(15); form.add(tfDesignation, c);

        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Contact No:"), c);
        c.gridx = 1; tfContact = new JTextField(15); form.add(tfContact, c);

        c.gridx = 0; c.gridy = 4; form.add(new JLabel("Basic Pay:"), c);
        c.gridx = 1; tfBasic = new JTextField(15); form.add(tfBasic, c);

        c.gridx = 0; c.gridy = 5; form.add(new JLabel("HRA:"), c);
        c.gridx = 1; tfHRA = new JTextField(15); form.add(tfHRA, c);

        c.gridx = 0; c.gridy = 6; form.add(new JLabel("Allowances:"), c);
        c.gridx = 1; tfAllowances = new JTextField(15); form.add(tfAllowances, c);

        c.gridx = 0; c.gridy = 7; form.add(new JLabel("Deductions:"), c);
        c.gridx = 1; tfDeductions = new JTextField(15); form.add(tfDeductions, c);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnAdd = new JButton("Add");
        btnUpdate = new JButton("Update");
        btnDelete = new JButton("Delete");
        btnCalculate = new JButton("Calculate Salary");
        btnPayslip = new JButton("View Payslip");
        btnDownloadPDF = new JButton("Download PDF");
        btnClear = new JButton("Clear");
        btnRefresh = new JButton("Refresh");

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnCalculate);
        btnPanel.add(btnPayslip);
        btnPanel.add(btnDownloadPDF);
        btnPanel.add(btnClear);
        btnPanel.add(btnRefresh);

        c.gridx = 0; c.gridy = 8; c.gridwidth = 2; form.add(btnPanel, c);
        add(form, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{
            "ID", "Name", "Designation", "Contact", "Basic", "HRA", "Allowances", "Deductions", "Net Pay"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> addEmployee());
        btnUpdate.addActionListener(e -> updateEmployee());
        btnDelete.addActionListener(e -> deleteEmployee());
        btnCalculate.addActionListener(e -> calculateSalary());
        btnPayslip.addActionListener(e -> generatePayslip());
        btnDownloadPDF.addActionListener(e -> downloadPayslipPDF());
        btnClear.addActionListener(e -> clearFields());
        btnRefresh.addActionListener(e -> loadEmployees());
        table.getSelectionModel().addListSelectionListener(e -> fillForm());
    }

    private void loadEmployees() {
        model.setRowCount(0);
        try {
            String query = "SELECT e.employee_id, e.name, e.designation, e.contact_number, " +
                          "s.basic_pay, s.hra, s.allowances, s.deductions, s.net_pay " +
                          "FROM employee e " +
                          "LEFT JOIN salary s ON e.employee_id = s.employee_id " +
                          "ORDER BY e.employee_id";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("employee_id"),
                    rs.getString("name"),
                    rs.getString("designation"),
                    rs.getString("contact_number"),
                    rs.getDouble("basic_pay"),
                    rs.getDouble("hra"),
                    rs.getDouble("allowances"),
                    rs.getDouble("deductions"),
                    rs.getDouble("net_pay")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading employees: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addEmployee() {
        if (tfName.getText().isEmpty() || tfDesignation.getText().isEmpty() || tfBasic.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Name, Designation, and Basic Pay");
            return;
        }

        try {
            conn.setAutoCommit(false); // Start transaction
            
            // Insert into employee table
            String empQuery = "INSERT INTO employee (name, designation, contact_number) VALUES (?, ?, ?)";
            PreparedStatement empStmt = conn.prepareStatement(empQuery, Statement.RETURN_GENERATED_KEYS);
            empStmt.setString(1, tfName.getText());
            empStmt.setString(2, tfDesignation.getText());
            empStmt.setString(3, tfContact.getText());
            empStmt.executeUpdate();
            
            // Get generated employee_id
            ResultSet rs = empStmt.getGeneratedKeys();
            int employeeId = 0;
            if (rs.next()) {
                employeeId = rs.getInt(1);
            }
            rs.close();
            empStmt.close();
            
            // Insert into salary table
            double basic = parseDouble(tfBasic.getText());
            double hra = parseDouble(tfHRA.getText());
            double allowances = parseDouble(tfAllowances.getText());
            double deductions = parseDouble(tfDeductions.getText());
            double netPay = basic + hra + allowances - deductions;
            
            String salQuery = "INSERT INTO salary (employee_id, basic_pay, hra, allowances, deductions, net_pay) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement salStmt = conn.prepareStatement(salQuery);
            salStmt.setInt(1, employeeId);
            salStmt.setDouble(2, basic);
            salStmt.setDouble(3, hra);
            salStmt.setDouble(4, allowances);
            salStmt.setDouble(5, deductions);
            salStmt.setDouble(6, netPay);
            salStmt.executeUpdate();
            salStmt.close();
            
            conn.commit(); // Commit transaction
            conn.setAutoCommit(true);
            
            JOptionPane.showMessageDialog(this, "Employee added successfully!");
            loadEmployees();
            clearFields();
            
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback on error
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this, "Error adding employee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateEmployee() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to update");
            return;
        }

        try {
            conn.setAutoCommit(false); // Start transaction
            
            int id = Integer.parseInt(tfId.getText());
            
            // Update employee table
            String empQuery = "UPDATE employee SET name=?, designation=?, contact_number=? WHERE employee_id=?";
            PreparedStatement empStmt = conn.prepareStatement(empQuery);
            empStmt.setString(1, tfName.getText());
            empStmt.setString(2, tfDesignation.getText());
            empStmt.setString(3, tfContact.getText());
            empStmt.setInt(4, id);
            empStmt.executeUpdate();
            empStmt.close();
            
            // Update salary table
            double basic = parseDouble(tfBasic.getText());
            double hra = parseDouble(tfHRA.getText());
            double allowances = parseDouble(tfAllowances.getText());
            double deductions = parseDouble(tfDeductions.getText());
            double netPay = basic + hra + allowances - deductions;
            
            String salQuery = "UPDATE salary SET basic_pay=?, hra=?, allowances=?, deductions=?, net_pay=? " +
                             "WHERE employee_id=?";
            PreparedStatement salStmt = conn.prepareStatement(salQuery);
            salStmt.setDouble(1, basic);
            salStmt.setDouble(2, hra);
            salStmt.setDouble(3, allowances);
            salStmt.setDouble(4, deductions);
            salStmt.setDouble(5, netPay);
            salStmt.setInt(6, id);
            salStmt.executeUpdate();
            salStmt.close();
            
            conn.commit(); // Commit transaction
            conn.setAutoCommit(true);
            
            JOptionPane.showMessageDialog(this, "Employee updated successfully!");
            loadEmployees();
            clearFields();
            
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback on error
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this, "Error updating employee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteEmployee() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to delete");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this employee?\nThis will also delete their salary records.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            int id = Integer.parseInt(tfId.getText());
            
            // Delete from employee table (salary will be deleted automatically due to CASCADE)
            String query = "DELETE FROM employee WHERE employee_id=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            pstmt.close();
            
            JOptionPane.showMessageDialog(this, "Employee deleted successfully!");
            loadEmployees();
            clearFields();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting employee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateSalary() {
        try {
            double basic = parseDouble(tfBasic.getText());
            double hra = parseDouble(tfHRA.getText());
            double allowances = parseDouble(tfAllowances.getText());
            double deductions = parseDouble(tfDeductions.getText());
            double netPay = basic + hra + allowances - deductions;
            JOptionPane.showMessageDialog(this, "Net Pay: ₹" + String.format("%.2f", netPay));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers");
        }
    }

    private void generatePayslip() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an employee from the table");
            return;
        }
        String payslip = "========== EMPLOYEE PAYSLIP ==========\n\n" +
                "Employee ID    : " + model.getValueAt(row, 0) + "\n" +
                "Name           : " + model.getValueAt(row, 1) + "\n" +
                "Designation    : " + model.getValueAt(row, 2) + "\n" +
                "Contact No     : " + model.getValueAt(row, 3) + "\n\n" +
                "---------- SALARY DETAILS ----------\n\n" +
                "Basic Pay      : ₹" + model.getValueAt(row, 4) + "\n" +
                "HRA            : ₹" + model.getValueAt(row, 5) + "\n" +
                "Allowances     : ₹" + model.getValueAt(row, 6) + "\n" +
                "Deductions     : ₹" + model.getValueAt(row, 7) + "\n\n" +
                "--------------------------------------\n" +
                "NET PAY        : ₹" + model.getValueAt(row, 8) + "\n" +
                "======================================";
        
        JTextArea area = new JTextArea(payslip);
        area.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(400, 350));
        JOptionPane.showMessageDialog(this, scrollPane, "Employee Payslip", JOptionPane.INFORMATION_MESSAGE);
    }

    private void downloadPayslipPDF() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an employee from the table to download payslip");
            return;
        }

        try {
            // Get employee data
            String empId = model.getValueAt(row, 0).toString();
            String name = model.getValueAt(row, 1).toString();
            String designation = model.getValueAt(row, 2).toString();
            String contact = model.getValueAt(row, 3).toString();
            double basic = (Double) model.getValueAt(row, 4);
            double hra = (Double) model.getValueAt(row, 5);
            double allowances = (Double) model.getValueAt(row, 6);
            double deductions = (Double) model.getValueAt(row, 7);
            double netPay = (Double) model.getValueAt(row, 8);

            // Get current date
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            String monthYear = currentDate.format(formatter);

            // Create file name
            String fileName = "Payslip_" + name.replaceAll(" ", "_") + "_" + empId + ".pdf";
            
            // File chooser for save location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new java.io.File(fileName));
            int result = fileChooser.showSaveDialog(this);
            
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".pdf")) {
                    filePath += ".pdf";
                }

                // Create PDF
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream(filePath));
                document.open();

                // Add company header
                Paragraph header = new Paragraph("EMPLOYEE PAYSLIP");
                header.setAlignment(Element.ALIGN_CENTER);
                header.setSpacingAfter(20);
                document.add(header);

                // Add month/year
                Paragraph month = new Paragraph("Pay Period: " + monthYear);
                month.setAlignment(Element.ALIGN_CENTER);
                month.setSpacingAfter(25);
                document.add(month);

                // Employee Details Table
                PdfPTable empTable = new PdfPTable(2);
                empTable.setWidthPercentage(100);
                empTable.setSpacingBefore(10);
                empTable.setSpacingAfter(20);

                addTableCell(empTable, "Employee ID:");
                addTableCell(empTable, empId);
                addTableCell(empTable, "Name:");
                addTableCell(empTable, name);
                addTableCell(empTable, "Designation:");
                addTableCell(empTable, designation);
                addTableCell(empTable, "Contact:");
                addTableCell(empTable, contact);

                document.add(empTable);

                // Salary Details Header
                Paragraph salaryHeader = new Paragraph("Salary Details");
                salaryHeader.setSpacingBefore(15);
                salaryHeader.setSpacingAfter(10);
                document.add(salaryHeader);

                // Salary Table
                PdfPTable salaryTable = new PdfPTable(2);
                salaryTable.setWidthPercentage(100);

                addTableCell(salaryTable, "Basic Pay");
                addTableCell(salaryTable, "₹" + String.format("%.2f", basic));
                addTableCell(salaryTable, "HRA");
                addTableCell(salaryTable, "₹" + String.format("%.2f", hra));
                addTableCell(salaryTable, "Allowances");
                addTableCell(salaryTable, "₹" + String.format("%.2f", allowances));
                addTableCell(salaryTable, "Deductions");
                addTableCell(salaryTable, "₹" + String.format("%.2f", deductions));

                document.add(salaryTable);

                // Net Pay
                PdfPTable netPayTable = new PdfPTable(2);
                netPayTable.setWidthPercentage(100);
                netPayTable.setSpacingBefore(15);

                addTableCell(netPayTable, "NET PAY");
                addTableCell(netPayTable, "₹" + String.format("%.2f", netPay));

                document.add(netPayTable);

                // Footer
                Paragraph footer = new Paragraph("\nThis is a computer-generated payslip and does not require a signature.");
                footer.setAlignment(Element.ALIGN_CENTER);
                footer.setSpacingBefore(30);
                document.add(footer);

                document.close();

                JOptionPane.showMessageDialog(this, 
                    "Payslip PDF downloaded successfully!\nLocation: " + filePath,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Ask if user wants to open the file
                int open = JOptionPane.showConfirmDialog(this,
                    "Would you like to open the PDF file?",
                    "Open PDF", JOptionPane.YES_NO_OPTION);
                
                if (open == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().open(new java.io.File(filePath));
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error creating PDF: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        tfId.setText(model.getValueAt(row, 0).toString());
        tfName.setText(model.getValueAt(row, 1).toString());
        tfDesignation.setText(model.getValueAt(row, 2).toString());
        tfContact.setText(model.getValueAt(row, 3).toString());
        tfBasic.setText(model.getValueAt(row, 4).toString());
        tfHRA.setText(model.getValueAt(row, 5).toString());
        tfAllowances.setText(model.getValueAt(row, 6).toString());
        tfDeductions.setText(model.getValueAt(row, 7).toString());
    }

    private void clearFields() {
        tfId.setText("");
        tfName.setText("");
        tfDesignation.setText("");
        tfContact.setText("");
        tfBasic.setText("");
        tfHRA.setText("");
        tfAllowances.setText("");
        tfDeductions.setText("");
        table.clearSelection();
    }

    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Double.parseDouble(s.trim());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PayrollGUI::new);
    }
}