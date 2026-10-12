package cl.biblioteca.vista;

import javax.swing.*;
import java.awt.Component;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** JDBC en segundo plano; resultados y mensajes en el hilo de eventos de Swing. */
public final class TareasSwing {
    private TareasSwing() { }

    public static <T> void ejecutar(Component vista, AbstractButton boton,
                                    Callable<T> tarea, Consumer<T> terminado) {
        ejecutar(vista, boton, tarea, terminado, () -> { });
    }

    public static <T> void ejecutar(Component vista, AbstractButton boton,
                                    Callable<T> tarea, Consumer<T> terminado, Runnable finalizar) {
        if (boton != null) boton.setEnabled(false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarea.call(); }
            @Override protected void done() {
                try {
                    terminado.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    error(vista, e);
                } catch (ExecutionException e) {
                    error(vista, e.getCause());
                } catch (RuntimeException e) {
                    error(vista, e);
                } finally {
                    if (boton != null) boton.setEnabled(true);
                    finalizar.run();
                }
            }
        }.execute();
    }

    public static void error(Component vista, Throwable error) {
        String mensaje = error.getMessage();
        if (error instanceof SQLException sql) {
            if (sql.getErrorCode() == 1062) mensaje = "El ISBN o RUT ya está registrado.";
            else if (sql.getErrorCode() == 1451) mensaje = "No se puede eliminar: existen registros relacionados.";
            else if (sql.getErrorCode() == 1452) mensaje = "El estudiante, libro o categoría seleccionado ya no existe.";
        }
        JOptionPane.showMessageDialog(vista, mensaje == null ? "No se pudo completar la operación." : mensaje,
                "Biblioteca escolar", JOptionPane.ERROR_MESSAGE);
    }
}
