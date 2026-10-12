package cl.biblioteca.dao;

import java.sql.SQLException;
import java.util.List;

/** Contrato compartido por las tres entidades administrables. */
public interface CrudDAO<T> {
    List<T> listar() throws SQLException;
    void crear(T entidad) throws SQLException;
    void actualizar(T entidad) throws SQLException;
    void eliminar(int id) throws SQLException;
}
