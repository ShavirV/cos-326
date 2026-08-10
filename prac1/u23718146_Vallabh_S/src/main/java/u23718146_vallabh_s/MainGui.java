package u23718146_vallabh_s;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 *
 * @author Shavir
 */

/**
 * all screens form part of one window
 * switch via tabs
*/
public class MainGui extends JFrame {

    private final BookingService service = new BookingService();

    public MainGui() {
        super("Shav prac 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Researchers", buildResearcherPanel());
        tabs.addTab("Equipment", buildEquipmentPanel());
        tabs.addTab("Bookings", buildBookingPanel());
        tabs.addTab("Summary & Reports", buildSummaryPanel());

        add(tabs);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                service.close();
            }
        });
    }

    //Researchers tab

    private JPanel buildResearcherPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField nameField = new JTextField();
        JTextField deptField = new JTextField();
        JTextField emailField = new JTextField();
        JButton registerBtn = new JButton("Register Researcher");

        form.add(new JLabel("Full Name:"));
        form.add(nameField);
        form.add(new JLabel("Department:"));
        form.add(deptField);
        form.add(new JLabel("Email Address:"));
        form.add(emailField);
        form.add(new JLabel());
        form.add(registerBtn);

        JPanel searchForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchIdField = new JTextField(10);
        JButton searchBtn = new JButton("Search by Researcher ID");
        searchForm.add(new JLabel("Researcher ID:"));
        searchForm.add(searchIdField);
        searchForm.add(searchBtn);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(form);
        top.add(searchForm);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        registerBtn.addActionListener(e -> {
            try {
                if (nameField.getText().trim().isEmpty()
                        || deptField.getText().trim().isEmpty()
                        || emailField.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("All fields are required.");
                }
                Researcher r = service.registerResearcher(
                        nameField.getText().trim(),
                        deptField.getText().trim(),
                        emailField.getText().trim());
                JOptionPane.showMessageDialog(this,
                        "Researcher registered with ID: " + r.getId());
                nameField.setText("");
                deptField.setText("");
                emailField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchBtn.addActionListener(e -> {
            try {
                Long id = Long.parseLong(searchIdField.getText().trim());
                Researcher r = service.findResearcher(id);
                if (r == null) {
                    output.setText("No researcher found with ID " + id);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Researcher ID: ").append(r.getId()).append("\n");
                sb.append("Department: ").append(r.getDepartment()).append("\n");
                sb.append("Email: ").append(r.getEmail()).append("\n");
                sb.append("\nBookings:\n");
                List<Booking> bookings = service.getBookingsForResearcher(id);
                if (bookings.isEmpty()) {
                    sb.append("  (no bookings)\n");
                } else {
                    for (Booking b : bookings) {
                        sb.append("  Booking ").append(b.getBookingId())
                          .append(" | ").append(b.getDate())
                          .append(" ").append(b.getStartTime()).append("-").append(b.getEndTime())
                          .append(" | Status: ").append(b.getStatus())
                          .append(" | Equipment: ").append(b.getEquipment().getName())
                          .append("\n");
                    }
                }
                output.setText(sb.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Researcher ID must be a number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    //equipment tab

    private JPanel buildEquipmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(6, 2, 5, 5));
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField dateField = new JTextField("yyyy-MM-dd");
        JTextField costField = new JTextField();
        JComboBox<Equipment.Status> statusBox = new JComboBox<>(Equipment.Status.values());
        JButton registerBtn = new JButton("Register Equipment");

        form.add(new JLabel("Equipment Name:"));
        form.add(nameField);
        form.add(new JLabel("Category:"));
        form.add(categoryField);
        form.add(new JLabel("Purchase Date (yyyy-MM-dd):"));
        form.add(dateField);
        form.add(new JLabel("Replacement Cost:"));
        form.add(costField);
        form.add(new JLabel("Status:"));
        form.add(statusBox);
        form.add(new JLabel());
        form.add(registerBtn);

        JPanel searchForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchIdField = new JTextField(10);
        JButton searchBtn = new JButton("Search by Equipment ID");
        JButton availableBtn = new JButton("Show Available Equipment");
        JButton neverBookedBtn = new JButton("Show Never-Booked Equipment");
        searchForm.add(new JLabel("Equipment ID:"));
        searchForm.add(searchIdField);
        searchForm.add(searchBtn);
        searchForm.add(availableBtn);
        searchForm.add(neverBookedBtn);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(form);
        top.add(searchForm);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        registerBtn.addActionListener(e -> {
            try {
                if (nameField.getText().trim().isEmpty()
                        || categoryField.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Name and category are required.");
                }
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                Integer cost = Integer.parseInt(costField.getText().trim());
                Equipment.Status status = (Equipment.Status) statusBox.getSelectedItem();

                Equipment eq = service.registerEquipment(
                        nameField.getText().trim(),
                        categoryField.getText().trim(),
                        date, cost, status);

                JOptionPane.showMessageDialog(this,
                        "Equipment registered with ID: " + eq.getId());
                nameField.setText("");
                categoryField.setText("");
                dateField.setText("yyyy-MM-dd");
                costField.setText("");
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Date must be in yyyy-MM-dd format.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Replacement cost must be an integer",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        searchBtn.addActionListener(e -> {
            try {
                Long id = Long.parseLong(searchIdField.getText().trim());
                Equipment eq = service.findEquipment(id);
                if (eq == null) {
                    output.setText("No equipment found with ID " + id);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Equipment ID: ").append(eq.getId()).append("\n");
                sb.append("Name: ").append(eq.getName()).append("\n");
                sb.append("Category: ").append(eq.getCategory()).append("\n");
                sb.append("Purchase Date: ").append(eq.getPurchaseDate()).append("\n");
                sb.append("Replacement Cost: ").append(eq.getReplacementCost()).append("\n");
                sb.append("Status: ").append(eq.getStatus()).append("\n");
                sb.append("\nBooking History:\n");
                if (eq.getBookings().isEmpty()) {
                    sb.append("  (no bookings)\n");
                } else {
                    for (Booking b : eq.getBookings()) {
                        sb.append("  Booking ").append(b.getBookingId())
                          .append(" | ").append(b.getDate())
                          .append(" ").append(b.getStartTime()).append("-").append(b.getEndTime())
                          .append(" | Status: ").append(b.getStatus())
                          .append("\n");
                    }
                }
                output.setText(sb.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Equipment ID must be a number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        availableBtn.addActionListener(e -> {
            List<Equipment> list = service.getAvailableEquipment();
            StringBuilder sb = new StringBuilder("Available Equipment:\n\n");
            if (list.isEmpty()) {
                sb.append("  (none available)\n");
            } else {
                for (Equipment eq : list) {
                    sb.append("  ").append(eq.getId()).append(" - ").append(eq.getName())
                      .append(" (").append(eq.getCategory()).append(")\n");
                }
            }
            output.setText(sb.toString());
        });

        neverBookedBtn.addActionListener(e -> {
            List<Equipment> list = service.getNeverBookedEquipment();
            StringBuilder sb = new StringBuilder("Equipment Never Booked:\n\n");
            if (list.isEmpty()) {
                sb.append("  (all equipment has been booked at least once)\n");
            } else {
                for (Equipment eq : list) {
                    sb.append("  ").append(eq.getId()).append(" - ").append(eq.getName()).append("\n");
                }
            }
            output.setText(sb.toString());
        });

        return panel;
    }

    // bookings tab

    private JPanel buildBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(7, 2, 5, 5));
        JTextField researcherIdField = new JTextField();
        JTextField equipmentIdField = new JTextField();
        JTextField dateField = new JTextField("yyyy-MM-dd");
        JTextField startField = new JTextField("HH:mm");
        JTextField endField = new JTextField("HH:mm");
        JTextField purposeField = new JTextField();
        JButton createBtn = new JButton("Create Booking");

        form.add(new JLabel("Researcher ID:"));
        form.add(researcherIdField);
        form.add(new JLabel("Equipment ID:"));
        form.add(equipmentIdField);
        form.add(new JLabel("Booking Date (yyyy-MM-dd):"));
        form.add(dateField);
        form.add(new JLabel("Start Time (HH:mm):"));
        form.add(startField);
        form.add(new JLabel("End Time (HH:mm):"));
        form.add(endField);
        form.add(new JLabel("Research Purpose:"));
        form.add(purposeField);
        form.add(new JLabel());
        form.add(createBtn);

        JPanel manageForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField bookingIdField = new JTextField(8);
        JComboBox<Booking.Status> statusBox = new JComboBox<>(Booking.Status.values());
        JButton updateBtn = new JButton("Update Status");
        JButton cancelBtn = new JButton("Cancel Booking");
        manageForm.add(new JLabel("Booking ID:"));
        manageForm.add(bookingIdField);
        manageForm.add(new JLabel("New Status:"));
        manageForm.add(statusBox);
        manageForm.add(updateBtn);
        manageForm.add(cancelBtn);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(form);
        top.add(manageForm);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        createBtn.addActionListener(e -> {
            try {
                Long researcherId = Long.parseLong(researcherIdField.getText().trim());
                Long equipmentId = Long.parseLong(equipmentIdField.getText().trim());
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                LocalTime start = LocalTime.parse(startField.getText().trim());
                LocalTime end = LocalTime.parse(endField.getText().trim());
                String purpose = purposeField.getText().trim();

                if (purpose.isEmpty()) {
                    throw new IllegalArgumentException("Research purpose is required.");
                }

                Booking b = service.createBooking(date, start, end, purpose, researcherId, equipmentId);

                JOptionPane.showMessageDialog(this,
                        "Booking created with ID: " + b.getBookingId());
                purposeField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Researcher ID and Equipment ID must be numbers.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                        "Check your date (yyyy-MM-dd) and time (HH:mm) formats.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Booking Rejected", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        updateBtn.addActionListener(e -> {
            try {
                Long id = Long.parseLong(bookingIdField.getText().trim());
                Booking.Status newStatus = (Booking.Status) statusBox.getSelectedItem();
                service.updateBookingStatus(id, newStatus);
                JOptionPane.showMessageDialog(this, "Booking " + id + " updated to " + newStatus);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Booking ID must be a number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> {
            try {
                Long id = Long.parseLong(bookingIdField.getText().trim());
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Cancel booking " + id + "? This cannot be undone.",
                        "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    service.cancelBooking(id);
                    JOptionPane.showMessageDialog(this, "Booking " + id + " cancelled.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Booking ID must be a number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Cancellation Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // summary panel

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton totalCostBtn = new JButton("Total Equipment Replacement Cost");
        JButton topResearchersBtn = new JButton("Researcher(s) With Most Bookings");
        buttons.add(totalCostBtn);
        buttons.add(topResearchersBtn);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        totalCostBtn.addActionListener(e -> {
            double total = service.getTotalReplacementCost();
            output.setText(String.format("Total replacement cost of all equipment: %.2f", total));
        });

        topResearchersBtn.addActionListener(e -> {
            List<Researcher> top = service.getTopResearchersByBookingCount();
            StringBuilder sb = new StringBuilder("Researcher(s) with the most bookings:\n\n");
            if (top.isEmpty()) {
                sb.append("  (no bookings exist yet)\n");
            } else {
                for (Researcher r : top) {
                    sb.append("  ").append(r.getId()).append(" - ")
                      .append(r.getDepartment()).append(" (").append(r.getEmail()).append(")\n");
                }
            }
            output.setText(sb.toString());
        });

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainGui gui = new MainGui();
            gui.setVisible(true);
        });
    }
}