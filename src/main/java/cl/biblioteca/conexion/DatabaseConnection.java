package cl.biblioteca.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Administrador Singleton. Cada operación debe cerrar su propia conexión. */
public final class DatabaseConnection {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    /** No comparte una conexión mutable entre los hilos de la aplicación. */
    public Connection getConnection() throws SQLException {
        String usuario = System.getenv("BIBLIOTECA_DB_USER");
        String password = System.getenv("BIBLIOTECA_DB_PASSWORD");
        Path archivo = Path.of("config-local.properties");
        if ((usuario == null || password == null) && Files.exists(archivo)) {
            Properties local = new Properties();
            try (InputStream entrada = Files.newInputStream(archivo)) {
                local.load(entrada);
            } catch (IOException e) {
                throw new SQLException("No se pudo leer config-local.properties.", e);
            }
            if (usuario == null) usuario = local.getProperty("db.user");
            if (password == null) password = local.getProperty("db.password");
        }
        if (usuario == null || usuario.isBlank() || password == null) {
            throw new SQLException("Configure BIBLIOTECA_DB_USER y BIBLIOTECA_DB_PASSWORD.");
        }
        Properties propiedades = new Properties();
        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", password);
        propiedades.setProperty("connectTimeout", "5000");
        propiedades.setProperty("socketTimeout", "15000");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/biblioteca", propiedades);
    }
}
