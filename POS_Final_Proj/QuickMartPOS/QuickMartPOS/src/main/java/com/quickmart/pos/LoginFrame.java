
package com.quickmart.pos;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

public class LoginFrame extends JFrame {

    private static final Color NAVY = new Color(20, 44, 75);
    private static final Color ORANGE = new Color(234, 111, 48);

    private final JTextField username = new JTextField("cashier", 20);
    private final JPasswordField password = new JPasswordField(20);

    public LoginFrame() {
        setTitle("QuickMart POS | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 390);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(28, 32, 28, 32));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("QUICKMART");
        brand.setFont(new Font("SansSerif", Font.BOLD, 26));
        brand.setForeground(NAVY);

        JLabel subtitle = new JLabel("Point of Sale System");
        subtitle.setForeground(new Color(100, 116, 139));

        card.add(brand);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));

        card.add(fieldLabel("Username"));
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(username);

        card.add(Box.createVerticalStrut(12));

        card.add(fieldLabel("Password"));
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(password);

        card.add(Box.createVerticalStrut(20));

        JButton signIn = new JButton("Sign in");

        signIn.setBackground(NAVY);
        signIn.setForeground(Color.WHITE);
        signIn.setFont(new Font("SansSerif", Font.BOLD, 14));
        signIn.setFocusPainted(false);
        signIn.setBorderPainted(false);
        signIn.setContentAreaFilled(true);
        signIn.setOpaque(true);
        signIn.setRolloverEnabled(true);
        signIn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        signIn.setPreferredSize(new Dimension(300, 42));

        // Navy normally, orange when hovered or pressed.
        ChangeListener buttonColorListener = e -> {
            ButtonModel model = signIn.getModel();

            if (model.isRollover() || model.isPressed()) {
                signIn.setBackground(ORANGE);
            } else {
                signIn.setBackground(NAVY);
            }
        };

        signIn.getModel().addChangeListener(buttonColorListener);

        signIn.addActionListener(e -> doLogin());
        password.addActionListener(e -> doLogin());

        card.add(signIn);

        JLabel hint = new JLabel(
            "<html><br>Cashier: cashier / cashier123<br>"
            + "Admin: admin / admin123</html>"
        );

        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(new Color(100, 116, 139));

        card.add(hint);

        outer.add(card);
        setContentPane(outer);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(51, 65, 85));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void doLogin() {
        try {
            Auth.User user = Auth.login(
                username.getText(),
                new String(password.getPassword())
            );

            if (user == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Incorrect username/password or inactive account.",
                    "Sign in failed",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            new POSFrame(user).setVisible(true);
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Login error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
