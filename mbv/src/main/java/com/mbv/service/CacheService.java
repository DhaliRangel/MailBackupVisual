package com.mbv.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

@Service
public class CacheService {

    private static final String DB_NAME = "mbv-cache.db";
    private Connection connection;

    private void ensureConnected() {
        if (connection == null) {
            initialize();
        }
    }

    public void initialize() {
        if (connection != null) return;
        try {
            Path dbPath = Paths.get(DB_NAME);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
        } catch (SQLException e) {
            System.err.println("Failed to initialize cache database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS config (
                key TEXT PRIMARY KEY,
                value TEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS import_cache (
                file_path TEXT PRIMARY KEY,
                file_hash TEXT,
                imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                message_count INTEGER DEFAULT 0,
                index_version INTEGER DEFAULT 1
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS search_cache (
                query_hash TEXT PRIMARY KEY,
                result_count INTEGER,
                cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id TEXT,
                label TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        stmt.close();
    }

    public String getConfig(String key) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT value FROM config WHERE key = ?");
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setConfig(String key, String value) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO config (key, value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)");
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getAllConfig() {
        ensureConnected();
        Map<String, String> config = new HashMap<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT key, value FROM config");
            while (rs.next()) {
                config.put(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return config;
    }

    public boolean isFileImported(String filePath, String fileHash) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT file_hash FROM import_cache WHERE file_path = ?");
            ps.setString(1, filePath);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("file_hash").equals(fileHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void cacheImportedFile(String filePath, String fileHash, int messageCount) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO import_cache (file_path, file_hash, message_count, imported_at) " +
                            "VALUES (?, ?, ?, CURRENT_TIMESTAMP)");
            ps.setString(1, filePath);
            ps.setString(2, fileHash);
            ps.setInt(3, messageCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addFavorite(String messageId, String label) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO favorites (message_id, label) VALUES (?, ?)");
            ps.setString(1, messageId);
            ps.setString(2, label);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFavorite(String messageId) {
        ensureConnected();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM favorites WHERE message_id = ?");
            ps.setString(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFavoriteIds() {
        ensureConnected();
        List<String> ids = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT message_id FROM favorites");
            while (rs.next()) {
                ids.add(rs.getString("message_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public String getTheme() {
        String theme = getConfig("theme");
        return theme != null ? theme : "blue";
    }

    public void setTheme(String theme) {
        setConfig("theme", theme);
    }

    public String getLastImportPath() {
        return getConfig("last_import_path");
    }

    public void setLastImportPath(String path) {
        setConfig("last_import_path", path);
    }

    public int getIndexVersion() {
        String v = getConfig("index_version");
        return v != null ? Integer.parseInt(v) : 0;
    }

    public void setIndexVersion(int version) {
        setConfig("index_version", String.valueOf(version));
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
