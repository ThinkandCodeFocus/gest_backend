package com.thinkcode.transportbackend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final String datasourceUrl;

    public DatabaseBackupService(
            DataSource dataSource,
            @Value("${spring.datasource.url}") String datasourceUrl
    ) {
        this.dataSource = dataSource;
        this.datasourceUrl = datasourceUrl;
    }

    public DatabaseBackup exportSqlDump() {
        JdbcTarget target = parseJdbcTarget(datasourceUrl);
        try (Connection connection = dataSource.getConnection()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                dumpDatabase(connection, writer, target.databaseName());
                writer.flush();
            }
            String fileName = "backup-" + target.databaseName() + "-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".sql";
            return new DatabaseBackup(fileName, outputStream.toByteArray());
        } catch (SQLException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Export SQL impossible: " + exception.getMessage());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Export SQL impossible: ecriture du fichier interrompue.");
        }
    }

    private JdbcTarget parseJdbcTarget(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "URL datasource manquante.");
        }
        String normalized = jdbcUrl.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("jdbc:mysql://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Export SQL supporte uniquement pour MySQL.");
        }

        String raw = jdbcUrl.substring("jdbc:".length());
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "URL datasource invalide.");
        }

        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
        String path = uri.getPath();
        String database = path != null && path.length() > 1 ? path.substring(1) : "";

        if (host == null || host.isBlank() || database.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de determiner l'hote ou la base MySQL.");
        }

        return new JdbcTarget(host, port, database);
    }

    private void dumpDatabase(Connection connection, OutputStreamWriter writer, String databaseName) throws SQLException, IOException {
        writer.write("-- Database backup generated on ");
        writer.write(LocalDateTime.now().toString());
        writer.write(System.lineSeparator());
        writer.write("SET NAMES utf8mb4;\n");
        writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + escapeSqlLiteral(databaseName) + "' AND table_type = 'BASE TABLE' ORDER BY table_name")) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }

        for (String table : tables) {
            dumpTable(connection, writer, table);
            writer.write("\n");
        }

        writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
    }

    private void dumpTable(Connection connection, OutputStreamWriter writer, String tableName) throws SQLException, IOException {
        writer.write("DROP TABLE IF EXISTS `");
        writer.write(tableName);
        writer.write("`;\n");

        try (Statement statement = connection.createStatement();
             ResultSet createResult = statement.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
            if (createResult.next()) {
                String createSql = createResult.getString(2);
                writer.write(createSql);
                writer.write(";\n\n");
            }
        }

        try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM `" + tableName + "`")) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    writer.write("INSERT INTO `");
                    writer.write(tableName);
                    writer.write("` (");
                    for (int index = 1; index <= columnCount; index++) {
                        if (index > 1) writer.write(", ");
                        writer.write("`");
                        writer.write(metaData.getColumnLabel(index));
                        writer.write("`");
                    }
                    writer.write(") VALUES (");
                    for (int index = 1; index <= columnCount; index++) {
                        if (index > 1) writer.write(", ");
                        writer.write(toSqlLiteral(resultSet.getObject(index)));
                    }
                    writer.write(");\n");
                }
            }
        }
    }

    private String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return "'" + localDateTime.toString().replace("'", "''") + "'";
        }
        if (value instanceof java.time.LocalDate localDate) {
            return "'" + localDate.toString().replace("'", "''") + "'";
        }
        if (value instanceof Timestamp timestamp) {
            return "'" + timestamp.toLocalDateTime().toString().replace("'", "''") + "'";
        }
        return "'" + value.toString().replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private record JdbcTarget(String host, int port, String databaseName) {
    }

    public record DatabaseBackup(String fileName, byte[] content) {
    }
}