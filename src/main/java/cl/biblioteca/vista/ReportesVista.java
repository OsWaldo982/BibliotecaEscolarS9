package cl.biblioteca.vista;

import cl.biblioteca.controlador.BibliotecaControlador;
import cl.biblioteca.controlador.PrestamoControlador;
import cl.biblioteca.modelo.Estudiante;
import javax.swing.*;
import java.awt.*;

public class ReportesVista extends JPanel {
    private final BibliotecaControlador biblioteca;
    private boolean cargando;
    private final JComboBox<String> informe = new JComboBox<>(new String[]{
            "Libros más prestados", "Historial de préstamos por estudiante", "Libros actualmente en préstamo"});
    private final JComboBox<Estudiante> estudiantes = new JComboBox<>();
    private final JButton consultar = new JButton("Consultar");
    private final JTable tabla = new JTable();

    public ReportesVista(BibliotecaControlador biblioteca, PrestamoControlador controlador) {
        super(new BorderLayout(10, 10));
        this.biblioteca = biblioteca;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel opciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opciones.add(informe); opciones.add(estudiantes); opciones.add(consultar);
        add(opciones, BorderLayout.NORTH); add(new JScrollPane(tabla), BorderLayout.CENTER);
        estudiantes.setEnabled(false);
        informe.addActionListener(e -> estudiantes.setEnabled(informe.getSelectedIndex() == 1 && biblioteca.getUsuario().esBibliotecario()));
        consultar.addActionListener(e -> {
            int tipo = informe.getSelectedIndex();
            Estudiante estudiante = (Estudiante) estudiantes.getSelectedItem();
            if (tipo == 1 && estudiante == null) {
                TareasSwing.error(this, new IllegalArgumentException("Seleccione un estudiante.")); return;
            }
            TareasSwing.ejecutar(this, consultar, () -> switch (tipo) {
                case 0 -> controlador.masPrestados();
                case 1 -> controlador.historial(estudiante.getId());
                default -> controlador.activos();
            }, filas -> PrestamosVista.mostrar(tabla, tipo == 0 ? new String[]{"Libro", "Autor", "Cantidad de préstamos"} : PrestamosVista.COLUMNAS, filas));
        });
        cargarEstudiantes();
    }
    public void cargarEstudiantes() {
        if (cargando) return;
        cargando = true;
        Estudiante seleccionado = (Estudiante) estudiantes.getSelectedItem();
        TareasSwing.ejecutar(this, consultar, biblioteca::estudiantes, lista -> {
            estudiantes.removeAllItems();
            lista.forEach(estudiantes::addItem);
            if (seleccionado != null) {
                for (int i = 0; i < estudiantes.getItemCount(); i++) {
                    if (estudiantes.getItemAt(i).getId() == seleccionado.getId()) estudiantes.setSelectedIndex(i);
                }
            }
        }, () -> cargando = false);
    }
}
