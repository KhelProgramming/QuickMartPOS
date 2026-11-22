package com.quickmart.pos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class POSFrame extends JFrame {
    private static final Color NAVY = new Color(10, 103, 124);       // QuickMart teal
    private static final Color NAVY_DARK = new Color(7, 83, 101);
    private static final Color ORANGE = new Color(244, 197, 66);
    private static final Color BG = new Color(239, 243, 244);
    private static final Color INK = new Color(28, 43, 48);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final int TERMINAL_ID = 1;

    private final Auth.User user;
    private Long activeSessionId;
    private BigDecimal openingFloat = BigDecimal.ZERO;
    private final CashierPanel cashierPanel;
    private final ProductsPanel productsPanel;
    private final ReportsPanel reportsPanel;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final java.util.List<JButton> navigationButtons = new java.util.ArrayList<>();

    public POSFrame(Auth.User user) throws SQLException {
        this.user = user;
        setTitle("QuickMart POS | Terminal 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 740));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setupModernDefaults();

        cashierPanel = new CashierPanel();
        productsPanel = new ProductsPanel();
        reportsPanel = new ReportsPanel();

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        pageContainer.setBackground(BG);
        pageContainer.add(cashierPanel, "cashier");
        pageContainer.add(productsPanel, "products");
        pageContainer.add(reportsPanel, "reports");
        pageContainer.add(buildAboutPanel(), "about");
        add(pageContainer, BorderLayout.CENTER);

        applyModernTheme(this);
        styleNavigation("cashier");
        loadOpenSession();
        cashierPanel.setSessionActive(activeSessionId != null);

    }

    private void setupModernDefaults() {
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageAreaBackground", Color.WHITE);
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Table.selectionBackground", new Color(219, 234, 254));
        UIManager.put("Table.selectionForeground", NAVY);
        UIManager.put("Table.gridColor", new Color(235, 239, 244));
    }

    private JPanel buildAboutPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(28, 30, 28, 30));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(25, 28, 25, 28)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("QuickMart POS");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(NAVY);
        JLabel description = new JLabel("A Java and MySQL point-of-sale system for a mini convenience store.");
        description.setForeground(new Color(71, 85, 105));
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JLabel featuresTitle = new JLabel("System features");
        featuresTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        featuresTitle.setForeground(NAVY);
        JLabel features = new JLabel("Inventory management, barcode lookup, webcam scanning, checkout, digital receipts,");
        JLabel features2 = new JLabel("printing, cashier sessions, sales history, and reports.");
        JLabel helpTitle = new JLabel("Quick help");
        helpTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        helpTitle.setForeground(NAVY);
        JLabel help = new JLabel("Open a cashier session before processing sales. Scan a barcode or type it in the barcode field.");
        JLabel help2 = new JLabel("Complete checkout to save the sale and deduct stock.");
        JLabel note = new JLabel("Educational receipt only. This application does not issue tax invoices.");
        note.setForeground(MUTED);

        for (JLabel label : new JLabel[]{description, featuresTitle, features, features2, helpTitle, help, help2, note}) {
            label.setFont(label == featuresTitle || label == helpTitle
                    ? new Font("SansSerif", Font.BOLD, 15)
                    : new Font("SansSerif", Font.PLAIN, 13));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(description);
        card.add(Box.createVerticalStrut(24));
        card.add(featuresTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(features);
        card.add(features2);
        card.add(Box.createVerticalStrut(22));
        card.add(helpTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(help);
        card.add(help2);
        card.add(Box.createVerticalStrut(22));
        card.add(note);
        panel.add(card);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildHeader() {
        // The QuickMart logo and store name appear only once, in the white top-left header.
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(10, 76));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 230, 232)),
                new EmptyBorder(10, 22, 10, 18)));

        JPanel branding = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        branding.setOpaque(false);
        JLabel logo = new JLabel("🛒");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 29));
        logo.setForeground(NAVY);
        JLabel brand = new JLabel("QUICKMART");
        brand.setFont(new Font("SansSerif", Font.BOLD, 21));
        brand.setForeground(NAVY);
        JLabel subtitle = new JLabel("GROCERY POINT OF SALE");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 9));
        subtitle.setForeground(MUTED);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(brand);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(subtitle);
        branding.add(logo);
        branding.add(brandText);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
        right.setOpaque(false);
        JLabel who = new JLabel(user.fullName() + "  |  " + user.role().toUpperCase() + "  |  T1");
        who.setForeground(INK);
        who.setFont(new Font("SansSerif", Font.BOLD, 11));
        right.add(who);

        JButton open = smallButton("Open session");
        JButton close = smallButton("Close session");
        JButton history = smallButton("Session history");
        JButton reconcile = smallButton("Reconcile");
        JButton signOut = smallButton("Sign out");
        open.addActionListener(e -> openSession());
        close.addActionListener(e -> closeSession());
        history.addActionListener(e -> showSessionHistory());
        reconcile.addActionListener(e -> reconcileSession());
        signOut.addActionListener(e -> signOut());
        right.add(open);
        right.add(close);
        right.add(history);
        right.add(reconcile);
        right.add(signOut);

        header.add(branding, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(205, 10));
        sidebar.setBorder(new EmptyBorder(24, 12, 18, 12));

        JLabel menuLabel = new JLabel("MAIN MENU");
        menuLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        menuLabel.setForeground(new Color(205, 229, 233));
        menuLabel.setBorder(new EmptyBorder(0, 10, 14, 0));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuLabel);

        addNavigationButton(sidebar, "▣   Cashier", "cashier");
        addNavigationButton(sidebar, "▤   Products", "products");
        addNavigationButton(sidebar, "▥   Sales & Reports", "reports");

        sidebar.add(Box.createVerticalStrut(18));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(67, 139, 151));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(16));

        JLabel supportLabel = new JLabel("SUPPORT");
        supportLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        supportLabel.setForeground(new Color(205, 229, 233));
        supportLabel.setBorder(new EmptyBorder(0, 10, 14, 0));
        supportLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(supportLabel);
        addNavigationButton(sidebar, "ⓘ   About & Help", "about");
        sidebar.add(Box.createVerticalGlue());

        JLabel terminal = new JLabel("QUICKMART POS  |  TERMINAL 1", SwingConstants.LEFT);
        terminal.setFont(new Font("SansSerif", Font.BOLD, 9));
        terminal.setForeground(new Color(205, 229, 233));
        terminal.setAlignmentX(Component.LEFT_ALIGNMENT);
        terminal.setBorder(new EmptyBorder(0, 10, 0, 0));
        sidebar.add(terminal);
        return sidebar;
    }

    private void addNavigationButton(JPanel sidebar, String text, String page) {
        JButton button = new JButton(text);
        button.putClientProperty("quickmart.navigation", Boolean.TRUE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(180, 48));
        button.setMinimumSize(new Dimension(160, 48));
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(NAVY);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(0, 14, 0, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> {
            pageLayout.show(pageContainer, page);
            styleNavigation(page);
            if ("cashier".equals(page) && cashierPanel != null) {
                cashierPanel.refreshCatalog();
            }
        });
        navigationButtons.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(8));
    }

    private void styleNavigation(String activePage) {
        String[] pages = {"cashier", "products", "reports", "about"};
        for (int i = 0; i < navigationButtons.size(); i++) {
            boolean active = pages[i].equals(activePage);
            JButton button = navigationButtons.get(i);

            // Unselected menu items stay navy with white text.
            // The selected item becomes orange with dark, readable text.
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setRolloverEnabled(false);
            button.setBackground(active ? ORANGE : NAVY);
            button.setForeground(active ? new Color(35, 54, 58) : Color.WHITE);
            button.setBorder(active
                    ? BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ORANGE, 2),
                            new EmptyBorder(0, 12, 0, 6))
                    : new EmptyBorder(2, 14, 2, 8));
        }
    }

    private void applyModernTheme(Component component) {
        if (component instanceof JButton button && Boolean.TRUE.equals(button.getClientProperty("quickmart.navigation"))) {
            // Navigation colors are assigned by styleNavigation after the theme pass.
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setForeground(Color.WHITE);
        } else if (component instanceof JButton button) {
            String text = button.getText() == null ? "" : button.getText().toLowerCase();
            button.setFont(new Font("SansSerif", Font.BOLD, 11));
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setOpaque(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225)),
                    new EmptyBorder(7, 11, 7, 11)));
            button.setBackground(new Color(248, 250, 252));
            button.setForeground(new Color(51, 65, 85));
            if (text.contains("complete sale")) {
                button.setBackground(ORANGE);
                button.setForeground(new Color(35, 54, 58));
                button.setBorder(new EmptyBorder(10, 17, 10, 17));
                button.setFont(new Font("SansSerif", Font.BOLD, 13));
            } else if (text.contains("add product") || text.contains("apply filter")
                    || text.contains("sign in") || text.contains("log in")) {
                button.setBackground(NAVY);
                button.setForeground(Color.WHITE);
                button.setBorder(new EmptyBorder(9, 15, 9, 15));
            } else if (text.contains("remove") || text.contains("cancel") || text.contains("deactivate")) {
                button.setForeground(new Color(185, 28, 28));
            }
        } else if (component instanceof JTable table) {
            table.setRowHeight(32);
            table.setFont(new Font("SansSerif", Font.PLAIN, 12));
            table.setForeground(new Color(51, 65, 85));
            table.setBackground(Color.WHITE);
            table.setSelectionBackground(new Color(219, 234, 254));
            table.setSelectionForeground(NAVY);
            table.setShowVerticalLines(false);
            table.setGridColor(new Color(235, 239, 244));
            table.setIntercellSpacing(new Dimension(0, 1));
            table.setFillsViewportHeight(true);
            table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
            table.getTableHeader().setBackground(new Color(245, 247, 250));
            table.getTableHeader().setForeground(new Color(71, 85, 105));
            table.getTableHeader().setPreferredSize(new Dimension(0, 36));
            table.getTableHeader().setReorderingAllowed(false);
        } else if (component instanceof JTextField field) {
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            field.setForeground(new Color(30, 41, 59));
            field.setBackground(Color.WHITE);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225)),
                    new EmptyBorder(7, 9, 7, 9)));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
            combo.setBackground(Color.WHITE);
            combo.setForeground(new Color(51, 65, 85));
        } else if (component instanceof JScrollPane scroll) {
            scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
            scroll.getViewport().setBackground(Color.WHITE);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyModernTheme(child);
            }
        }
    }

    private JButton smallButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 10));
        return b;
    }


    private void loadOpenSession() throws SQLException {
        String sql = "SELECT id,opening_float FROM cash_sessions WHERE terminal_id=? AND status='OPEN' ORDER BY id DESC LIMIT 1";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, TERMINAL_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    activeSessionId = rs.getLong("id");
                    openingFloat = rs.getBigDecimal("opening_float");
                }
            }
        }
    }

    private void openSession() {
        if (activeSessionId != null) {
            info("A cashier session is already open.");
            return;
        }
        String value = JOptionPane.showInputDialog(this, "Enter opening cash (PHP):", "0.00");
        if (value == null) return;
        try {
            BigDecimal amount = new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() < 0) throw new NumberFormatException();
            try (Connection c = DB.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO cash_sessions(user_id,terminal_id,opening_float,status) VALUES(?,?,?,'OPEN')",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, user.id());
                ps.setInt(2, TERMINAL_ID);
                ps.setBigDecimal(3, amount);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    activeSessionId = rs.getLong(1);
                }
            }
            openingFloat = amount;
            audit("OPEN_SESSION", "Opened session #" + activeSessionId + " with PHP " + amount);
            cashierPanel.setSessionActive(true);
            info("Session opened successfully.");
        } catch (NumberFormatException ex) {
            error("Enter a valid non-negative amount.");
        } catch (SQLException ex) {
            error("Could not open session: " + ex.getMessage());
        }
    }

    private BigDecimal sessionSalesTotal(long sessionId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM sales WHERE session_id=? AND status='COMPLETED'";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    private void closeSession() {
        if (activeSessionId == null) {
            info("There is no open session.");
            return;
        }
        try {
            BigDecimal expected = openingFloat.add(sessionSalesTotal(activeSessionId)).setScale(2, RoundingMode.HALF_UP);
            String value = JOptionPane.showInputDialog(this,
                    "Expected cash: PHP " + money(expected) + "\nEnter actual cash counted:", money(expected));
            if (value == null) return;
            BigDecimal actual = new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
            if (actual.signum() < 0) throw new NumberFormatException();
            BigDecimal variance = actual.subtract(expected);
            try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "UPDATE cash_sessions SET expected_cash=?,actual_cash=?,variance=?,status='CLOSED',closed_at=NOW() WHERE id=? AND status='OPEN'")) {
                ps.setBigDecimal(1, expected);
                ps.setBigDecimal(2, actual);
                ps.setBigDecimal(3, variance);
                ps.setLong(4, activeSessionId);
                ps.executeUpdate();
            }
            audit("CLOSE_SESSION", "Closed session #" + activeSessionId + "; variance PHP " + variance);
            activeSessionId = null;
            openingFloat = BigDecimal.ZERO;
            cashierPanel.setSessionActive(false);
            JOptionPane.showMessageDialog(this,
                    "Session closed.\nExpected: PHP " + money(expected)
                    + "\nActual: PHP " + money(actual) + "\nVariance: PHP " + money(variance),
                    "Cash reconciliation", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            error("Enter a valid non-negative amount.");
        } catch (SQLException ex) {
            error("Could not close session: " + ex.getMessage());
        }
    }

    private void reconcileSession() {
        if (activeSessionId == null) {
            info("No active session. Use Session History to review closed sessions.");
            return;
        }
        try {
            BigDecimal sales = sessionSalesTotal(activeSessionId);
            BigDecimal expected = openingFloat.add(sales);
            JOptionPane.showMessageDialog(this,
                    "SESSION RECONCILIATION\n\nSession: #" + activeSessionId
                    + "\nOpening cash: PHP " + money(openingFloat)
                    + "\nCompleted sales: PHP " + money(sales)
                    + "\nExpected cash: PHP " + money(expected)
                    + "\n\nUse Close session to enter the actual cash counted.",
                    "Reconcile", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) { error(ex.getMessage()); }
    }

    private void showSessionHistory() {
        String sql = "SELECT cs.id,u.username,t.code,cs.opening_float,cs.expected_cash,cs.actual_cash,cs.variance,cs.status,cs.opened_at,cs.closed_at "
                + "FROM cash_sessions cs JOIN users u ON cs.user_id=u.id JOIN terminals t ON cs.terminal_id=t.id ORDER BY cs.id DESC LIMIT 100";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            DefaultTableModel m = new DefaultTableModel(new Object[]{
                    "Session","Cashier","Terminal","Opening","Expected","Actual","Variance","Status","Opened","Closed"}, 0);
            while (rs.next()) m.addRow(new Object[]{rs.getLong("id"),rs.getString("username"),rs.getString("code"),
                    rs.getBigDecimal("opening_float"),rs.getBigDecimal("expected_cash"),rs.getBigDecimal("actual_cash"),
                    rs.getBigDecimal("variance"),rs.getString("status"),rs.getTimestamp("opened_at"),rs.getTimestamp("closed_at")});
            JTable table = new JTable(m);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            JScrollPane pane = new JScrollPane(table);
            pane.setPreferredSize(new Dimension(1000, 360));
            JOptionPane.showMessageDialog(this, pane, "Cashier Session History", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) { error(ex.getMessage()); }
    }

    private void audit(String action, String details) {
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO audit_logs(user_id,terminal_id,action,details) VALUES(?,?,?,?)")) {
            ps.setInt(1, user.id()); ps.setInt(2, TERMINAL_ID);
            ps.setString(3, action); ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void signOut() {
        if (activeSessionId != null) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "A cashier session is still open. Close it before signing out?",
                    "Open session", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
            closeSession();
            if (activeSessionId != null) return;
        }
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void info(String text) {
        JOptionPane.showMessageDialog(this, text, "QuickMart POS", JOptionPane.INFORMATION_MESSAGE);
    }
    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "QuickMart POS", JOptionPane.ERROR_MESSAGE);
    }
    private static String money(BigDecimal value) {
        return new DecimalFormat("#,##0.00").format(value == null ? BigDecimal.ZERO : value);
    }

    private static String discountDescription(BigDecimal regularPrice, BigDecimal discountPercent) {
        if (discountPercent.signum() == 0) return "";
        return " (" + discountPercent.stripTrailingZeros().toPlainString()
                + "% off PHP " + money(regularPrice) + ")";
    }

    private record Product(int id, String barcode, String name, String category,
                           BigDecimal price, BigDecimal discountPercent, int stock) {
        BigDecimal sellingPrice() {
            return price.multiply(BigDecimal.ONE.subtract(discountPercent.movePointLeft(2)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static Product readProduct(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getString("barcode"), rs.getString("name"),
                rs.getString("category"), rs.getBigDecimal("price"),
                rs.getBigDecimal("discount_percent"), rs.getInt("stock"));
    }

    private class CashierPanel extends JPanel {
        private final JTextField barcodeField = new JTextField(18);
        private final JTextField cashField = new JTextField(10);
        private final JLabel totalLabel = new JLabel("PHP 0.00");
        private final JLabel sessionLabel = new JLabel("Open a cashier session before checkout.");
        private final DefaultTableModel cartModel = new DefaultTableModel(
                new Object[]{"ID","Product","Barcode","Unit price","Qty","Line total","Regular price","Discount %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        private final JTable cartTable = new JTable(cartModel);
        private final JPanel productGrid = new JPanel(new GridLayout(0, 4, 12, 12));
        private final JPanel categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 2));
        private String selectedCategory = "All";

        CashierPanel() {
            setLayout(new BorderLayout(14, 14));
            setBackground(BG);
            setBorder(new EmptyBorder(14, 14, 14, 14));

            JPanel top = new JPanel(new BorderLayout(10, 0));
            top.setOpaque(false);
            JPanel status = new JPanel();
            status.setOpaque(false);
            status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));
            sessionLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            sessionLabel.setForeground(new Color(180, 83, 9));
            JLabel hint = new JLabel("Browse categories or scan a product barcode");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            hint.setForeground(MUTED);
            status.add(sessionLabel);
            status.add(Box.createVerticalStrut(3));
            status.add(hint);
            JButton scannerButton = new JButton("▦  Barcode scanner");
            scannerButton.addActionListener(e -> showScannerDialog());
            top.add(status, BorderLayout.WEST);
            top.add(scannerButton, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            JPanel browse = buildProductBrowser();
            JPanel order = buildCartPanel();
            order.setPreferredSize(new Dimension(390, 10));
            JPanel center = new JPanel(new BorderLayout(14, 0));
            center.setOpaque(false);
            center.add(browse, BorderLayout.CENTER);
            center.add(order, BorderLayout.EAST);
            add(center, BorderLayout.CENTER);
            add(buildCheckoutPanel(), BorderLayout.SOUTH);

            cartTable.setRowHeight(34);
            cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            cartTable.getColumnModel().getColumn(0).setMinWidth(0);
            cartTable.getColumnModel().getColumn(0).setMaxWidth(0);
            cartTable.getColumnModel().getColumn(0).setWidth(0);
            cartTable.removeColumn(cartTable.getColumnModel().getColumn(7));
            cartTable.removeColumn(cartTable.getColumnModel().getColumn(6));
            barcodeField.addActionListener(e -> addBarcode());
            reloadProductCards();
        }

        private JPanel buildProductBrowser() {
            JPanel panel = cardPanel();
            panel.setLayout(new BorderLayout(8, 12));
            JPanel north = new JPanel();
            north.setOpaque(false);
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Categories");
            title.setFont(new Font("SansSerif", Font.BOLD, 17));
            title.setForeground(INK);
            categoryBar.setOpaque(false);
            north.add(title);
            north.add(Box.createVerticalStrut(8));
            north.add(categoryBar);
            JLabel productsTitle = new JLabel("Products");
            productsTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
            productsTitle.setForeground(INK);
            north.add(Box.createVerticalStrut(12));
            north.add(productsTitle);
            panel.add(north, BorderLayout.NORTH);
            productGrid.setOpaque(false);
            productGrid.setBorder(new EmptyBorder(2, 1, 2, 1));
            JScrollPane scroll = new JScrollPane(productGrid);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(Color.WHITE);
            panel.add(scroll, BorderLayout.CENTER);
            rebuildCategories();
            return panel;
        }

        private void rebuildCategories() {
            categoryBar.removeAll();
            java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
            categories.add("All");
            try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT COALESCE(NULLIF(TRIM(category),''),'Uncategorized') AS category FROM products WHERE active=1 ORDER BY category");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) categories.add(rs.getString("category"));
            } catch (SQLException ex) { error("Could not load categories: " + ex.getMessage()); }
            for (String category : categories) {
                JButton button = new JButton(category);
                button.setFont(new Font("SansSerif", Font.BOLD, 11));
                button.setFocusPainted(false);
                button.setBorder(new EmptyBorder(8, 15, 8, 15));
                button.setBackground(category.equals(selectedCategory) ? NAVY : new Color(241, 245, 246));
                button.setForeground(category.equals(selectedCategory) ? Color.WHITE : INK);
                button.addActionListener(e -> {
                    selectedCategory = category;
                    rebuildCategories();
                    reloadProductCards();
                });
                categoryBar.add(button);
            }
            categoryBar.revalidate();
            categoryBar.repaint();
        }

        private void reloadProductCards() {
            productGrid.removeAll();
            String sql = "SELECT id,barcode,name,category,price,discount_percent,stock,photo_jpeg FROM products WHERE active=1 "
                    + ("All".equals(selectedCategory) ? "" : "AND COALESCE(NULLIF(TRIM(category),''),'Uncategorized')=? ")
                    + "ORDER BY name";
            try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                if (!"All".equals(selectedCategory)) ps.setString(1, selectedCategory);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productGrid.add(productCard(readProduct(rs), rs.getBytes("photo_jpeg")));
                    }
                }
            } catch (SQLException ex) { error("Could not load products: " + ex.getMessage()); }
            if (productGrid.getComponentCount() == 0) {
                JLabel empty = new JLabel("No products in this category.");
                empty.setForeground(MUTED);
                productGrid.add(empty);
            }
            productGrid.revalidate();
            productGrid.repaint();
        }

        private JPanel productCard(Product product, byte[] photo) {
            JPanel card = new JPanel(new BorderLayout(5, 7));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 232, 234)), new EmptyBorder(10, 10, 10, 10)));
            ImageIcon thumbnail = ProductPhoto.icon(photo, 84);
            JLabel visual = new JLabel(thumbnail == null ? "▣" : "", thumbnail, SwingConstants.CENTER);
            visual.setFont(new Font("SansSerif", Font.PLAIN, 38));
            visual.setForeground(new Color(125, 160, 166));
            visual.setOpaque(true);
            visual.setBackground(thumbnail == null ? new Color(241, 246, 247) : Color.WHITE);
            visual.setPreferredSize(new Dimension(130, 88));
            JLabel name = new JLabel("<html><b>" + html(product.name()) + "</b></html>");
            name.setForeground(INK);
            JLabel price = new JLabel("PHP " + money(product.sellingPrice()));
            price.setToolTipText("Regular price: PHP " + money(product.price()));
            JLabel discount = new JLabel(product.discountPercent().signum() > 0
                    ? product.discountPercent().stripTrailingZeros().toPlainString() + "% off" : "");
            discount.setForeground(NAVY);
            price.setFont(new Font("SansSerif", Font.BOLD, 13));
            price.setForeground(NAVY);
            JLabel stock = new JLabel(product.stock() > 0 ? "In stock: " + product.stock() : "Out of stock");
            stock.setFont(new Font("SansSerif", Font.PLAIN, 10));
            stock.setForeground(product.stock() > 0 ? new Color(30, 130, 85) : new Color(190, 50, 50));
            JButton add = new JButton("Add to order");
            add.setFont(new Font("SansSerif", Font.BOLD, 10));
            add.setBackground(NAVY);
            add.setForeground(Color.WHITE);
            add.setEnabled(product.stock() > 0);
            add.addActionListener(e -> addProductById(product.id()));
            JPanel details = new JPanel(new GridLayout(0, 1, 2, 2));
            details.setOpaque(false);
            details.add(name); details.add(price); details.add(discount); details.add(stock);
            card.add(visual, BorderLayout.NORTH);
            card.add(details, BorderLayout.CENTER);
            card.add(add, BorderLayout.SOUTH);
            return card;
        }

        private String html(String value) {
            return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private void showScannerDialog() {
            CameraDialog.scan(this, barcode -> addProductByBarcode(barcode, true));
        }

        private JPanel cardPanel() {
            JPanel p = new JPanel();
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240)),
                    new EmptyBorder(16, 16, 16, 16)));
            return p;
        }

        private JPanel buildCartPanel() {
            JPanel panel = cardPanel();
            panel.setLayout(new BorderLayout(8,8));
            JLabel title = new JLabel("ORDER DETAIL");
            title.setFont(new Font("SansSerif", Font.BOLD, 15));
            title.setForeground(NAVY);

            JPanel entry = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
            entry.setOpaque(false);
            entry.add(new JLabel("Barcode:"));
            entry.add(barcodeField);
            JButton add = new JButton("Add barcode");
            JButton find = new JButton("Find product");
            entry.add(add); entry.add(find);

            JPanel north = new JPanel(new BorderLayout(5,8));
            north.setOpaque(false);
            north.add(title, BorderLayout.NORTH);
            north.add(entry, BorderLayout.SOUTH);

            JScrollPane scroll = new JScrollPane(cartTable);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(226,232,240)));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
            actions.setOpaque(false);
            JButton qty = new JButton("Quantity");
            JButton remove = new JButton("Remove item");
            JButton cancel = new JButton("Cancel sale");
            JButton refresh = new JButton("Refresh prices");
            actions.add(qty); actions.add(remove); actions.add(cancel); actions.add(refresh);

            panel.add(north, BorderLayout.NORTH);
            panel.add(scroll, BorderLayout.CENTER);
            panel.add(actions, BorderLayout.SOUTH);
            add.addActionListener(e -> addBarcode());
            find.addActionListener(e -> findProduct());
            qty.addActionListener(e -> changeQuantity());
            remove.addActionListener(e -> removeSelected());
            cancel.addActionListener(e -> clearCart());
            refresh.addActionListener(e -> refreshPrices());
            return panel;
        }

        private JPanel buildCheckoutPanel() {
            JPanel panel = new JPanel(new BorderLayout(12,0));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240)),
                    new EmptyBorder(16, 19, 16, 19)));
            totalLabel.setFont(new Font("SansSerif",Font.BOLD,30));
            totalLabel.setForeground(NAVY);
            JPanel left = new JPanel(new GridLayout(2,1));
            left.setOpaque(false);
            left.add(new JLabel("TOTAL"));
            left.add(totalLabel);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
            right.setOpaque(false);
            right.add(new JLabel("Cash received (PHP):"));
            right.add(cashField);
            JButton complete = new JButton("Complete sale");
            complete.setBackground(ORANGE);
            complete.setForeground(new Color(35, 54, 58));
            complete.setFont(new Font("SansSerif",Font.BOLD,14));
            complete.setBorder(new EmptyBorder(12, 24, 12, 24));
            complete.addActionListener(e -> completeSale());
            right.add(complete);
            panel.add(left,BorderLayout.WEST);
            panel.add(right,BorderLayout.EAST);
            return panel;
        }

        void refreshCatalog() {
            rebuildCategories();
            reloadProductCards();
        }

        void setSessionActive(boolean active) {
            sessionLabel.setText(active ? "Cashier session is OPEN" : "Open a cashier session before checkout.");
            sessionLabel.setForeground(active ? new Color(22,128,74) : new Color(180,83,9));
        }

        private Product getProductByBarcode(String barcode) throws SQLException {
            String sql = "SELECT id,barcode,name,category,price,discount_percent,stock FROM products WHERE barcode=? AND active=1";

            try (Connection c = DB.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, barcode);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;

                    return readProduct(rs);
                }
            }
        }

        private void addBarcode() {
            String barcode = barcodeField.getText().trim();
            if (barcode.isEmpty()) return;
            addProductByBarcode(barcode, false);
        }

        private String addProductByBarcode(String barcode, boolean fromCamera) {
            String failure;
            try {
                Product product = getProductByBarcode(barcode);
                if (product == null && barcode.matches("[0-9]{12}")) {
                    product = getProductByBarcode("0" + barcode);
                }
                if (product == null && barcode.matches("0[0-9]{12}")) {
                    product = getProductByBarcode(barcode.substring(1));
                }
                failure = product == null ? "Product not found: " + barcode : addProduct(product);
                if (failure == null) {
                    barcodeField.setText("");
                    return "Added " + product.name() + " to the order.";
                }
            } catch (SQLException ex) {
                failure = "Database error: " + ex.getMessage();
            }
            if (!fromCamera) info(failure);
            return failure;
        }

        private String addProduct(Product product) {
            int row = findCartRow(product.id());
            int quantity = row < 0 ? 1 : (int) cartModel.getValueAt(row, 4) + 1;
            if (quantity > product.stock()) {
                return "Not enough stock for " + product.name() + ". Available: " + product.stock();
            }
            setCartItem(row, product, quantity);
            updateTotal();
            return null;
        }

        private void setCartItem(int row, Product product, int quantity) {
            BigDecimal unitPrice = product.sellingPrice();
            Object[] item = {product.id(), product.name(), product.barcode(), unitPrice, quantity,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)), product.price(), product.discountPercent()};
            if (row < 0) {
                cartModel.addRow(item);
            } else {
                for (int column = 0; column < item.length; column++) {
                    cartModel.setValueAt(item[column], row, column);
                }
            }
        }

        private int findCartRow(int productId) {
            for(int i=0;i<cartModel.getRowCount();i++)
                if((int)cartModel.getValueAt(i,0)==productId) return i;
            return -1;
        }

        private void updateTotal() {
            BigDecimal total=BigDecimal.ZERO;
            for(int i=0;i<cartModel.getRowCount();i++)
                total=total.add((BigDecimal)cartModel.getValueAt(i,5));
            totalLabel.setText("PHP "+money(total.setScale(2,RoundingMode.HALF_UP)));
        }

        private BigDecimal cartTotal() {
            BigDecimal total=BigDecimal.ZERO;
            for(int i=0;i<cartModel.getRowCount();i++) total=total.add((BigDecimal)cartModel.getValueAt(i,5));
            return total.setScale(2,RoundingMode.HALF_UP);
        }

        private void findProduct() {
            String term=JOptionPane.showInputDialog(this,"Enter product name or barcode:");
            if(term==null || term.isBlank()) return;
            String sql="SELECT id,barcode,name,category,price,discount_percent,stock FROM products WHERE active=1 AND (name LIKE ? OR barcode LIKE ?) ORDER BY name LIMIT 40";
            try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(sql)) {
                ps.setString(1,"%"+term.trim()+"%"); ps.setString(2,"%"+term.trim()+"%");
                try(ResultSet rs=ps.executeQuery()) {
                    DefaultTableModel m=new DefaultTableModel(new Object[]{"ID","Barcode","Product","Price","Stock"},0);
                    while (rs.next()) {
                        Product product = readProduct(rs);
                        m.addRow(new Object[]{product.id(), product.barcode(), product.name(),
                                product.sellingPrice(), product.stock()});
                    }
                    JTable t=new JTable(m);
                    int answer=JOptionPane.showConfirmDialog(this,new JScrollPane(t),"Select a product",
                            JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
                    if(answer==JOptionPane.OK_OPTION && t.getSelectedRow()>=0) {
                        int id=(int)m.getValueAt(t.convertRowIndexToModel(t.getSelectedRow()),0);
                        addProductById(id);
                    }
                }
            } catch(SQLException ex) { error(ex.getMessage()); }
        }

        private void addProductById(int id) {
            try {
                Product product = getProductById(id);
                if (product == null) {
                    info("This product is no longer available.");
                } else {
                    String failure = addProduct(product);
                    if (failure != null) info(failure);
                }
            } catch (SQLException ex) { error(ex.getMessage()); }
        }

        private Product getProductById(int id) throws SQLException {
            String sql = "SELECT id,barcode,name,category,price,discount_percent,stock FROM products WHERE id=? AND active=1";
            try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? readProduct(rs) : null;
                }
            }
        }

        private void changeQuantity() {
            int viewRow=cartTable.getSelectedRow();
            if(viewRow<0) { info("Select an item first."); return; }
            int row=cartTable.convertRowIndexToModel(viewRow);
            String value=JOptionPane.showInputDialog(this,"Enter quantity:",cartModel.getValueAt(row,4));
            if(value==null) return;
            try {
                int qty=Integer.parseInt(value.trim());
                if(qty<=0) throw new NumberFormatException();
                Product product = getProductById((int) cartModel.getValueAt(row, 0));
                if (product == null || qty > product.stock()) { error("Quantity exceeds available stock."); return; }
                setCartItem(row, product, qty);
                updateTotal();
            } catch(NumberFormatException ex) { error("Enter a whole number greater than zero."); }
            catch(SQLException ex) { error(ex.getMessage()); }
        }

        private void removeSelected() {
            int row=cartTable.getSelectedRow();
            if(row<0) { info("Select an item first."); return; }
            cartModel.removeRow(cartTable.convertRowIndexToModel(row));
            updateTotal();
        }

        private void clearCart() {
            if(cartModel.getRowCount()==0) return;
            if(JOptionPane.showConfirmDialog(this,"Clear all items in the current sale?",
                    "Cancel sale",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                cartModel.setRowCount(0); cashField.setText(""); updateTotal();
            }
        }

        private void refreshPrices() {
            for(int i=0;i<cartModel.getRowCount();i++) {
                try {
                    Product product = getProductById((int) cartModel.getValueAt(i, 0));
                    if (product != null) {
                        setCartItem(i, product, (int) cartModel.getValueAt(i, 4));
                    }
                } catch(SQLException ex) { error(ex.getMessage()); }
            }
            updateTotal();
        }

        private void completeSale() {
            if (activeSessionId == null) { error("Open a cashier session first."); return; }
            if (cartModel.getRowCount() == 0) { error("The cart is empty."); return; }

            BigDecimal total = cartTotal();
            BigDecimal tender;
            try {
                tender = new BigDecimal(cashField.getText().trim()).setScale(2, RoundingMode.HALF_UP);
                if (tender.compareTo(total) < 0) { error("Cash received is less than the total."); return; }
            } catch (Exception ex) {
                error("Enter a valid cash received amount.");
                return;
            }

            BigDecimal change = tender.subtract(total).setScale(2, RoundingMode.HALF_UP);
            String receiptNo = "QM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + "-" + (System.currentTimeMillis() % 10000);
            StringBuilder receipt = new StringBuilder();
            receipt.append("QUICKMART GROCERY\n");
            receipt.append("Receipt: ").append(receiptNo).append("\n");
            receipt.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            receipt.append("Cashier: ").append(user.fullName()).append("\n");
            receipt.append("Terminal: T1 | Session: ").append(activeSessionId).append("\n");
            receipt.append("----------------------------------------\n");

            try (Connection c = DB.getConnection()) {
                c.setAutoCommit(false);
                try {
                    long saleId;
                    String saleSql = "INSERT INTO sales(receipt_no,request_id,session_id,user_id,terminal_id,total,tender,change_amount,status) "
                            + "VALUES(?,?,?,?,?,?,?,?,'COMPLETED')";
                    try (PreparedStatement ps = c.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, receiptNo);
                        ps.setString(2, UUID.randomUUID().toString());
                        ps.setLong(3, activeSessionId);
                        ps.setInt(4, user.id());
                        ps.setInt(5, TERMINAL_ID);
                        ps.setBigDecimal(6, total);
                        ps.setBigDecimal(7, tender);
                        ps.setBigDecimal(8, change);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            rs.next();
                            saleId = rs.getLong(1);
                        }
                    }

                    for (int i = 0; i < cartModel.getRowCount(); i++) {
                        int productId = (int) cartModel.getValueAt(i, 0);
                        String productName = (String) cartModel.getValueAt(i, 1);
                        String barcode = (String) cartModel.getValueAt(i, 2);
                        BigDecimal price = (BigDecimal) cartModel.getValueAt(i, 3);
                        int quantity = (int) cartModel.getValueAt(i, 4);
                        BigDecimal lineTotal = (BigDecimal) cartModel.getValueAt(i, 5);
                        BigDecimal regularPrice = (BigDecimal) cartModel.getValueAt(i, 6);
                        BigDecimal discountPercent = (BigDecimal) cartModel.getValueAt(i, 7);

                        try (PreparedStatement ps = c.prepareStatement(
                                "UPDATE products SET stock=stock-?,version=version+1 WHERE id=? AND active=1 AND stock>=?")) {
                            ps.setInt(1, quantity);
                            ps.setInt(2, productId);
                            ps.setInt(3, quantity);
                            if (ps.executeUpdate() != 1)
                                throw new SQLException("Insufficient stock for " + productName + ". Refresh the cart.");
                        }

                        try (PreparedStatement ps = c.prepareStatement(
                                "INSERT INTO sale_items(sale_id,product_id,product_name_snapshot,barcode_snapshot,"
                                + "unit_price,quantity,line_total,regular_unit_price,discount_percent) VALUES(?,?,?,?,?,?,?,?,?)")) {
                            ps.setLong(1, saleId);
                            ps.setInt(2, productId);
                            ps.setString(3, productName);
                            ps.setString(4, barcode);
                            ps.setBigDecimal(5, price);
                            ps.setInt(6, quantity);
                            ps.setBigDecimal(7, lineTotal);
                            ps.setBigDecimal(8, regularPrice);
                            ps.setBigDecimal(9, discountPercent);
                            ps.executeUpdate();
                        }

                        try (PreparedStatement ps = c.prepareStatement(
                                "INSERT INTO stock_movements(product_id,user_id,sale_id,movement_type,quantity_delta,reason) "
                                + "VALUES(?,?,?,'SALE',?,'POS sale')")) {
                            ps.setInt(1, productId);
                            ps.setInt(2, user.id());
                            ps.setLong(3, saleId);
                            ps.setInt(4, -quantity);
                            ps.executeUpdate();
                        }

                        receipt.append(productName).append("\n")
                                .append("  ").append(quantity).append(" x PHP ").append(money(price))
                                .append(discountDescription(regularPrice, discountPercent))
                                .append(" = PHP ").append(money(lineTotal)).append("\n");
                    }

                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO audit_logs(user_id,terminal_id,action,details) VALUES(?,?,'COMPLETE_SALE',?)")) {
                        ps.setInt(1, user.id());
                        ps.setInt(2, TERMINAL_ID);
                        ps.setString(3, "Sale " + receiptNo + " total PHP " + total);
                        ps.executeUpdate();
                    }
                    c.commit();
                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                } finally {
                    c.setAutoCommit(true);
                }
            } catch (Exception ex) {
                error("Sale was not completed: " + ex.getMessage());
                return;
            }

            receipt.append("----------------------------------------\n");
            receipt.append("TOTAL:  PHP ").append(money(total)).append("\n");
            receipt.append("CASH:   PHP ").append(money(tender)).append("\n");
            receipt.append("CHANGE: PHP ").append(money(change)).append("\n\n");
            receipt.append("Thank you for shopping!\n");
            receipt.append("Educational receipt; not a tax invoice.\n");

            cartModel.setRowCount(0);
            cashField.setText("");
            updateTotal();
            productsPanel.reload();
            reloadProductCards();
            reportsPanel.reload();
            new ReceiptDialog(POSFrame.this, receipt.toString()).setVisible(true);
        }
    }

    private class ProductsPanel extends JPanel {
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID","Barcode","Product name","Category","Regular price","Discount %","Stock","Active"}, 0) {
            @Override public boolean isCellEditable(int r,int c) { return false; }
        };
        private final JTable table = new JTable(model);

        ProductsPanel() {
            setLayout(new BorderLayout(10,10));
            setBackground(BG);
            setBorder(new EmptyBorder(15,15,15,15));
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toolbar.setOpaque(false);
            JButton add = new JButton("Add product");
            JButton edit = new JButton("Update product");
            JButton deactivate = new JButton("Deactivate product");
            JButton stock = new JButton("Adjust stock");
            JButton refresh = new JButton("Refresh");
            toolbar.add(add); toolbar.add(edit); toolbar.add(deactivate); toolbar.add(stock); toolbar.add(refresh);
            add(toolbar,BorderLayout.NORTH);
            table.setRowHeight(28);
            add(new JScrollPane(table),BorderLayout.CENTER);
            add.addActionListener(e -> editProduct(null));
            edit.addActionListener(e -> editSelected());
            deactivate.addActionListener(e -> deactivateSelected());
            stock.addActionListener(e -> adjustStock());
            refresh.addActionListener(e -> reload());
            reload();
        }

        void reload() {
            model.setRowCount(0);
            String sql="SELECT id,barcode,name,category,price,discount_percent,stock,active FROM products ORDER BY name";
            try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
                while(rs.next()) model.addRow(new Object[]{rs.getInt("id"),rs.getString("barcode"),
                        rs.getString("name"),rs.getString("category"),rs.getBigDecimal("price"),
                        rs.getBigDecimal("discount_percent"),rs.getInt("stock"),rs.getBoolean("active")});
            } catch(SQLException ex) { error(ex.getMessage()); }
        }

        private Integer selectedId() {
            int row=table.getSelectedRow();
            return row<0 ? null : (Integer)model.getValueAt(table.convertRowIndexToModel(row),0);
        }

        private void editSelected() {
            Integer id=selectedId();
            if(id==null) { info("Select a product first."); return; }
            editProduct(id);
        }

        private void editProduct(Integer id) {
            JTextField barcode=new JTextField();
            JTextField name=new JTextField();
            JTextField category=new JTextField("General");
            JTextField price=new JTextField("0.00");
            JTextField discount=new JTextField("0.00");
            JTextField stock=new JTextField("0");
            int oldStock=0;
            byte[] existingPhoto = null;

            if(id!=null) {
                String sql="SELECT barcode,name,category,price,discount_percent,stock,photo_jpeg FROM products WHERE id=?";
                try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(sql)) {
                    ps.setInt(1,id);
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next()) return;
                        barcode.setText(rs.getString("barcode"));
                        name.setText(rs.getString("name"));
                        category.setText(rs.getString("category"));
                        price.setText(rs.getBigDecimal("price").toPlainString());
                        discount.setText(rs.getBigDecimal("discount_percent").toPlainString());
                        oldStock=rs.getInt("stock");
                        stock.setText(String.valueOf(oldStock));
                        existingPhoto = rs.getBytes("photo_jpeg");
                    }
                } catch(SQLException ex) { error(ex.getMessage()); return; }
            }

            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Barcode")); form.add(barcode);
            form.add(new JLabel("Product name")); form.add(name);
            form.add(new JLabel("Category")); form.add(category);
            form.add(new JLabel("Regular price (PHP)")); form.add(price);
            form.add(new JLabel("Discount (%)")); form.add(discount);
            form.add(new JLabel("Stock")); form.add(stock);
            ProductPhoto photo = new ProductPhoto(existingPhoto);
            photo.setBorder(BorderFactory.createTitledBorder("Product photo (optional)"));
            JPanel editor = new JPanel(new BorderLayout(8, 12));
            editor.add(form, BorderLayout.NORTH);
            editor.add(photo, BorderLayout.CENTER);
            int answer=JOptionPane.showConfirmDialog(this,editor,id==null?"Add product":"Update product",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(answer!=JOptionPane.OK_OPTION) return;

            try {
                BigDecimal p=new BigDecimal(price.getText().trim()).setScale(2,RoundingMode.HALF_UP);
                BigDecimal discountPercent;
                try {
                    discountPercent = new BigDecimal(discount.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Enter a discount from 0 to 100 percent.");
                }
                if (discountPercent.signum() < 0 || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Enter a discount from 0 to 100 percent.");
                }
                discountPercent = discountPercent.setScale(2, RoundingMode.HALF_UP);
                int qty=Integer.parseInt(stock.getText().trim());
                if(barcode.getText().isBlank() || name.getText().isBlank() || p.signum()<0 || qty<0)
                    throw new IllegalArgumentException("Complete all fields using valid values.");

                try(Connection c=DB.getConnection()) {
                    if(id==null) {
                        try(PreparedStatement ps=c.prepareStatement(
                                "INSERT INTO products(barcode,name,category,price,discount_percent,stock,photo_jpeg) VALUES(?,?,?,?,?,?,?)")) {
                            ps.setString(1,barcode.getText().trim());
                            ps.setString(2,name.getText().trim());
                            ps.setString(3,category.getText().trim());
                            ps.setBigDecimal(4,p);
                            ps.setBigDecimal(5,discountPercent);
                            ps.setInt(6,qty);
                            ps.setBytes(7,photo.jpeg());
                            ps.executeUpdate();
                        }
                        audit("ADD_PRODUCT",name.getText().trim());
                    } else {
                        try(PreparedStatement ps=c.prepareStatement(
                                "UPDATE products SET barcode=?,name=?,category=?,price=?,discount_percent=?,stock=?,"
                                + "photo_jpeg=CASE WHEN ? THEN ? ELSE photo_jpeg END,version=version+1 WHERE id=?")) {
                            ps.setString(1,barcode.getText().trim());
                            ps.setString(2,name.getText().trim());
                            ps.setString(3,category.getText().trim());
                            ps.setBigDecimal(4,p);
                            ps.setBigDecimal(5,discountPercent);
                            ps.setInt(6,qty);
                            ps.setBoolean(7,photo.changed());
                            ps.setBytes(8,photo.jpeg());
                            ps.setInt(9,id);
                            ps.executeUpdate();
                        }
                        int delta=qty-oldStock;
                        if(delta!=0) {
                            try(PreparedStatement ps=c.prepareStatement(
                                    "INSERT INTO stock_movements(product_id,user_id,movement_type,quantity_delta,reason) "
                                    + "VALUES(?,?,'ADJUSTMENT',?,'Stock changed from product editor')")) {
                                ps.setInt(1,id); ps.setInt(2,user.id()); ps.setInt(3,delta); ps.executeUpdate();
                            }
                        }
                        audit("UPDATE_PRODUCT",name.getText().trim());
                    }
                }
                reload();
            } catch(Exception ex) {
                error("Could not save product: "+ex.getMessage());
            }
        }

        private void deactivateSelected() {
            Integer id=selectedId();
            if(id==null) { info("Select a product first."); return; }
            if(JOptionPane.showConfirmDialog(this,"Deactivate this product? It will no longer appear in barcode search.",
                    "Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(
                    "UPDATE products SET active=0,version=version+1 WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate();
                audit("DEACTIVATE_PRODUCT","Product id "+id);
                reload();
            } catch(SQLException ex) { error(ex.getMessage()); }
        }

        private void adjustStock() {
            Integer id=selectedId();
            if(id==null) { info("Select a product first."); return; }
            String value=JOptionPane.showInputDialog(this,"Stock adjustment (+ to add, - to remove):","0");
            if(value==null) return;
            try {
                int delta=Integer.parseInt(value.trim());
                if(delta==0) return;
                try(Connection c=DB.getConnection()) {
                    c.setAutoCommit(false);
                    try {
                        try(PreparedStatement ps=c.prepareStatement(
                                "UPDATE products SET stock=stock+?,version=version+1 WHERE id=? AND stock+?>=0")) {
                            ps.setInt(1,delta); ps.setInt(2,id); ps.setInt(3,delta);
                            if(ps.executeUpdate()!=1) throw new SQLException("Stock cannot become negative.");
                        }
                        try(PreparedStatement ps=c.prepareStatement(
                                "INSERT INTO stock_movements(product_id,user_id,movement_type,quantity_delta,reason) "
                                + "VALUES(?,?,'ADJUSTMENT',?,'Manual stock adjustment')")) {
                            ps.setInt(1,id); ps.setInt(2,user.id()); ps.setInt(3,delta); ps.executeUpdate();
                        }
                        c.commit();
                    } catch(Exception ex) {
                        c.rollback();
                        throw ex;
                    } finally {
                        c.setAutoCommit(true);
                    }
                }
                audit("ADJUST_STOCK","Product "+id+" delta "+delta);
                reload();
            } catch(Exception ex) { error("Stock adjustment failed: "+ex.getMessage()); }
        }
    }

    private class ReportsPanel extends JPanel {
        private final DefaultTableModel model=new DefaultTableModel(
                new Object[]{"Receipt","Date","Cashier","Lines","Total","Tender","Change","Status"},0) {
            @Override public boolean isCellEditable(int r,int c) { return false; }
        };
        private final JTable table=new JTable(model);
        private final JLabel summary=new JLabel("Sales summary");
        private final JTextField from=new JTextField(LocalDate.now().toString(),10);
        private final JTextField to=new JTextField(LocalDate.now().toString(),10);

        ReportsPanel() {
            setLayout(new BorderLayout(10,10));
            setBackground(BG);
            setBorder(new EmptyBorder(15,15,15,15));
            JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.setOpaque(false);
            top.add(new JLabel("From (YYYY-MM-DD):")); top.add(from);
            top.add(new JLabel("To:")); top.add(to);
            JButton apply=new JButton("Apply filter");
            JButton today=new JButton("Today");
            JButton details=new JButton("View sale details");
            top.add(apply); top.add(today); top.add(details);
            add(top,BorderLayout.NORTH);
            table.setRowHeight(27);
            add(new JScrollPane(table),BorderLayout.CENTER);
            summary.setFont(new Font("SansSerif",Font.BOLD,15));
            JPanel bottom=new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(summary,BorderLayout.WEST);
            add(bottom,BorderLayout.SOUTH);
            apply.addActionListener(e -> reload());
            today.addActionListener(e -> {
                from.setText(LocalDate.now().toString());
                to.setText(LocalDate.now().toString());
                reload();
            });
            details.addActionListener(e -> showSaleDetails());
            reload();
        }

        void reload() {
            model.setRowCount(0);
            try {
                LocalDate d1=LocalDate.parse(from.getText().trim());
                LocalDate d2=LocalDate.parse(to.getText().trim());
                if(d2.isBefore(d1)) throw new IllegalArgumentException("To date must not be before From date.");
                String sql="SELECT s.receipt_no,s.created_at,u.username,COUNT(si.id) AS line_count,"
                        + "s.total,s.tender,s.change_amount,s.status "
                        + "FROM sales s JOIN users u ON s.user_id=u.id "
                        + "LEFT JOIN sale_items si ON s.id=si.sale_id "
                        + "WHERE DATE(s.created_at) BETWEEN ? AND ? GROUP BY s.id ORDER BY s.created_at DESC";
                BigDecimal total=BigDecimal.ZERO;
                int count=0;
                try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(sql)) {
                    ps.setDate(1,java.sql.Date.valueOf(d1));
                    ps.setDate(2,java.sql.Date.valueOf(d2));
                    try(ResultSet rs=ps.executeQuery()) {
                        while(rs.next()) {
                            model.addRow(new Object[]{rs.getString("receipt_no"),rs.getTimestamp("created_at"),
                                    rs.getString("username"),rs.getInt("line_count"),rs.getBigDecimal("total"),
                                    rs.getBigDecimal("tender"),rs.getBigDecimal("change_amount"),rs.getString("status")});
                            if("COMPLETED".equals(rs.getString("status"))) total=total.add(rs.getBigDecimal("total"));
                            count++;
                        }
                    }
                }
                summary.setText("Transactions: "+count+"     Completed sales: PHP "+money(total));
            } catch(Exception ex) { error("Report error: "+ex.getMessage()); }
        }

        private void showSaleDetails() {
            int viewRow=table.getSelectedRow();
            if(viewRow<0) { info("Select a sale first."); return; }
            int row=table.convertRowIndexToModel(viewRow);
            String receipt=(String)model.getValueAt(row,0);
            String sql="SELECT product_name_snapshot,barcode_snapshot,unit_price,quantity,line_total,"
                    + "COALESCE(regular_unit_price, unit_price) AS regular_price, discount_percent "
                    + "FROM sale_items si JOIN sales s ON si.sale_id=s.id WHERE s.receipt_no=?";
            try(Connection c=DB.getConnection();PreparedStatement ps=c.prepareStatement(sql)) {
                ps.setString(1,receipt);
                try(ResultSet rs=ps.executeQuery()) {
                    StringBuilder b=new StringBuilder("Receipt: ").append(receipt).append("\n\n");
                    while(rs.next()) b.append(rs.getString(1)).append(" | ").append(rs.getString(2))
                            .append(" | ").append(rs.getInt(4)).append(" x PHP ").append(money(rs.getBigDecimal(3)))
                            .append(discountDescription(rs.getBigDecimal("regular_price"), rs.getBigDecimal("discount_percent")))
                            .append(" = PHP ").append(money(rs.getBigDecimal(5))).append("\n");
                    JTextArea area=new JTextArea(b.toString(),16,60);
                    area.setEditable(false);
                    JOptionPane.showMessageDialog(this,new JScrollPane(area),"Sale details",JOptionPane.PLAIN_MESSAGE);
                }
            } catch(SQLException ex) { error(ex.getMessage()); }
        }
    }

    private class ReceiptDialog extends JDialog {
        ReceiptDialog(JFrame owner,String receipt) {
            super(owner,"Receipt",true);
            JTextArea area=new JTextArea(receipt,24,42);
            area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            area.setEditable(false);
            JButton save=new JButton("Save .txt");
            JButton print=new JButton("Print");
            JButton close=new JButton("Close");
            save.addActionListener(e -> {
                JFileChooser chooser=new JFileChooser();
                chooser.setSelectedFile(new java.io.File("QuickMart-Receipt.txt"));
                if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
                    try {
                        java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(),area.getText());
                        JOptionPane.showMessageDialog(this,"Receipt saved.");
                    } catch(Exception ex) { error("Could not save receipt: "+ex.getMessage()); }
                }
            });
            print.addActionListener(e -> {
                try { area.print(); }
                catch(Exception ex) { error("Printing failed: "+ex.getMessage()); }
            });
            close.addActionListener(e -> dispose());
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(save); buttons.add(print); buttons.add(close);
            add(new JScrollPane(area),BorderLayout.CENTER);
            add(buttons,BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(owner);
        }
    }
}
