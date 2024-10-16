package org.davidmoten.EDB.dao;

import java.sql.SQLException;
import java.util.List;

public interface GenericDAO<T> {
    void insert(T entity, String tableName) throws SQLException;
    void update(T entity, String tableName) throws SQLException;
    void delete(String keyword, String tableName) throws SQLException;
    T get(String keyword, String tableName) throws SQLException;
    List<T> getAll(String tableName) throws SQLException;
}
