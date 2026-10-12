package cl.biblioteca.controlador;

import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.dao.ReporteDAO;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Prestamo;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PrestamoControlador {
    public static final int PLAZO_DIAS = 7;
    // Compartido por los controladores de la aplicación; MySQL protege entre procesos.
    private static final Object BLOQUEO_PRESTAMOS = new Object();
    private final BibliotecaControlador biblioteca;
    private final PrestamoDAO prestamos = new PrestamoDAO();
    private final ReporteDAO reportes = new ReporteDAO();

    public PrestamoControlador(BibliotecaControlador biblioteca) { this.biblioteca = biblioteca; }

    public void prestar(int estudiante, int libro) throws SQLException {
        synchronized (BLOQUEO_PRESTAMOS) {
            validarEstudiante(estudiante);
            LocalDate hoy = LocalDate.now();
            Prestamo prestamo = new Prestamo();
            prestamo.setIdEstudiante(estudiante); prestamo.setIdLibro(libro);
            prestamo.setFechaPrestamo(hoy); prestamo.setFechaDevolucion(hoy.plusDays(PLAZO_DIAS));
            prestamos.registrar(prestamo);
        }
    }

    public void devolver(int id) throws SQLException {
        synchronized (BLOQUEO_PRESTAMOS) {
            prestamos.devolver(id, estudiantePermitido());
        }
    }

    public List<Object[]> activos() throws SQLException {
        return reportes.actualmentePrestados(estudiantePermitido());
    }

    public List<Object[]> masPrestados() throws SQLException { return reportes.masPrestados(); }

    public List<Object[]> historial(int estudiante) throws SQLException {
        validarEstudiante(estudiante);
        return reportes.historial(estudiante);
    }

    private Integer estudiantePermitido() throws SQLException {
        if (biblioteca.getUsuario().esBibliotecario()) return null;
        List<Estudiante> lista = biblioteca.estudiantes();
        if (lista.size() != 1) throw new SQLException("No hay un estudiante asociado al RUT de este usuario.");
        return lista.get(0).getId();
    }

    private void validarEstudiante(int estudiante) throws SQLException {
        Integer permitido = estudiantePermitido();
        if (estudiante <= 0 || (permitido != null && permitido != estudiante)) {
            throw new SQLException("Seleccione un estudiante permitido.");
        }
    }
}
