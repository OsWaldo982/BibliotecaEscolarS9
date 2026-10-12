package cl.biblioteca;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.controlador.BibliotecaControlador;
import cl.biblioteca.controlador.PrestamoControlador;
import cl.biblioteca.dao.*;
import cl.biblioteca.modelo.*;
import java.sql.*;

/** Verificación real de lectura; no inserta, modifica ni elimina registros. */
public class VerificacionLectura {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) throws Exception {
        comprobar(DatabaseConnection.getInstance() == DatabaseConnection.getInstance(), "Singleton");
        try (Connection primera = DatabaseConnection.getInstance().getConnection();
             Connection segunda = DatabaseConnection.getInstance().getConnection()) {
            comprobar(primera != segunda && primera.isValid(3) && segunda.isValid(3), "Conexiones independientes");
            try (Statement s = primera.createStatement(); ResultSet rs = s.executeQuery(
                    "SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'biblioteca'")) {
                int tablas = 0;
                while (rs.next()) {
                    comprobar("InnoDB".equalsIgnoreCase(rs.getString("ENGINE")), "Se requiere InnoDB para transacciones");
                    tablas++;
                }
                comprobar(tablas == 5, "Se esperaban las cinco tablas académicas");
            }
            for (String tabla : new String[]{"usuarios", "estudiantes", "categorias", "libros", "prestamos"}) {
                try (Statement s = primera.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                    rs.next(); System.out.println(tabla + ": " + rs.getInt(1) + " registros");
                }
            }
        }
        UsuarioDAO dao = new UsuarioDAO();
        Usuario bibliotecario = dao.autenticar("12345678-9", "clave123");
        Usuario estudiante = dao.autenticar("98765432-1", "clave123");
        comprobar(bibliotecario != null && bibliotecario.esBibliotecario(), "Autenticación bibliotecario");
        comprobar(estudiante != null && "estudiante".equals(estudiante.getRol()), "Autenticación estudiante");
        comprobar(dao.autenticar("98765432-1", "incorrecta") == null, "Contraseña incorrecta");
        comprobar(dao.autenticar("98765432-1", "CLAVE123") == null, "Contraseña distingue mayúsculas");
        BibliotecaControlador admin = new BibliotecaControlador(bibliotecario);
        BibliotecaControlador alumno = new BibliotecaControlador(estudiante);
        comprobar(!admin.libros().isEmpty(), "Consulta libros");
        comprobar(!admin.categorias().isEmpty(), "Consulta categorías");
        comprobar(!admin.estudiantes().isEmpty(), "Consulta estudiantes");
        comprobar(alumno.estudiantes().size() == 1, "Estudiante asociado por RUT");
        try {
            alumno.guardar(new Categoria());
            throw new AssertionError("Se permitió administrar a estudiante");
        } catch (SQLException esperado) {
            comprobar(esperado.getMessage().contains("bibliotecario"), "Permiso de administración");
        }
        PrestamoControlador prestamos = new PrestamoControlador(admin);
        System.out.println("Más prestados: " + prestamos.masPrestados().size());
        System.out.println("Historial estudiante 1: " + prestamos.historial(1).size());
        System.out.println("Préstamos activos: " + prestamos.activos().size());
        PrestamoControlador propios = new PrestamoControlador(alumno);
        try {
            propios.historial(2);
            throw new AssertionError("Se permitió historial ajeno");
        } catch (SQLException esperado) {
            comprobar(esperado.getMessage().contains("permitido"), "Permiso de historial");
        }
        Persona persona = estudiante;
        comprobar(persona.getDescripcion().contains("estudiante"), "Polimorfismo");
        System.out.println("OK: conexión, Singleton, autenticación, permisos, consultas y tres reportes.");
    }
}
