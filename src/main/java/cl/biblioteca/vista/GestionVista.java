package cl.biblioteca.vista;

import cl.biblioteca.controlador.BibliotecaControlador;
import cl.biblioteca.modelo.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

/** Formulario sencillo compartido por los tres CRUD. No contiene SQL. */
public class GestionVista extends JPanel {
    public enum Tipo { LIBROS, ESTUDIANTES, CATEGORIAS }
    private final BibliotecaControlador controlador;
    private final Tipo tipo;
    private final JTable tabla = new JTable();
    private final List<JTextField> campos = new ArrayList<>();
    private final JComboBox<Categoria> categoria = new JComboBox<>();
    private final JButton guardar = new JButton("Guardar");
    private final JButton eliminar = new JButton("Eliminar seleccionado");
    private final JButton nuevo = new JButton("Nuevo");
    private final JButton actualizar = new JButton("Actualizar lista");
    private List<?> registros = List.of();
    private int idSeleccionado;
    private boolean ocupada;
    private final boolean administrador;

    public GestionVista(BibliotecaControlador controlador, Tipo tipo) {
        super(new BorderLayout(10, 10));
        this.controlador = controlador; this.tipo = tipo;
        administrador = controlador.getUsuario().esBibliotecario();
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        String[] etiquetas = switch (tipo) {
            case LIBROS -> new String[]{"Título", "Autor", "ISBN", "Editorial", "Stock"};
            case ESTUDIANTES -> new String[]{"Nombre", "RUT", "Curso", "Correo"};
            case CATEGORIAS -> new String[]{"Nombre"};
        };
        for (String etiqueta : etiquetas) {
            JTextField campo = new JTextField(20); campos.add(campo);
            formulario.add(new JLabel(etiqueta)); formulario.add(campo);
        }
        if (tipo == Tipo.LIBROS) { formulario.add(new JLabel("Categoría")); formulario.add(categoria); }
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (administrador) { botones.add(nuevo); botones.add(guardar); botones.add(eliminar); }
        botones.add(actualizar);
        JPanel inferior = new JPanel(new BorderLayout());
        if (administrador) inferior.add(formulario, BorderLayout.CENTER);
        inferior.add(botones, BorderLayout.SOUTH); add(inferior, BorderLayout.SOUTH);
        nuevo.addActionListener(e -> limpiar());
        actualizar.addActionListener(e -> cargar());
        guardar.addActionListener(e -> guardar());
        eliminar.addActionListener(e -> eliminar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionar();
        });
        cargar();
    }

    public void cargar() {
        if (ocupada) return;
        ocupado(true);
        TareasSwing.ejecutar(this, actualizar, () -> {
            List<?> lista = switch (tipo) {
                case LIBROS -> controlador.libros();
                case ESTUDIANTES -> controlador.estudiantes();
                case CATEGORIAS -> controlador.categorias();
            };
            List<Categoria> categorias = tipo == Tipo.LIBROS ? controlador.categorias() : List.of();
            return new Datos(lista, categorias);
        }, datos -> {
            registros = datos.registros();
            categoria.removeAllItems(); datos.categorias().forEach(categoria::addItem);
            String[] columnas = switch (tipo) {
                case LIBROS -> new String[]{"ID", "Título", "Autor", "ISBN", "Editorial", "Stock", "Categoría"};
                case ESTUDIANTES -> new String[]{"ID", "Nombre", "RUT", "Curso", "Correo"};
                case CATEGORIAS -> new String[]{"ID", "Nombre"};
            };
            DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Object registro : registros) {
                if (registro instanceof Libro l) {
                    String nombreCategoria = datos.categorias().stream().filter(c -> c.getId() == l.getIdCategoria())
                            .map(Categoria::getNombre).findFirst().orElse("");
                    modelo.addRow(new Object[]{l.getId(), l.getTitulo(), l.getAutor(), l.getIsbn(), l.getEditorial(), l.getStock(), nombreCategoria});
                } else if (registro instanceof Estudiante s) {
                    modelo.addRow(new Object[]{s.getId(), s.getNombre(), s.getRut(), s.getCurso(), s.getCorreo()});
                } else if (registro instanceof Categoria c) modelo.addRow(new Object[]{c.getId(), c.getNombre()});
            }
            tabla.setModel(modelo); limpiar();
        }, () -> ocupado(false));
        // Si la consulta falla, el botón Actualizar permite volver a intentarla.
    }

    private void ocupado(boolean valor) {
        ocupada = valor;
        actualizar.setEnabled(!valor); tabla.setEnabled(!valor);
        guardar.setEnabled(!valor); eliminar.setEnabled(!valor); nuevo.setEnabled(!valor);
        campos.forEach(c -> c.setEnabled(!valor)); categoria.setEnabled(!valor);
    }

    private String campo(int i) { return campos.get(i).getText().trim(); }

    private void guardar() {
        if (ocupada) return;
        try {
            Object entidad;
            if (tipo == Tipo.LIBROS) {
                Libro l = new Libro(); l.setId(idSeleccionado); l.setTitulo(campo(0)); l.setAutor(campo(1));
                l.setIsbn(campo(2)); l.setEditorial(campo(3)); l.setStock(Integer.parseInt(campo(4)));
                Categoria c = (Categoria) categoria.getSelectedItem(); l.setIdCategoria(c == null ? 0 : c.getId()); entidad = l;
            } else if (tipo == Tipo.ESTUDIANTES) {
                Estudiante s = new Estudiante(); s.setId(idSeleccionado); s.setNombre(campo(0)); s.setRut(campo(1));
                s.setCurso(campo(2)); s.setCorreo(campo(3)); entidad = s;
            } else {
                Categoria c = new Categoria(); c.setId(idSeleccionado); c.setNombre(campo(0)); entidad = c;
            }
            ocupado(true);
            TareasSwing.ejecutar(this, null, () -> {
                if (entidad instanceof Libro l) controlador.guardar(l);
                else if (entidad instanceof Estudiante s) controlador.guardar(s);
                else controlador.guardar((Categoria) entidad);
                return null;
            }, resultado -> { }, () -> { ocupado(false); cargar(); });
        } catch (NumberFormatException e) {
            TareasSwing.error(this, new IllegalArgumentException("Stock debe ser un número entero."));
        }
    }

    private void eliminar() {
        if (ocupada) return;
        final int id = idSeleccionado;
        if (id == 0) { TareasSwing.error(this, new IllegalArgumentException("Seleccione un registro.")); return; }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar el registro seleccionado?", "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ocupado(true);
        TareasSwing.ejecutar(this, null, () -> {
            switch (tipo) {
                case LIBROS -> controlador.eliminarLibro(id);
                case ESTUDIANTES -> controlador.eliminarEstudiante(id);
                case CATEGORIAS -> controlador.eliminarCategoria(id);
            }
            return null;
        }, resultado -> { }, () -> { ocupado(false); cargar(); });
    }

    private void seleccionar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= registros.size()) return;
        Object registro = registros.get(fila);
        if (registro instanceof Libro l) {
            idSeleccionado = l.getId();
            llenar(l.getTitulo(), l.getAutor(), l.getIsbn(), l.getEditorial(), String.valueOf(l.getStock()));
            for (int i = 0; i < categoria.getItemCount(); i++) {
                if (categoria.getItemAt(i).getId() == l.getIdCategoria()) categoria.setSelectedIndex(i);
            }
        } else if (registro instanceof Estudiante s) {
            idSeleccionado = s.getId(); llenar(s.getNombre(), s.getRut(), s.getCurso(), s.getCorreo());
        } else if (registro instanceof Categoria c) { idSeleccionado = c.getId(); llenar(c.getNombre()); }
    }

    private void llenar(String... valores) { for (int i = 0; i < valores.length; i++) campos.get(i).setText(valores[i]); }
    private void limpiar() { idSeleccionado = 0; tabla.clearSelection(); campos.forEach(c -> c.setText("")); }
    private record Datos(List<?> registros, List<Categoria> categorias) { }
}
