package cl.biblioteca;

import cl.biblioteca.controlador.*;
import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.vista.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Prueba de componentes propios, renderizado y SwingWorker; solo consulta MySQL. */
public class VerificacionSwing {
    public static void main(String[] args) throws Exception {
        Usuario admin = new UsuarioDAO().autenticar("12345678-9", "clave123");
        Usuario alumno = new UsuarioDAO().autenticar("98765432-1", "clave123");
        CountDownLatch terminada = new CountDownLatch(1);
        List<Throwable> errores = new CopyOnWriteArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            if (!SwingUtilities.isEventDispatchThread()) throw new AssertionError("EDT");
            JButton boton = new JButton();
            TareasSwing.ejecutar(boton, boton, () -> {
                if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("JDBC ejecutado en EDT");
                return new cl.biblioteca.dao.LibroDAO().listar();
            }, libros -> {
                if (!SwingUtilities.isEventDispatchThread() || libros.isEmpty()) errores.add(new AssertionError("Callback EDT y consulta"));
                terminada.countDown();
            });
        });
        if (!terminada.await(20, TimeUnit.SECONDS) || !errores.isEmpty()) throw new AssertionError("SwingWorker");
        Path salida = Path.of("target", "verificacion-visual"); Files.createDirectories(salida);
        SwingUtilities.invokeAndWait(() -> {
            LoginVista login = new LoginVista(); login.setVisible(true);
            try { renderizar(login, salida.resolve("login.png")); }
            catch (Exception ex) { errores.add(ex); }
            finally { login.dispose(); }
        });
        // Las ventanas de prueba se renderizan con los componentes reales y se cierran.
        for (Usuario usuario : List.of(admin, alumno)) {
            PrincipalVista[] vista = new PrincipalVista[1];
            SwingUtilities.invokeAndWait(() -> {
                vista[0] = new PrincipalVista(new BibliotecaControlador(usuario));
                vista[0].setVisible(true);
            });
            CountDownLatch cargada = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                Timer timer = new Timer(100, null);
                timer.addActionListener(e -> {
                    List<JTable> tablas = new ArrayList<>(); recolectar(vista[0], JTable.class, tablas);
                    boolean librosCargados = !tablas.isEmpty() && tablas.get(0).getRowCount() == 10;
                    if (librosCargados) { timer.stop(); cargada.countDown(); }
                }); timer.start();
            });
            if (!cargada.await(20, TimeUnit.SECONDS)) throw new AssertionError("Carga asíncrona de libros");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    List<JButton> botones = new ArrayList<>(); recolectar(vista[0], JButton.class, botones);
                    boolean administra = botones.stream().anyMatch(b -> "Gestión de estudiantes".equals(b.getText()));
                    if (administra != usuario.esBibliotecario()) throw new AssertionError("Menú por rol");
                    if (!usuario.esBibliotecario() && botones.stream().anyMatch(b -> "Guardar".equals(b.getText()) && b.getParent() != null)) {
                        throw new AssertionError("CRUD visible al estudiante");
                    }
                    renderizar(vista[0], salida.resolve(usuario.getRol() + ".png"));
                    JPanel tarjetas = null;
                    JPanel menu = null;
                    for (Component componente : vista[0].getContentPane().getComponents()) {
                        if (componente instanceof JPanel panel) {
                            if (panel.getLayout() instanceof CardLayout) tarjetas = panel;
                            else if (panel.getLayout() instanceof FlowLayout) menu = panel;
                        }
                    }
                    if (tarjetas == null || menu == null) throw new AssertionError("Navegación principal");
                    int numero = 0;
                    for (Component componente : menu.getComponents()) {
                        JButton opcion = (JButton) componente;
                        ((CardLayout) tarjetas.getLayout()).show(tarjetas, opcion.getText());
                        renderizar(vista[0], salida.resolve(usuario.getRol() + "-pantalla-" + numero++ + ".png"));
                    }
                } catch (Exception ex) { errores.add(ex); }
                finally { vista[0].dispose(); }
            });
        }
        if (!errores.isEmpty()) throw new AssertionError(errores.toString());
        System.out.println("OK: JDBC fuera de EDT, callback en EDT, carga asíncrona y menús por rol. PNG en target/verificacion-visual.");
        System.exit(0);
    }

    private static void renderizar(JFrame vista, Path destino) throws Exception {
        BufferedImage imagen = new BufferedImage(vista.getWidth(), vista.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics(); vista.printAll(g); g.dispose();
        ImageIO.write(imagen, "png", destino.toFile());
    }

    private static <T extends Component> void recolectar(Component actual, Class<T> tipo, List<T> encontrados) {
        if (tipo.isInstance(actual)) encontrados.add(tipo.cast(actual));
        if (actual instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) recolectar(hijo, tipo, encontrados);
        }
    }
}
