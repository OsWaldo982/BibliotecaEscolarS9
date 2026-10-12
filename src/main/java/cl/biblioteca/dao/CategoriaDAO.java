package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.modelo.Categoria;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO implements CrudDAO<Categoria> {
    @Override
    public List<Categoria> listar() throws SQLException {
        List<Categoria> resultado = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nombre FROM categorias ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Categoria entidad = new Categoria();
                entidad.setId(rs.getInt("id"));
                entidad.setNombre(rs.getString("nombre"));
                resultado.add(entidad);
            }
        }
        return resultado;
    }

    @Override
    public void crear(Categoria entidad) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO categorias (nombre) VALUES (?)")) {
            ps.setString(1, entidad.getNombre());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void actualizar(Categoria entidad) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE categorias SET nombre = ? WHERE id = ?")) {
            ps.setString(1, entidad.getNombre());
            ps.setInt(2, entidad.getId());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        // MySQL rechaza eliminaciones que incumplan las relaciones existentes.
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM categorias WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("El registro ya no existe.");
        }
    }
}
