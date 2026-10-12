package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.modelo.Usuario;
import java.sql.*;

public class UsuarioDAO {
    /** Conserva el formato de contraseñas del script académico. */
    public Usuario autenticar(String rut, String password) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, nombre, rut, correo, rol FROM usuarios WHERE rut = ? AND BINARY `contraseña` = ?")) {
            ps.setString(1, rut); ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id")); usuario.setNombre(rs.getString("nombre"));
                usuario.setRut(rs.getString("rut")); usuario.setCorreo(rs.getString("correo"));
                usuario.setRol(rs.getString("rol"));
                if (!"bibliotecario".equals(usuario.getRol()) && !"estudiante".equals(usuario.getRol())) {
                    throw new SQLException("Rol de usuario no permitido.");
                }
                return usuario;
            }
        }
    }
}
