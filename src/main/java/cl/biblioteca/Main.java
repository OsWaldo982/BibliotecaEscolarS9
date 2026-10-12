package cl.biblioteca;

import cl.biblioteca.controlador.LoginControlador;
import cl.biblioteca.vista.LoginVista;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginVista vista = new LoginVista();
            new LoginControlador(vista);
            vista.setVisible(true);
        });
    }
}
