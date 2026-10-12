package cl.biblioteca.vista;

import cl.biblioteca.controlador.BibliotecaControlador;
import cl.biblioteca.controlador.PrestamoControlador;
import cl.biblioteca.modelo.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PrestamosVista extends JPanel {
    private final BibliotecaControlador biblioteca;
    private final PrestamoControlador controlador;
    private final JComboBox<Estudiante> estudiantes = new JComboBox<>();
    private final JComboBox<Libro> libros = new JComboBox<>();
    private final JButton prestar = new JButton("Registrar préstamo");
    private final JButton devolver = new JButton("Devolver seleccionado");
    private final JButton actualizar = new JButton("Actualizar");
    private boolean ocupada;
    private final JTable tabla = new JTable();
    public static final String[] COLUMNAS = {"ID", "Estudiante", "Libro", "Fecha préstamo", "Vencimiento", "Estado"};

    public PrestamosVista(BibliotecaControlador biblioteca, PrestamoControlador controlador) {
        super(new BorderLayout(10, 10));
        this.biblioteca = biblioteca; this.controlador = controlador;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        campos.add(new JLabel("Estudiante:")); campos.add(estudiantes);
        campos.add(new JLabel("Libro:")); campos.add(libros);
        campos.add(new JLabel("Plazo: " + PrestamoControlador.PLAZO_DIAS + " días desde hoy")); campos.add(prestar);
        add(campos, BorderLayout.NORTH); add(new JScrollPane(tabla), BorderLayout.CENTER);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(devolver); botones.add(actualizar); add(botones, BorderLayout.SOUTH);
        actualizar.addActionListener(e -> cargar());
        prestar.addActionListener(e -> prestar()); devolver.addActionListener(e -> devolver());
        cargar();
    }

    private void ocupado(boolean valor) {
        ocupada = valor;
        prestar.setEnabled(!valor); devolver.setEnabled(!valor); actualizar.setEnabled(!valor);
        estudiantes.setEnabled(!valor && biblioteca.getUsuario().esBibliotecario()); libros.setEnabled(!valor);
    }

    public void cargar() {
        if (ocupada) return;
        ocupado(true);
        TareasSwing.ejecutar(this, null, () -> new Datos(biblioteca.estudiantes(), biblioteca.libros(), controlador.activos()), datos -> {
            estudiantes.removeAllItems(); datos.estudiantes().forEach(estudiantes::addItem);
            libros.removeAllItems(); datos.libros().forEach(libros::addItem);
            mostrar(tabla, COLUMNAS, datos.prestamos());
        }, () -> ocupado(false));
    }

    private void prestar() {
        Estudiante estudiante = (Estudiante) estudiantes.getSelectedItem();
        Libro libro = (Libro) libros.getSelectedItem();
        if (estudiante == null || libro == null) {
            TareasSwing.error(this, new IllegalArgumentException("Seleccione estudiante y libro.")); return;
        }
        ocupado(true);
        TareasSwing.ejecutar(this, null, () -> {
            controlador.prestar(estudiante.getId(), libro.getId()); return null;
        }, resultado -> JOptionPane.showMessageDialog(this, "Préstamo registrado."), () -> { ocupado(false); cargar(); });
    }

    private void devolver() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { TareasSwing.error(this, new IllegalArgumentException("Seleccione un préstamo.")); return; }
        int id = ((Number) tabla.getValueAt(fila, 0)).intValue();
        ocupado(true);
        TareasSwing.ejecutar(this, null, () -> { controlador.devolver(id); return null; },
                resultado -> JOptionPane.showMessageDialog(this, "Devolución registrada."), () -> { ocupado(false); cargar(); });
    }

    public static void mostrar(JTable tabla, String[] columnas, List<Object[]> filas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        filas.forEach(modelo::addRow); tabla.setModel(modelo);
    }

    private record Datos(List<Estudiante> estudiantes, List<Libro> libros, List<Object[]> prestamos) { }
}
