package cl.biblioteca.controlador;

import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.vista.LoginVista;
import cl.biblioteca.vista.PrincipalVista;
import cl.biblioteca.vista.TareasSwing;
import java.sql.SQLException;
import java.util.Arrays;

public class LoginControlador {
    public LoginControlador(LoginVista vista) {
        vista.getIngresar().addActionListener(e -> {
            String rut = vista.getRut().getText().trim();
            char[] password = vista.getPassword().getPassword();
            vista.getPassword().setText("");
            TareasSwing.ejecutar(vista, vista.getIngresar(), () -> {
                try {
                    if (rut.isEmpty() || password.length == 0) throw new SQLException("Ingrese RUT y contraseña.");
                    var usuario = new UsuarioDAO().autenticar(rut, new String(password));
                    if (usuario == null) throw new SQLException("Credenciales incorrectas.");
                    return usuario;
                } finally { Arrays.fill(password, '\0'); }
            }, usuario -> {
                new PrincipalVista(new BibliotecaControlador(usuario)).setVisible(true);
                vista.dispose();
            });
        });
    }
}
