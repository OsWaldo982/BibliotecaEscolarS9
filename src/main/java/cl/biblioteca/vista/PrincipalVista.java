package cl.biblioteca.vista;

import cl.biblioteca.controlador.BibliotecaControlador;
import cl.biblioteca.controlador.PrestamoControlador;
import javax.swing.*;
import java.awt.*;

public class PrincipalVista extends JFrame {
    public PrincipalVista(BibliotecaControlador biblioteca) {
        super("Biblioteca escolar — " + biblioteca.getUsuario().getDescripcion());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        PrestamoControlador prestamos = new PrestamoControlador(biblioteca);
        JPanel contenido = new JPanel(new CardLayout());
        JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        agregar(menu, contenido, biblioteca.getUsuario().esBibliotecario() ? "Gestión de libros" : "Consulta de libros",
                new GestionVista(biblioteca, GestionVista.Tipo.LIBROS));
        if (biblioteca.getUsuario().esBibliotecario()) {
            agregar(menu, contenido, "Gestión de estudiantes", new GestionVista(biblioteca, GestionVista.Tipo.ESTUDIANTES));
            agregar(menu, contenido, "Categorías", new GestionVista(biblioteca, GestionVista.Tipo.CATEGORIAS));
        }
        agregar(menu, contenido, "Préstamos y devoluciones", new PrestamosVista(biblioteca, prestamos));
        agregar(menu, contenido, "Reportes", new ReportesVista(biblioteca, prestamos));
        add(menu, BorderLayout.NORTH); add(contenido, BorderLayout.CENTER);
        setSize(1100, 700); setMinimumSize(new Dimension(850, 550)); setLocationRelativeTo(null);
    }

    private void agregar(JPanel menu, JPanel contenido, String nombre, JPanel vista) {
        contenido.add(vista, nombre);
        JButton boton = new JButton(nombre); menu.add(boton);
        boton.addActionListener(e -> {
            ((CardLayout) contenido.getLayout()).show(contenido, nombre);
            if (vista instanceof GestionVista gestion) gestion.cargar();
            else if (vista instanceof PrestamosVista prestamos) prestamos.cargar();
            else if (vista instanceof ReportesVista reportes) reportes.cargarEstudiantes();
        });
    }
}
