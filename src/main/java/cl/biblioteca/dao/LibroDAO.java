package cl.biblioteca.dao;

import cl.biblioteca.conexion.DatabaseConnection;
import cl.biblioteca.modelo.Libro;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibroDAO implements CrudDAO<Libro> {
    @Override
    public List<Libro> listar() throws SQLException {
        List<Libro> resultado = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, titulo, autor, isbn, editorial, stock, id_categoria FROM libros ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Libro entidad = new Libro();
                entidad.setId(rs.getInt("id"));
                entidad.setTitulo(rs.getString("titulo"));
                entidad.setAutor(rs.getString("autor"));
                entidad.setIsbn(rs.getString("isbn"));
                entidad.setEditorial(rs.getString("editorial"));
                entidad.setStock(rs.getInt("stock"));
                entidad.setIdCategoria(rs.getInt("id_categoria"));
                resultado.add(entidad);
            }
        }
        return resultado;
    }

    @Override
    public void crear(Libro entidad) throws SQLException {
        if (entidad.getStock() < 0) throw new SQLException("El stock no puede ser negativo.");
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO libros (titulo, autor, isbn, editorial, stock, id_categoria) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, entidad.getTitulo());
            ps.setString(2, entidad.getAutor());
            ps.setString(3, entidad.getIsbn());
            ps.setString(4, entidad.getEditorial());
            ps.setInt(5, entidad.getStock());
            ps.setInt(6, entidad.getIdCategoria());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void actualizar(Libro entidad) throws SQLException {
        if (entidad.getStock() < 0) throw new SQLException("El stock no puede ser negativo.");
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE libros SET titulo = ?, autor = ?, isbn = ?, editorial = ?, stock = ?, id_categoria = ? WHERE id = ?")) {
            ps.setString(1, entidad.getTitulo());
            ps.setString(2, entidad.getAutor());
            ps.setString(3, entidad.getIsbn());
            ps.setString(4, entidad.getEditorial());
            ps.setInt(5, entidad.getStock());
            ps.setInt(6, entidad.getIdCategoria());
            ps.setInt(7, entidad.getId());
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo guardar el registro.");
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        // MySQL rechaza eliminaciones que incumplan las relaciones existentes.
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM libros WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) throw new SQLException("El registro ya no existe.");
        }
    }
}
