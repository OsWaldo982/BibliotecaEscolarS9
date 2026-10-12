package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.modelo.Estudiante;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EstudianteDAO implements CrudDAO<Estudiante> {
    @Override
    public List<Estudiante> listar() throws SQLException {
        List<Estudiante> resultado = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nombre, rut, curso, correo FROM estudiantes ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Estudiante entidad = new Estudiante();
                entidad.setId(rs.getInt("id"));
                entidad.setNombre(rs.getString("nombre"));
                entidad.setRut(rs.getString("rut"));
                entidad.setCurso(rs.getString("curso"));
                entidad.setCorreo(rs.getString("correo"));
                resultado.add(entidad);
            }
        }
        return resultado;
    }

    @Override
    public void crear(Estudiante entidad) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO estudiantes (nombre, rut, curso, correo) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, entidad.getNombre());
            ps.setString(2, entidad.getRut());
            ps.setString(3, entidad.getCurso());
            ps.setString(4, entidad.getCorreo());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void actualizar(Estudiante entidad) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE estudiantes SET nombre = ?, rut = ?, curso = ?, correo = ? WHERE id = ?")) {
            ps.setString(1, entidad.getNombre());
            ps.setString(2, entidad.getRut());
            ps.setString(3, entidad.getCurso());
            ps.setString(4, entidad.getCorreo());
            ps.setInt(5, entidad.getId());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        // MySQL rechaza eliminaciones que incumplan las relaciones existentes.
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM estudiantes WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("El registro ya no existe.");
        }
    }
}
