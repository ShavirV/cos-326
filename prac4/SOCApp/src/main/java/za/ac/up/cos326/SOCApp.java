package za.ac.up.cos326;

import org.bson.Document;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Question 3.2 - Swing GUI for SOC analysts. */
public class SOCApp extends JFrame {

    private static final String DEFAULT_URI = "mongodb://localhost:27017/?serverSelectionTimeoutMS=3000";

    private final MongoService service = new MongoService();

    private final JTextField uriField = new JTextField(DEFAULT_URI, 30);
    private final JLabel statusLabel = new JLabel("Not connected");

    private final JTextField ipField = new JTextField(14);
    private final DefaultTableModel resultModel = new DefaultTableModel(
            new String[]{"Event ID", "Timestamp", "Source IP", "Destination IP", "Event Type", "Severity", "Description"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTextField eventIdField = new JTextField(10);
    private final JComboBox<String> severityBox = new JComboBox<>(new String[]{"Low", "Medium", "High", "Critical"});

    private final DefaultTableModel summaryModel = new DefaultTableModel(
            new String[]{"Severity", "Number of events"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public SOCApp() {
        super("CyberShield SOC - Security Event Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(buildConnectionPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search Events", buildSearchPanel());
        tabs.addTab("Update Event", buildUpdatePanel());
        tabs.addTab("Security Summary", buildSummaryPanel());
        add(tabs, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { service.close(); }
        });

        connect(); // try on start-up
    }

    // ---------- panels ----------

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder("MongoDB Connection"));
        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> connect());
        p.add(new JLabel("URI:"));
        p.add(uriField);
        p.add(connectBtn);
        p.add(statusLabel);
        return p;
    }

    private JPanel buildSearchPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search Events");
        searchBtn.addActionListener(e -> searchEvents());
        ipField.addActionListener(e -> searchEvents());
        top.add(new JLabel("Source IP:"));
        top.add(ipField);
        top.add(searchBtn);
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(resultModel)), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildUpdatePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton updateBtn = new JButton("Update Severity");
        updateBtn.addActionListener(e -> updateSeverity());
        p.add(new JLabel("Event ID:"));
        p.add(eventIdField);
        p.add(new JLabel("New severity:"));
        p.add(severityBox);
        p.add(updateBtn);
        return p;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JButton btn = new JButton("Security Summary");
        btn.addActionListener(e -> showSummary());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btn);
        JLabel title = new JLabel("SECURITY EVENT SUMMARY", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        p.add(top, BorderLayout.NORTH);
        JPanel centre = new JPanel(new BorderLayout());
        centre.add(title, BorderLayout.NORTH);
        centre.add(new JScrollPane(new JTable(summaryModel)), BorderLayout.CENTER);
        p.add(centre, BorderLayout.CENTER);
        return p;
    }

    // ---------- actions ----------

    private void connect() {
        try {
            service.connect(uriField.getText().trim());
            statusLabel.setText("Connected to cybershield.securityEvents");
            statusLabel.setForeground(new Color(0, 128, 0));
        } catch (Exception ex) {
            statusLabel.setText("Not connected");
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "Could not connect to MongoDB.\nCheck that mongod is running and the URI is correct.\n\n"
                            + ex.getMessage(),
                    "Connection failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean requireConnection() {
        if (!service.isConnected()) {
            JOptionPane.showMessageDialog(this, "Not connected to MongoDB. Click Connect first.",
                    "Not connected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void searchEvents() {
        if (!requireConnection()) return;
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a source IP address.");
            return;
        }
        try {
            List<Document> docs = service.findBySourceIP(ip);
            resultModel.setRowCount(0);
            if (docs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No security events found for source IP " + ip + ".");
                return;
            }
            for (Document d : docs) {
                resultModel.addRow(new Object[]{
                        d.getString("eventId"), d.getString("timestamp"), d.getString("sourceIP"),
                        d.getString("destinationIP"), d.getString("eventType"),
                        d.getString("severity"), d.getString("description")});
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void updateSeverity() {
        if (!requireConnection()) return;
        String id = eventIdField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an event ID.");
            return;
        }
        String sev = (String) severityBox.getSelectedItem();
        try {
            if (service.updateSeverity(id, sev)) {
                JOptionPane.showMessageDialog(this, "Event " + id + " updated to severity " + sev + ".");
            } else {
                JOptionPane.showMessageDialog(this, "Update failed: no event with ID " + id + " exists.",
                        "Not found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showSummary() {
        if (!requireConnection()) return;
        try {
            summaryModel.setRowCount(0);
            for (Document d : service.severitySummary()) {
                summaryModel.addRow(new Object[]{d.get("_id"), d.get("numberOfEvents")});
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SOCApp().setVisible(true));
    }
}
