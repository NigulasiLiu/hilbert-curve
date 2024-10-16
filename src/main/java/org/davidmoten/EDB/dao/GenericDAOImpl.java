package org.davidmoten.EDB.dao;

import org.davidmoten.EDB.config.DatabaseConfig;
import org.davidmoten.EDB.model.Entity;

import java.math.BigInteger;
import java.sql.*;

public class GenericDAOImpl<T extends Entity> {

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getDataSource().getConnection();
    }

    // 如果你的列是 BLOB 类型，用 setBytes 存储二进制数据
    public void insertOrUpdate(Entity entity, String tableName) throws SQLException {
        String query = "";

        // 处理 PDB 和 KDB 表
        if (tableName.equals("PDB") || tableName.equals("KDB")) {
            query = "INSERT INTO " + tableName + " (keyword, C, ea, eb) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE C=VALUES(C), ea=VALUES(ea), eb=VALUES(eb)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, entity.getKeyword());
                stmt.setBytes(2, (byte[]) entity.getCiphertext()[0]);  // C 是 byte[]
                stmt.setBytes(3, ((BigInteger) entity.getCiphertext()[1]).toByteArray());  // ea 是 BigInteger
                stmt.setBytes(4, ((BigInteger) entity.getCiphertext()[2]).toByteArray());  // eb 是 BigInteger
                stmt.executeUpdate();
            }
        }

        // 处理 SC 表
        else if (tableName.equals("SC")) {
            query = "INSERT INTO " + tableName + " (keyword, c0, c, Rc) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE c0=VALUES(c0), c=VALUES(c), Rc=VALUES(Rc)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, entity.getKeyword());
                int[] state = entity.getState();
                stmt.setInt(2, state[0]);  // c0 是 int
                stmt.setInt(3, state[1]);  // c 是 int
                stmt.setInt(4, state[2]);  // Rc 是 int
                stmt.executeUpdate();
            }
        }

        // 处理 SS 表
        else if (tableName.equals("SS")) {
            query = "INSERT INTO " + tableName + " (keyword, value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE value=VALUES(value)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, entity.getKeyword());
                stmt.setBytes(2, entity.getBigIntegerValue().toByteArray());  // value 是 BigInteger
                stmt.executeUpdate();
            }
        }
    }

    // 获取数据
    public Entity get(String keyword, String tableName) throws SQLException {
        String query = "";

        // 获取 SC 表数据
        if (tableName.equals("SC")) {
            query = "SELECT c0, c, Rc FROM SC WHERE keyword = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, keyword);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int[] state = new int[]{rs.getInt("c0"), rs.getInt("c"), rs.getInt("Rc")};
                        return new Entity(keyword, state);
                    }
                }
            }
        }

        // 获取 SS 表数据
        else if (tableName.equals("SS")) {
            query = "SELECT value FROM SS WHERE keyword = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, keyword);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        BigInteger bigIntValue = new BigInteger(rs.getBytes("value"));
                        return new Entity(keyword, bigIntValue);
                    }
                }
            }
        }

        // 获取 PDB 或 KDB 表数据
        else if (tableName.equals("PDB") || tableName.equals("KDB")) {
            query = "SELECT C, ea, eb FROM " + tableName + " WHERE keyword = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, keyword);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        byte[] C = rs.getBytes("C");
                        BigInteger ea = new BigInteger(rs.getBytes("ea"));
                        BigInteger eb = new BigInteger(rs.getBytes("eb"));
                        Object[] ciphertext = new Object[]{C, ea, eb};
                        return new Entity(keyword, ciphertext);
                    }
                }
            }
        }
        return null; // 如果未找到
    }

    // 删除数据
    public void delete(String keyword, String tableName) throws SQLException {
        String query = "DELETE FROM " + tableName + " WHERE keyword = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, keyword);
            stmt.executeUpdate();
        }
    }

    // ... additional methods if needed ...
}
