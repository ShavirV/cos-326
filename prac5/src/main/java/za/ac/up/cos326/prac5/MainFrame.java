package za.ac.up.cos326.prac5;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Swing user interface (Task 4.2). Contains
 * <ul>
 *   <li>a connection panel (URI, user, password, database) with a Connect/Disconnect button and a status label,</li>
 *   <li>an investigation panel: analysis selector, two account-name fields and a Run button,</li>
 *   <li>a results table plus a message line for summaries and (red) error messages.</li>
 * </ul>
 * Database work runs on a {@link SwingWorker} thread so the window never freezes.
 */
public class MainFrame extends JFrame {

    private static final Color OK_COLOUR = new Color(0x1B, 0x7F, 0x3B);
    private static final Color ERROR_COLOUR = new Color(0xB0, 0x1E, 0x1E);

    /** The analyses offered to the investigator and how many account names each one needs. */
    private enum Analysis {
        OVERVIEW("Network overview (people and FOLLOWS)", 0),
        INFLUENTIAL("Influential accounts (top 5 by followers)", 0),
        CONNECTION("Connection investigation (shortest path A -> B)", 2),
        CONTENT("Content investigation (posts of account A)", 1),
        FOLLOWERS("Followers of account A", 1),
        DIRECT("Direct connections of account A (in / out)", 1),
        REACHABLE("Accounts reachable from A in 1-2 hops", 1),
        ACTIVE("Most active followers (top 5 by accounts followed)", 0);

        final String label;
        final int accountsNeeded;

        Analysis(String label, int accountsNeeded) {
            this.label = label;
            this.accountsNeeded = accountsNeeded;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Neo4jService service;

    private final JTextField uriField = new JTextField(22);
    private final JTextField userField = new JTextField(10);
    private final JPasswordField passwordField = new JPasswordField(10);
    private final JTextField databaseField = new JTextField(12);
    private final JButton connectButton = new JButton("Connect");
    private final JLabel statusLabel = new JLabel("Not connected");

    private final JComboBox<Analysis> analysisBox = new JComboBox<>(Analysis.values());
    private final JTextField accountAField = new JTextField(20);
    private final JTextField accountBField = new JTextField(20);
    private final JLabel accountALabel = new JLabel("Account A:");
    private final JLabel accountBLabel = new JLabel("Account B:");
    private final JButton runButton = new JButton("Run analysis");

    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);
    private final JLabel messageLabel = new JLabel(" ");

    public MainFrame(Neo4jService service) {
        super("COS326 Practical 5 - Social Network Investigation");
        this.service = service;
        loadDefaults();
        buildLayout();
        wireEvents();
        updateControls();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        setMinimumSize(new Dimension(900, 560));
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ layout

    private void buildLayout() {
        // --- connection panel
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        connection.setBorder(BorderFactory.createTitledBorder("1. Database connection"));
        connection.add(new JLabel("URI:"));
        connection.add(uriField);
        connection.add(new JLabel("User:"));
        connection.add(userField);
        connection.add(new JLabel("Password:"));
        connection.add(passwordField);
        connection.add(new JLabel("Database:"));
        connection.add(databaseField);
        connection.add(connectButton);
        connection.add(Box.createHorizontalStrut(8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        connection.add(statusLabel);

        // --- investigation panel
        JPanel investigate = new JPanel(new GridBagLayout());
        investigate.setBorder(BorderFactory.createTitledBorder("2. Investigation"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        investigate.add(new JLabel("Analysis:"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        investigate.add(analysisBox, c);

        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        investigate.add(accountALabel, c);
        c.gridx = 1;
        investigate.add(accountAField, c);
        c.gridx = 2;
        investigate.add(accountBLabel, c);
        c.gridx = 3;
        investigate.add(accountBField, c);
        c.gridx = 4;
        investigate.add(runButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 5;
        JLabel hint = new JLabel("Account names look like @mileycyrus (the leading @ may be omitted). "
                + "Press Enter in a name field to run.");
        hint.setForeground(Color.GRAY);
        investigate.add(hint, c);

        JPanel top = new JPanel(new BorderLayout());
        top.add(connection, BorderLayout.NORTH);
        top.add(investigate, BorderLayout.CENTER);

        // --- results
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(resultTable);
        scroll.setBorder(BorderFactory.createTitledBorder("3. Results"));
        scroll.setPreferredSize(new Dimension(900, 300));

        messageLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(messageLabel, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        connectButton.addActionListener(e -> {
            if (service.isConnected()) {
                disconnect();
            } else {
                connect();
            }
        });
        runButton.addActionListener(e -> runSelectedAnalysis());
        accountAField.addActionListener(e -> runSelectedAnalysis());
        accountBField.addActionListener(e -> runSelectedAnalysis());
        analysisBox.addActionListener(e -> updateControls());
    }

    /** Enables / disables controls according to the connection state and the selected analysis. */
    private void updateControls() {
        boolean connected = service.isConnected();
        Analysis a = (Analysis) analysisBox.getSelectedItem();
        int needed = a == null ? 0 : a.accountsNeeded;

        connectButton.setText(connected ? "Disconnect" : "Connect");
        for (var f : List.of(uriField, userField, passwordField, databaseField)) {
            f.setEnabled(!connected);
        }
        analysisBox.setEnabled(connected);
        runButton.setEnabled(connected);
        accountAField.setEnabled(connected && needed >= 1);
        accountALabel.setEnabled(connected && needed >= 1);
        accountBField.setEnabled(connected && needed >= 2);
        accountBLabel.setEnabled(connected && needed >= 2);
        accountALabel.setText(needed == 2 ? "From (A):" : "Account A:");
        accountBLabel.setText(needed == 2 ? "To (B):" : "Account B:");
    }

    // ------------------------------------------------------------------ 4.1 connect / disconnect

    private void connect() {
        String uri = uriField.getText().trim();
        String user = userField.getText().trim();
        String password = new String(passwordField.getPassword());
        String database = databaseField.getText().trim();
        if (uri.isEmpty() || user.isEmpty() || database.isEmpty()) {
            showError("Please fill in the URI, user and database fields.");
            return;
        }
        setBusy(true, "Connecting...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                service.connect(uri, user, password, database);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    get();
                    statusLabel.setText("Connected to '" + service.getDatabase() + "'");
                    statusLabel.setForeground(OK_COLOUR);
                    showInfo("Connection successful: " + uri + " (database " + service.getDatabase() + ").");
                } catch (Exception ex) {
                    statusLabel.setText("Connection failed");
                    statusLabel.setForeground(ERROR_COLOUR);
                    showError("Connection failed: " + Neo4jService.describe(unwrap(ex)));
                }
                updateControls();
            }
        }.execute();
    }

    private void disconnect() {
        service.close();
        statusLabel.setText("Not connected");
        statusLabel.setForeground(Color.DARK_GRAY);
        tableModel.setDataVector(new Object[0][0], new Object[0]);
        showInfo("Disconnected.");
        updateControls();
    }

    // ------------------------------------------------------------------ 4.3 run an analysis

    private void runSelectedAnalysis() {
        if (!service.isConnected()) {
            showError("Connect to the database first.");
            return;
        }
        Analysis analysis = (Analysis) analysisBox.getSelectedItem();
        if (analysis == null) {
            return;
        }
        String a = accountAField.getText();
        String b = accountBField.getText();

        // basic input validation before anything is sent to the database
        try {
            if (analysis.accountsNeeded >= 1) {
                Neo4jService.validateName(a, analysis.accountsNeeded == 2 ? "first (From)" : "account");
            }
            if (analysis.accountsNeeded >= 2) {
                Neo4jService.validateName(b, "second (To)");
            }
        } catch (InvalidAccountException ex) {
            showError(ex.getMessage());
            return;
        }

        Supplier<QueryResult> job = switch (analysis) {
            case OVERVIEW -> service::networkOverview;
            case INFLUENTIAL -> service::influentialAccounts;
            case CONNECTION -> () -> service.shortestPath(a, b);
            case CONTENT -> () -> service.postsOfAccount(a);
            case FOLLOWERS -> () -> service.followersOf(a);
            case DIRECT -> () -> service.directConnections(a);
            case REACHABLE -> () -> service.reachableWithinTwoHops(a);
            case ACTIVE -> service::mostActiveFollowers;
        };

        setBusy(true, "Running: " + analysis.label + " ...");
        new SwingWorker<QueryResult, Void>() {
            @Override
            protected QueryResult doInBackground() {
                return job.get();
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    show(get());
                } catch (Exception ex) {
                    // invalid account, lost connection, Cypher error ... -> message, application keeps running
                    tableModel.setDataVector(new Object[0][0], new Object[0]);
                    showError(Neo4jService.describe(unwrap(ex)));
                    if (!service.isConnected()) {
                        updateControls();
                    }
                }
            }
        }.execute();
    }

    private void show(QueryResult result) {
        tableModel.setDataVector(new Object[0][0], result.columns().toArray());
        for (List<Object> row : result.rows()) {
            tableModel.addRow(row.stream().map(MainFrame::format).toArray());
        }
        if (result.isEmpty() && (result.message() == null || result.message().isBlank())) {
            showInfo("The query returned no rows.");
        } else {
            showInfo(result.message());
        }
    }

    private static Object format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        return value;   // Long / Boolean / String keep natural sorting in the table
    }

    // ------------------------------------------------------------------ helpers

    private void setBusy(boolean busy, String message) {
        setCursor(busy ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
        connectButton.setEnabled(!busy);
        runButton.setEnabled(!busy && service.isConnected());
        if (message != null) {
            showInfo(message);
        }
    }

    private void showInfo(String text) {
        messageLabel.setForeground(OK_COLOUR);
        messageLabel.setText(text == null || text.isBlank() ? " " : text);
    }

    private void showError(String text) {
        messageLabel.setForeground(ERROR_COLOUR);
        messageLabel.setText(text);
    }

    private static Throwable unwrap(Exception ex) {
        return ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
    }

    /** Closes the driver (and its sessions) before the window disappears. */
    private void shutdown() {
        service.close();
        dispose();
        System.exit(0);
    }

    /** Pre-fills the connection fields from config.properties (working directory) if present. */
    private void loadDefaults() {
        Properties p = new Properties();
        Path file = Path.of("config.properties");
        if (Files.isReadable(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                // use built-in defaults
            }
        }
        uriField.setText(p.getProperty("neo4j.uri", "bolt://localhost:7687"));
        userField.setText(p.getProperty("neo4j.user", "neo4j"));
        passwordField.setText(p.getProperty("neo4j.password", ""));
        databaseField.setText(p.getProperty("neo4j.database", "COS326Prac5"));
    }
}
