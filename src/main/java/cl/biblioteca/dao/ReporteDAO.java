package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Consultas para los tres informes exigidos. */
public class ReporteDAO {
    public List<Object[]> masPrestados() throws SQLException {
        return consultar("SELECT l.titulo, l.autor, COUNT(*) AS cantidad FROM prestamos p "
                + "JOIN libros l ON l.id = p.id_libro GROUP BY l.id, l.titulo, l.autor "
                + "ORDER BY cantidad DESC, l.titulo", null);
    }

    public List<Object[]> historial(int estudiante) throws SQLException {
        return consultar(consultaPrestamos() + " WHERE p.id_estudiante = ? ORDER BY p.fecha_prestamo DESC, p.id DESC", estudiante);
    }

    public List<Object[]> actualmentePrestados(Integer estudiante) throws SQLException {
        return consultar(consultaPrestamos() + " WHERE p.devuelto = false"
                + (estudiante == null ? "" : " AND p.id_estudiante = ?") + " ORDER BY p.fecha_devolucion, p.id", estudiante);
    }

    private String consultaPrestamos() {
        return "SELECT p.id, e.nombre, l.titulo, p.fecha_prestamo, p.fecha_devolucion, "
                + "CASE WHEN p.devuelto = true THEN 'Devuelto' "
                + "WHEN p.fecha_devolucion < CURRENT_DATE THEN 'Atrasado' ELSE 'En préstamo' END AS estado "
                + "FROM prestamos p JOIN estudiantes e ON e.id = p.id_estudiante JOIN libros l ON l.id = p.id_libro";
    }

    private List<Object[]> consultar(String sql, Integer estudiante) throws SQLException {
        List<Object[]> filas = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (estudiante != null) ps.setInt(1, estudiante);
            try (ResultSet rs = ps.executeQuery()) {
                int columnas = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] fila = new Object[columnas];
                    for (int i = 0; i < columnas; i++) fila[i] = rs.getObject(i + 1);
                    filas.add(fila);
                }
            }
        }
        return filas;
    }
}
