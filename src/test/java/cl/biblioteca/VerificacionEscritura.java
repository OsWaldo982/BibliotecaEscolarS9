package cl.biblioteca;

import cl.biblioteca.controlador.*;
import cl.biblioteca.dao.*;
import cl.biblioteca.modelo.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.*;

/** Prueba de integración real, optativa. Solo elimina sus propios registros nuevos.
 * No ejecutar sin autorización: crea datos y consume identificadores AUTO_INCREMENT.
 */
public class VerificacionEscritura {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"--autorizar-datos-nuevos".equals(args[0])) {
            throw new IllegalArgumentException("Requiere autorización explícita para datos nuevos y su limpieza.");
        }
        var usuario = new UsuarioDAO().autenticar("12345678-9", "clave123");
        BibliotecaControlador biblioteca = new BibliotecaControlador(usuario);
        PrestamoControlador prestamos = new PrestamoControlador(biblioteca);
        String marca = Long.toString(System.currentTimeMillis());
        Categoria categoria = new Categoria(); categoria.setNombre("Prueba EFT " + marca);
        Estudiante estudiante = new Estudiante(); estudiante.setNombre("Prueba EFT " + marca);
        estudiante.setRut(marca.substring(marca.length() - 8) + "-K");
        estudiante.setCurso("Prueba EFT"); estudiante.setCorreo("prueba@ejemplo.cl");
        Libro libro = new Libro(); libro.setTitulo("Prueba EFT " + marca); libro.setAutor("Prueba");
        libro.setIsbn("EFT" + marca); libro.setEditorial("Prueba"); libro.setStock(1);
        try {
            biblioteca.guardar(categoria);
            categoria = biblioteca.categorias().stream().filter(c -> c.getNombre().equals("Prueba EFT " + marca)).findFirst().orElseThrow();
            categoria.setNombre(categoria.getNombre() + " editada"); biblioteca.guardar(categoria);
            comprobar(biblioteca.categorias().stream().anyMatch(c -> c.getNombre().endsWith(marca + " editada")), "Actualizar categoría");
            biblioteca.guardar(estudiante);
            estudiante = biblioteca.estudiantes().stream().filter(e -> e.getNombre().equals("Prueba EFT " + marca)).findFirst().orElseThrow();
            estudiante.setCurso("Curso editado"); biblioteca.guardar(estudiante);
            final int idEstudiante = estudiante.getId();
            comprobar(biblioteca.estudiantes().stream().anyMatch(e -> e.getId() == idEstudiante && "Curso editado".equals(e.getCurso())), "Actualizar estudiante");
            libro.setIdCategoria(categoria.getId()); biblioteca.guardar(libro);
            libro = biblioteca.libros().stream().filter(l -> l.getIsbn().equals("EFT" + marca)).findFirst().orElseThrow();
            libro.setEditorial("Editorial editada"); biblioteca.guardar(libro);
            final int idLibro = libro.getId();
            comprobar(biblioteca.libros().stream().anyMatch(l -> l.getId() == idLibro && "Editorial editada".equals(l.getEditorial())), "Actualizar libro");
            Libro duplicado = new Libro(); duplicado.setTitulo("Duplicado"); duplicado.setAutor("Prueba");
            duplicado.setIsbn(libro.getIsbn()); duplicado.setEditorial("Prueba"); duplicado.setStock(1); duplicado.setIdCategoria(categoria.getId());
            falla(() -> biblioteca.guardar(duplicado), "ISBN duplicado");
            Estudiante duplicadoRut = new Estudiante(); duplicadoRut.setNombre("Duplicado"); duplicadoRut.setRut(estudiante.getRut());
            duplicadoRut.setCurso("Prueba"); duplicadoRut.setCorreo("prueba@ejemplo.cl");
            falla(() -> biblioteca.guardar(duplicadoRut), "RUT duplicado");
            duplicado.setIsbn("NEG" + marca); duplicado.setStock(-1);
            falla(() -> biblioteca.guardar(duplicado), "Stock negativo");
            PrestamoDAO dao = new PrestamoDAO();
            // La inserción inválida debe deshacer la disminución de stock.
            falla(() -> dao.registrar(nuevoPrestamo(-1, idLibro)), "Rollback");
            comprobar(stock(biblioteca, idLibro) == 1, "Stock tras rollback");
            // Dos operaciones reales, sin simulador ni pantalla adicional.
            int exitos = simultaneos(() -> prestamos.prestar(idEstudiante, idLibro));
            comprobar(exitos == 1 && stock(biblioteca, idLibro) == 0, "Préstamos sincronizados con stock 1");
            var historial = prestamos.historial(idEstudiante);
            comprobar(historial.size() == 1, "Historial persistente");
            Object[] fila = historial.get(0);
            comprobar(java.sql.Date.valueOf(LocalDate.now().plusDays(7)).equals(fila[4]), "Vencimiento siete días");
            int idPrestamo = ((Number) fila[0]).intValue();
            comprobar(prestamos.activos().stream().anyMatch(f -> ((Number) f[0]).intValue() == idPrestamo), "Informe activo");
            comprobar(prestamos.masPrestados().stream().anyMatch(f -> ("Prueba EFT " + marca).equals(f[0])), "Informe más prestados");
            final int categoriaRelacionada = categoria.getId();
            falla(() -> biblioteca.eliminarLibro(idLibro), "Relación libro-préstamo");
            falla(() -> biblioteca.eliminarEstudiante(idEstudiante), "Relación estudiante-préstamo");
            falla(() -> biblioteca.eliminarCategoria(categoriaRelacionada), "Relación categoría-libro");
            prestamos.devolver(idPrestamo);
            comprobar(stock(biblioteca, idLibro) == 1, "Restitución stock");
            falla(() -> prestamos.devolver(idPrestamo), "Devolución repetida");
            comprobar(stock(biblioteca, idLibro) == 1, "No se duplica stock");
            // Comprobar protección de MySQL también sin el bloqueo Java.
            exitos = simultaneos(() -> dao.registrar(nuevoPrestamo(idEstudiante, idLibro)));
            comprobar(exitos == 1 && stock(biblioteca, idLibro) == 0, "Conexiones concurrentes con stock 1");
            historial = prestamos.historial(idEstudiante);
            comprobar(historial.size() == 2, "Historial de ambos préstamos");
            int segundo = historial.stream().filter(f -> "En préstamo".equals(f[5])).map(f -> ((Number) f[0]).intValue()).findFirst().orElseThrow();
            exitos = simultaneos(() -> dao.devolver(segundo, null));
            comprobar(exitos == 1 && stock(biblioteca, idLibro) == 1, "Devoluciones concurrentes");
            System.out.println("OK: CRUD crear/consultar/actualizar, unicidad, transacciones, préstamos y devoluciones concurrentes.");
        } finally {
            // Limpieza limitada estrictamente a los registros identificados en esta ejecución.
            if (estudiante.getId() != 0 && libro.getId() != 0) {
                try (var c = cl.biblioteca.conexion.DatabaseConnection.getInstance().getConnection();
                     var ps = c.prepareStatement("DELETE FROM prestamos WHERE id_estudiante = ? AND id_libro = ?")) {
                    ps.setInt(1, estudiante.getId()); ps.setInt(2, libro.getId()); ps.executeUpdate();
                }
            }
            if (libro.getId() != 0) biblioteca.eliminarLibro(libro.getId());
            if (estudiante.getId() != 0) biblioteca.eliminarEstudiante(estudiante.getId());
            if (categoria.getId() != 0) biblioteca.eliminarCategoria(categoria.getId());
            comprobar(biblioteca.libros().stream().noneMatch(l -> l.getIsbn().equals("EFT" + marca)), "Eliminar libro");
            comprobar(biblioteca.estudiantes().stream().noneMatch(e -> e.getNombre().equals("Prueba EFT " + marca)), "Eliminar estudiante");
            comprobar(biblioteca.categorias().stream().noneMatch(c -> c.getNombre().startsWith("Prueba EFT " + marca)), "Eliminar categoría");
            System.out.println("OK: CRUD eliminar y limpieza exclusiva de los registros creados por esta prueba.");
        }
    }

    private static Prestamo nuevoPrestamo(int estudiante, int libro) {
        Prestamo prestamo = new Prestamo();
        prestamo.setIdEstudiante(estudiante); prestamo.setIdLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now()); prestamo.setFechaDevolucion(LocalDate.now().plusDays(7));
        return prestamo;
    }

    private static int stock(BibliotecaControlador biblioteca, int id) throws SQLException {
        return biblioteca.libros().stream().filter(l -> l.getId() == id).findFirst().orElseThrow().getStock();
    }

    @FunctionalInterface private interface Operacion { void ejecutar() throws Exception; }

    private static void falla(Operacion operacion, String mensaje) throws Exception {
        try { operacion.ejecutar(); throw new AssertionError(mensaje); }
        catch (SQLException esperado) { }
    }

    private static int simultaneos(Operacion operacion) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> tarea = () -> {
            inicio.await();
            try { operacion.ejecutar(); return true; } catch (SQLException esperado) { return false; }
        };
        try {
            Future<Boolean> a = hilos.submit(tarea); Future<Boolean> b = hilos.submit(tarea); inicio.countDown();
            return (a.get(30, TimeUnit.SECONDS) ? 1 : 0) + (b.get(30, TimeUnit.SECONDS) ? 1 : 0);
        } finally { hilos.shutdownNow(); }
    }
}
