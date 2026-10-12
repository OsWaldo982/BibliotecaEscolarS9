package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.modelo.Prestamo;
import java.sql.*;
import java.time.LocalDate;

/** El registro en prestamos constituye también el historial. */
public class PrestamoDAO {
    public void registrar(Prestamo prestamo) throws SQLException {
        int estudiante = prestamo.getIdEstudiante();
        int libro = prestamo.getIdLibro();
        LocalDate fecha = prestamo.getFechaPrestamo();
        LocalDate vencimiento = prestamo.getFechaDevolucion();
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE libros SET stock = stock - 1 WHERE id = ? AND stock > 0")) {
                    ps.setInt(1, libro);
                    if (ps.executeUpdate() != 1) throw new SQLException("El libro no tiene stock disponible.");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO prestamos (id_estudiante, id_libro, fecha_prestamo, fecha_devolucion, devuelto) VALUES (?, ?, ?, ?, false)")) {
                    ps.setInt(1, estudiante);
                    ps.setInt(2, libro);
                    ps.setDate(3, Date.valueOf(fecha));
                    ps.setDate(4, Date.valueOf(vencimiento));
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    public void devolver(int prestamo, Integer estudiantePermitido) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                int libro;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id_libro, id_estudiante, devuelto FROM prestamos WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, prestamo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El préstamo no existe.");
                        if (estudiantePermitido != null && estudiantePermitido != rs.getInt("id_estudiante")) {
                            throw new SQLException("No puede devolver este préstamo.");
                        }
                        if (rs.getBoolean("devuelto")) throw new SQLException("El préstamo ya fue devuelto.");
                        libro = rs.getInt("id_libro");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE prestamos SET devuelto = true WHERE id = ? AND devuelto = false")) {
                    ps.setInt(1, prestamo);
                    if (ps.executeUpdate() != 1) throw new SQLException("El préstamo ya fue devuelto.");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE libros SET stock = stock + 1 WHERE id = ?")) {
                    ps.setInt(1, libro);
                    if (ps.executeUpdate() != 1) throw new SQLException("El libro ya no existe.");
                }
                c.commit();
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
