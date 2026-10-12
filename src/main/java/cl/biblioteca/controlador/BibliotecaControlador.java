package cl.biblioteca.controlador;

import cl.biblioteca.dao.*;
import cl.biblioteca.modelo.*;
import java.sql.SQLException;
import java.util.List;

/** Aplica permisos y validaciones antes de acceder a los DAO. */
public class BibliotecaControlador {
    private final Usuario usuario;
    private final LibroDAO libros = new LibroDAO();
    private final EstudianteDAO estudiantes = new EstudianteDAO();
    private final CategoriaDAO categorias = new CategoriaDAO();

    public BibliotecaControlador(Usuario usuario) {
        if (usuario == null || !("bibliotecario".equals(usuario.getRol()) || "estudiante".equals(usuario.getRol()))) {
            throw new IllegalArgumentException("Se requiere un usuario autenticado con un rol válido.");
        }
        this.usuario = usuario;
    }

    public Usuario getUsuario() { return usuario; }
    public List<Libro> libros() throws SQLException { return libros.listar(); }
    public List<Categoria> categorias() throws SQLException { return categorias.listar(); }

    public List<Estudiante> estudiantes() throws SQLException {
        List<Estudiante> lista = estudiantes.listar();
        if (usuario.esBibliotecario()) return lista;
        return lista.stream().filter(e -> usuario.getRut().equals(e.getRut())).toList();
    }

    public void guardar(Libro libro) throws SQLException {
        exigirBibliotecario();
        texto(libro.getTitulo(), "Título", 200); texto(libro.getAutor(), "Autor", 100);
        texto(libro.getIsbn(), "ISBN", 20); texto(libro.getEditorial(), "Editorial", 100);
        if (libro.getStock() < 0) throw new SQLException("El stock no puede ser negativo.");
        if (libro.getIdCategoria() <= 0) throw new SQLException("Seleccione una categoría.");
        if (libro.getId() == 0) libros.crear(libro); else libros.actualizar(libro);
    }

    public void guardar(Estudiante estudiante) throws SQLException {
        exigirBibliotecario();
        texto(estudiante.getNombre(), "Nombre", 100); texto(estudiante.getRut(), "RUT", 12);
        texto(estudiante.getCurso(), "Curso", 20); texto(estudiante.getCorreo(), "Correo", 100);
        if (estudiante.getId() == 0) estudiantes.crear(estudiante); else estudiantes.actualizar(estudiante);
    }

    public void guardar(Categoria categoria) throws SQLException {
        exigirBibliotecario(); texto(categoria.getNombre(), "Nombre", 100);
        if (categoria.getId() == 0) categorias.crear(categoria); else categorias.actualizar(categoria);
    }

    public void eliminarLibro(int id) throws SQLException { exigirBibliotecario(); libros.eliminar(id); }
    public void eliminarEstudiante(int id) throws SQLException { exigirBibliotecario(); estudiantes.eliminar(id); }
    public void eliminarCategoria(int id) throws SQLException { exigirBibliotecario(); categorias.eliminar(id); }

    public void exigirBibliotecario() throws SQLException {
        if (!usuario.esBibliotecario()) throw new SQLException("Esta operación requiere el rol bibliotecario.");
    }

    private void texto(String valor, String campo, int maximo) throws SQLException {
        if (valor == null || valor.isBlank()) throw new SQLException(campo + " es obligatorio.");
        if (valor.length() > maximo) throw new SQLException(campo + " admite hasta " + maximo + " caracteres.");
    }
}
