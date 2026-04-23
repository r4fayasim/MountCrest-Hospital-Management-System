import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class Doctor {
    private int id; 
    private String username, password, specialization;
    private double salary;

    public Doctor(int id, String u, String p, String s, double sal) {
        this.id = id; this.username = u; this.password = p; this.specialization = s; this.salary = sal;
    }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getSpecialization() { return specialization; }
    public double getSalary() { return salary; }
}

public class Main extends JFrame {
    static java.util.List<Doctor> doctors = new ArrayList<>();
    static int currentAdminId = -1;

    // Database Connection
    public Connection getConnection() {
        try {
          return DriverManager.getConnection("jdbc:mysql://localhost:3306/mountcrest_db", "root", "YOUR_PASSWORD_HERE");
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    CardLayout cardLayout = new CardLayout();
    JPanel mainPanel = new JPanel(cardLayout);

    public Main() {
        setTitle("Mountcrest Specialty Hospital - Integrated System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initDoctors(); 

        mainPanel.add(welcomePanel(), "welcome");
        mainPanel.add(patientPanel(), "patient");
        
        add(mainPanel);
        setVisible(true);
    }

    void initDoctors() {
        doctors.clear();
        try (Connection con = getConnection()) {
            if(con == null) return;
            // Joins
            String sql = "SELECT d.doctor_id, d.username, d.password, s.specialization_name, d.salary_per_hour " +
                         "FROM doctors d JOIN specializations s ON d.specialization_id = s.specialization_id " +
                         "WHERE d.status = 'ACTIVE'";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                doctors.add(new Doctor(rs.getInt("doctor_id"), rs.getString("username"), rs.getString("password"), 
                                     rs.getString("specialization_name"), rs.getDouble("salary_per_hour")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // welcome screen
    JPanel welcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Mountcrest Specialty Hospital", SwingConstants.CENTER);
        title.setFont(new Font("Avenir Next", Font.BOLD, 38));
        panel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setBackground(Color.WHITE);
        try {
            ImageIcon icon = new ImageIcon("Logo.jpg"); 
            if(icon.getImageLoadStatus() == MediaTracker.COMPLETE)
                centerPanel.add(new JLabel(new ImageIcon(icon.getImage().getScaledInstance(800, 400, Image.SCALE_SMOOTH))));
            else centerPanel.add(new JLabel("<html><h1>🏥</h1></html>"));
        } catch (Exception e) { }
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 50));
        bottom.setBackground(new Color(255, 102, 0));

        JButton doctorBtn = new JButton("Doctor Login");
        JButton patientBtn = new JButton("Book Appointment");
        JButton adminBtn = new JButton("Admin Dashboard"); 
        JButton reportsBtn = new JButton("📊 View Reports");

        for (JButton btn : new JButton[] { doctorBtn, patientBtn, adminBtn, reportsBtn }) {
            btn.setPreferredSize(new Dimension(200, 40));
            bottom.add(btn);
        }

        reportsBtn.addActionListener(e -> showReportsDialog());

        doctorBtn.addActionListener(e -> {
            JFrame frame = new JFrame("Doctor Login");
            frame.setContentPane(doctorLoginPanel());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
            SwingUtilities.getWindowAncestor(doctorBtn).dispose();
        });

        patientBtn.addActionListener(e -> cardLayout.show(mainPanel, "patient"));

        adminBtn.addActionListener(e -> {
            JFrame frame = new JFrame("Admin Login");
            frame.setContentPane(adminLoginPanel()); 
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
            SwingUtilities.getWindowAncestor(adminBtn).dispose();
        });

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // Reports dialog
    void showReportsDialog() {
        JDialog d = new JDialog(this, "Hospital Reports (SQL)", true);
        d.setSize(400, 400);
        d.setLayout(new GridLayout(5, 1, 10, 10));
        d.setLocationRelativeTo(null);

        JButton btn1 = new JButton("1. Active Doctors");
        JButton btn2 = new JButton("2. Detailed Appointments");
        JButton btn3 = new JButton("3. Cardiology Patients");
        JButton btn4 = new JButton("4. High Earners");

        // Joins
        btn1.addActionListener(e -> displayTable("Active Doctors", 
            "SELECT d.username, s.specialization_name FROM doctors d JOIN specializations s ON d.specialization_id = s.specialization_id WHERE d.status = 'ACTIVE'"));

        // Joins
        btn2.addActionListener(e -> displayTable("Appointment Details", 
            "SELECT p.full_name AS Patient, d.username AS Doctor, s.specialization_name, a.appointment_time FROM appointments a JOIN patients p ON a.patient_id = p.patient_id JOIN doctors d ON a.doctor_id = d.doctor_id JOIN specializations s ON d.specialization_id = s.specialization_id"));

        // Join
        btn3.addActionListener(e -> displayTable("Cardiology Patients", 
            "SELECT p.full_name, a.appointment_time FROM appointments a JOIN patients p ON a.patient_id = p.patient_id JOIN doctors d ON a.doctor_id = d.doctor_id JOIN specializations s ON d.specialization_id = s.specialization_id WHERE s.specialization_name = 'Cardiologist'"));

        // Subquery
        btn4.addActionListener(e -> displayTable("High Earners (> Avg)", 
            "SELECT username, salary_per_hour FROM doctors WHERE salary_per_hour > (SELECT AVG(salary_per_hour) FROM doctors)"));

        d.add(new JLabel("Select a Report to View:", SwingConstants.CENTER));
        d.add(btn1); d.add(btn2); d.add(btn3); d.add(btn4);
        d.setVisible(true);
    }

    void displayTable(String title, String query) {
        JFrame f = new JFrame(title);
        f.setSize(800, 500);
        f.setLocationRelativeTo(null);
        Vector<String> columns = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection con = getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData md = rs.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) columns.add(md.getColumnName(i));
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= count; i++) row.add(rs.getObject(i));
                data.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        f.add(new JScrollPane(new JTable(data, columns)));
        f.setVisible(true);
    }

    // Patient panel
    JPanel patientPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel left = new JPanel(); left.setBackground(new Color(255, 102, 0)); left.setPreferredSize(new Dimension(200, 0));
        left.add(new JLabel("<html><h2 style='color:white'>Book Now</h2></html>"));
        
        JPanel right = new JPanel(new GridBagLayout()); right.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameF = new JTextField(15);
        JTextField ageF = new JTextField(15);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Select", "military", "insurance", "gratis", "regular"});
        JComboBox<String> issueBox = new JComboBox<>(new String[]{"Select", "Heart", "Cancer", "Teeth", "General", "Skin", "Lungs", "Eyes", "Mental"});
        
        gbc.gridx=0; gbc.gridy=0; right.add(new JLabel("Name:"), gbc); gbc.gridx=1; right.add(nameF, gbc);
        gbc.gridx=0; gbc.gridy++; right.add(new JLabel("Age:"), gbc); gbc.gridx=1; right.add(ageF, gbc);
        gbc.gridx=0; gbc.gridy++; right.add(new JLabel("Type:"), gbc); gbc.gridx=1; right.add(typeBox, gbc);
        gbc.gridx=0; gbc.gridy++; right.add(new JLabel("Issue:"), gbc); gbc.gridx=1; right.add(issueBox, gbc);
        
        JButton bookBtn = new JButton("Confirm Booking"); gbc.gridy++; right.add(bookBtn, gbc);
        JButton backBtn = new JButton("Back"); gbc.gridx=0; right.add(backBtn, gbc);

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "welcome"));

        bookBtn.addActionListener(e -> {
            String name = nameF.getText(); String type = (String)typeBox.getSelectedItem(); String issue = (String)issueBox.getSelectedItem();
            if(name.isEmpty() || type.equals("Select")) { JOptionPane.showMessageDialog(null, "Fill all fields"); return; }
            
            // Map Issue to Specialization
            String spec = "";
            if(issue.equals("Heart")) spec="Cardiologist"; else if(issue.equals("Cancer")) spec="Oncologist";
            else if(issue.equals("Teeth")) spec="Dentist"; else if(issue.equals("General")) spec="General Physician";
            else if(issue.equals("Skin")) spec="Dermatologist"; else if(issue.equals("Lungs")) spec="Pulmonologist";
            else if(issue.equals("Eyes")) spec="Ophthalmologist"; else if(issue.equals("Mental")) spec="Psychologist";

            Doctor selectedDoc = null;
            for(Doctor d : doctors) if(d.getSpecialization().equals(spec)) { selectedDoc = d; break; }
            
            if(selectedDoc == null) { JOptionPane.showMessageDialog(null, "No doctor available for " + spec); return; }

            try(Connection con = getConnection()) {
                // 1. Get Type ID
                int typeId = 4;
                PreparedStatement pstT = con.prepareStatement("SELECT type_id FROM patient_types WHERE type_name=?");
                pstT.setString(1, type); ResultSet rsT = pstT.executeQuery();
                if(rsT.next()) typeId = rsT.getInt(1);

                // Trigger patient check
                PreparedStatement pstP = con.prepareStatement("INSERT INTO patients (full_name, age, type_id) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
                pstP.setString(1, name); pstP.setInt(2, Integer.parseInt(ageF.getText())); pstP.setInt(3, typeId);
                pstP.executeUpdate();
                ResultSet rsK = pstP.getGeneratedKeys(); rsK.next(); int pid = rsK.getInt(1);

                // Generating random time to fix doctor book issue
                int rHour = 9 + new Random().nextInt(8); 
                int rMin = new Random().nextInt(4) * 15; 
                String date = LocalDateTime.now().plusDays(1).withHour(rHour).withMinute(rMin).withSecond(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // Trigger doctor appointment validation
                PreparedStatement pstA = con.prepareStatement("INSERT INTO appointments (patient_id, doctor_id, appointment_time, issue) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                pstA.setInt(1, pid); pstA.setInt(2, selectedDoc.getId()); pstA.setString(3, date); pstA.setString(4, issue);
                pstA.executeUpdate();

                // Final bill
                ResultSet rsA = pstA.getGeneratedKeys(); rsA.next();
                PreparedStatement pstB = con.prepareStatement("SELECT final_bill FROM appointments WHERE appointment_id=?");
                pstB.setInt(1, rsA.getInt(1)); ResultSet rsB = pstB.executeQuery(); rsB.next();
                
                // Receipt
                String receipt = "✅ APPOINTMENT CONFIRMED!\n\n" +
                                 "Patient: " + name + "\n" +
                                 "Doctor:  " + selectedDoc.getUsername() + " (" + spec + ")\n" +
                                 "Time:    " + date + "\n" +
                                 "Issue:   " + issue + "\n" +
                                 "--------------------------\n" +
                                 "TOTAL BILL: $" + rsB.getDouble(1);

                JOptionPane.showMessageDialog(null, receipt);

            } catch(SQLException ex) { JOptionPane.showMessageDialog(null, "Error/Trigger Blocked: " + ex.getMessage()); }
        });

        main.add(left, BorderLayout.WEST); main.add(right, BorderLayout.CENTER);
        return main;
    }

    // Admin Panel
    JPanel adminLoginPanel() {
        JPanel main = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10);
        
        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        
        main.add(new JLabel("Admin User:"), gbc); gbc.gridx = 1; main.add(userField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; main.add(new JLabel("Password:"), gbc); gbc.gridx = 1; main.add(passField, gbc);
        
        JButton loginBtn = new JButton("Login"); gbc.gridy = 2; main.add(loginBtn, gbc);
        JButton backBtn = new JButton("Back"); gbc.gridy = 3; main.add(backBtn, gbc);

        backBtn.addActionListener(e -> { new Main(); SwingUtilities.getWindowAncestor(backBtn).dispose(); });

        loginBtn.addActionListener(e -> {
            try (Connection con = getConnection()) {
                PreparedStatement pst = con.prepareStatement("SELECT admin_id FROM admin WHERE username=? AND password=?");
                pst.setString(1, userField.getText()); pst.setString(2, new String(passField.getPassword()));
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    currentAdminId = rs.getInt("admin_id"); 
                    JFrame top = (JFrame) SwingUtilities.getWindowAncestor(loginBtn);
                    top.setContentPane(adminDashboardPanel());
                    top.revalidate();
                } else JOptionPane.showMessageDialog(null, "Invalid Credentials");
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        return main;
    }

    JPanel adminDashboardPanel() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel top = new JPanel(); top.setBackground(Color.DARK_GRAY);
        JLabel title = new JLabel("Admin Dashboard"); title.setForeground(Color.WHITE);
        top.add(title); main.add(top, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Doc ID", "Name", "Status", "Appt ID", "Patient", "Time"}, 0);
        JTable table = new JTable(model);
        refreshAdminData(model);

        JPanel bottom = new JPanel(new GridLayout(2, 4, 5, 5));
        
        JTextField idField = new JTextField(); idField.setBorder(BorderFactory.createTitledBorder("Target ID"));
        JButton addDocBtn = new JButton("Add Doctor");
        JButton fireBtn = new JButton("Fire Doctor");
        JButton hireBtn = new JButton("Re-Hire Doctor");
        JButton cancelBtn = new JButton("Cancel Appt");
        JButton refreshBtn = new JButton("Refresh");
        JButton backBtn = new JButton("Logout");

        bottom.add(addDocBtn); bottom.add(idField); bottom.add(fireBtn); bottom.add(hireBtn);
        bottom.add(cancelBtn); bottom.add(refreshBtn); bottom.add(backBtn);

        main.add(new JScrollPane(table), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        backBtn.addActionListener(e -> { new Main(); SwingUtilities.getWindowAncestor(backBtn).dispose(); });
        refreshBtn.addActionListener(e -> refreshAdminData(model));

        fireBtn.addActionListener(e -> callProcedure("fire_doctor", idField.getText(), model));
        hireBtn.addActionListener(e -> callProcedure("rehire_doctor", idField.getText(), model));
        addDocBtn.addActionListener(e -> showAddDoctorDialog());

        // trigger after appointment delete
        cancelBtn.addActionListener(e -> {
            String id = idField.getText();
            if(id.isEmpty()) return;
            try(Connection con = getConnection()) {
                PreparedStatement pst = con.prepareStatement("DELETE FROM appointments WHERE appointment_id = ?");
                pst.setInt(1, Integer.parseInt(id));
                int rows = pst.executeUpdate();
                if(rows > 0) JOptionPane.showMessageDialog(null, "Appointment Cancelled.\n(Log Trigger executed)");
                else JOptionPane.showMessageDialog(null, "ID not found.");
                refreshAdminData(model);
            } catch(Exception ex) { JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage()); }
        });

        return main;
    }

    void callProcedure(String procName, String idStr, DefaultTableModel model) {
        if(idStr.isEmpty()) return;
        try(Connection con = getConnection()) {
            // Procedure fire doctor
            CallableStatement cstmt = con.prepareCall("{call " + procName + "(?, ?)}");
            cstmt.setInt(1, currentAdminId);
            cstmt.setInt(2, Integer.parseInt(idStr));
            cstmt.execute();
            JOptionPane.showMessageDialog(null, "Success!");
            refreshAdminData(model);
            initDoctors(); 
        } catch(Exception ex) { JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage()); }
    }

    void showAddDoctorDialog() {
        JDialog d = new JDialog((Frame)null, "Add Doctor", true);
        d.setSize(300, 300); d.setLayout(new GridLayout(6, 2)); d.setLocationRelativeTo(null);
        
        JTextField uF = new JTextField(); JPasswordField pF = new JPasswordField();
        JTextField sF = new JTextField(); 
        String[] specs = {"1-Cardio", "2-Onco", "3-Dentist", "4-Gen", "5-Derma", "6-Pulmo", "7-Opth", "8-Psych"};
        JComboBox<String> specBox = new JComboBox<>(specs);

        d.add(new JLabel("Username:")); d.add(uF);
        d.add(new JLabel("Password:")); d.add(pF);
        d.add(new JLabel("Spec:")); d.add(specBox);
        d.add(new JLabel("Salary:")); d.add(sF);
        JButton save = new JButton("Save"); d.add(save);

        save.addActionListener(e -> {
            try(Connection con = getConnection()) {
                // Procedure add doctor
                CallableStatement cstmt = con.prepareCall("{call add_doctor(?, ?, ?, ?)}");
                cstmt.setString(1, uF.getText());
                cstmt.setString(2, new String(pF.getPassword()));
                cstmt.setInt(3, specBox.getSelectedIndex() + 1);
                cstmt.setDouble(4, Double.parseDouble(sF.getText()));
                cstmt.execute();
                JOptionPane.showMessageDialog(null, "Doctor Added!");
                initDoctors(); d.dispose();
            } catch(Exception ex) { JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage()); }
        });
        d.setVisible(true);
    }

    void refreshAdminData(DefaultTableModel model) {
        model.setRowCount(0);
        try(Connection con = getConnection(); Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT doctor_id, username, status FROM doctors");
            while(rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), "-", "-", "-"});
            
            // Joins (patients with appointment to show appoijntment in dashboard)
            rs = stmt.executeQuery("SELECT a.appointment_id, p.full_name, a.appointment_time FROM appointments a JOIN patients p ON a.patient_id = p.patient_id");
            while(rs.next()) model.addRow(new Object[]{"-", "-", "-", rs.getInt(1), rs.getString(2), rs.getString(3)});
        } catch(Exception e) {}
    }

    // Doctor Panel
    JPanel doctorLoginPanel() {
        JPanel main = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10,10,10,10);
        
        JTextField uF = new JTextField(15); JPasswordField pF = new JPasswordField(15);
        main.add(new JLabel("User:"), gbc); gbc.gridx=1; main.add(uF, gbc);
        gbc.gridx=0; gbc.gridy=1; main.add(new JLabel("Pass:"), gbc); gbc.gridx=1; main.add(pF, gbc);
        
        JButton login = new JButton("Login"); gbc.gridy=2; main.add(login, gbc);
        JButton back = new JButton("Back"); gbc.gridy=3; main.add(back, gbc);
        JTextArea area = new JTextArea(10, 30); gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; main.add(new JScrollPane(area), gbc);

        back.addActionListener(e -> { new Main(); SwingUtilities.getWindowAncestor(back).dispose(); });
        
        login.addActionListener(e -> {
            String u = uF.getText(), p = new String(pF.getPassword());
            Doctor doc = null;
            for(Doctor d : doctors) if(d.getUsername().equalsIgnoreCase(u) && d.getPassword().equals(p)) doc = d;
            
            if(doc != null) {
                area.setText("Welcome " + doc.getUsername() + "\nLoading Schedule...\n");
                try(Connection con = getConnection()) {
                    // Procedure doctor schedule
                    CallableStatement cs = con.prepareCall("{call doctor_schedule(?)}");
                    cs.setInt(1, doc.getId());
                    ResultSet rs = cs.executeQuery();
                    while(rs.next()) area.append("• " + rs.getString("patient_name") + " @ " + rs.getString("appointment_time") + "\n");
                } catch(Exception ex) { area.setText("Error: " + ex.getMessage()); }
            } else area.setText("Invalid Login");
        });
        return main;
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Main()); }
}