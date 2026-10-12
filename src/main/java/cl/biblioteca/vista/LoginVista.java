package cl.biblioteca.vista;

import javax.swing.*;
import java.awt.*;

public class LoginVista extends JFrame {
    private final JTextField rut = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JButton ingresar = new JButton("Ingresar");

    public LoginVista() {
        super("Biblioteca escolar — Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel campos = new JPanel(new GridLayout(0, 2, 10, 10));
        campos.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        campos.add(new JLabel("RUT (sin puntos):")); campos.add(rut);
        campos.add(new JLabel("Contraseña:")); campos.add(password);
        campos.add(new JLabel("")); campos.add(ingresar);
        add(campos); getRootPane().setDefaultButton(ingresar);
        pack(); setLocationRelativeTo(null); setResizable(false);
    }

    public JTextField getRut() { return rut; }
    public JPasswordField getPassword() { return password; }
    public JButton getIngresar() { return ingresar; }
}
