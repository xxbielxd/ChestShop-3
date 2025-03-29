package com.Acrobot.ChestShop.Database;

import com.Acrobot.ChestShop.ChestShop;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.LruObjectCache;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.SqliteDatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import com.Acrobot.ChestShop.Configuration.Properties;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.j256.ormlite.table.DatabaseTable;
/**
 * 
 * 
 * Creates a DAO appropriate for the plugin
 *
 * @author Andrzej Pomirski
 */
public class DaoCreator {

    /**
     * Returns a DAO for the given entity and with the given ID
     * @param entity Entity's class
     * @param <ENTITY> Type of the entity
     * @return Dao
     * @throws InvalidParameterException
     * @throws SQLException
     */
    public static <ENTITY, ID> Dao<ENTITY, ID> getDao(Class<ENTITY> entity) throws SQLException {
        DatabaseType dbType;
        String uri;

        if (Properties.DATABASE_TYPE.equalsIgnoreCase("mysql")) {
            dbType = new MysqlDatabaseType();

            // Constrói a URI do MySQL com base nos dados do config.yml
            uri = "jdbc:mysql://" + Properties.MYSQL_HOST + ":" + Properties.MYSQL_PORT + "/" + Properties.MYSQL_DATABASE +
                    "?user=" + Properties.MYSQL_USERNAME +
                    "&password=" + Properties.MYSQL_PASSWORD +
                    "&useSSL=false&autoReconnect=true";
        } else {
            dbType = new SqliteDatabaseType();

            // Usa SQLite como fallback (ainda opcional)
            uri = "jdbc:sqlite:" + ChestShop.loadFile("database.db").getAbsolutePath();
        }

        ConnectionSource connectionSource = new JdbcConnectionSource(uri, dbType);

        Dao<ENTITY, ID> dao = DaoManager.createDao(connectionSource, entity);
        dao.setObjectCache(new LruObjectCache(200));

        return dao;
    }


    /**
     * Creates a dao as well as a default table, if doesn't exist
     * @see #getDao(Class)
     * @throws SQLException
     * @throws InvalidParameterException
     */
    public static <ENTITY, ID> Dao<ENTITY, ID> getDaoAndCreateTable(Class<ENTITY> entity) throws SQLException, InvalidParameterException {
        Dao<ENTITY, ID> dao = getDao(entity);

        TableUtils.createTableIfNotExists(dao.getConnectionSource(), entity);

        return dao;
    }

    public static <ENTITY, ID> Dao<ENTITY, ID> getSafeDao(Class<ENTITY> entity, String indexName) throws SQLException {
        Dao<ENTITY, ID> dao = getDao(entity);
        String tableName = getTableName(entity);

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet result = null;

        try {
            connection = (Connection) dao.getConnectionSource()
                    .getReadWriteConnection(tableName)
                    .getUnderlyingConnection();

            // Verifica se a tabela existe antes de tentar criar
            boolean tableExists = false;
            String checkTableSQL = Properties.DATABASE_TYPE.equalsIgnoreCase("sqlite")
                    ? "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
                    : "SHOW TABLES LIKE ?";

            stmt = connection.prepareStatement(checkTableSQL);
            stmt.setString(1, tableName);
            result = stmt.executeQuery();
            tableExists = result.next();

            if (!tableExists) {
                ChestShop.getBukkitLogger().info("Tabela '" + tableName + "' não encontrada. Criando agora...");
                TableUtils.createTable(dao.getConnectionSource(), entity);
            } else {
                ChestShop.getBukkitLogger().info("Tabela '" + tableName + "' já existe.");
            }

            // Para SQLite, não precisa checar índice
            if (!Properties.DATABASE_TYPE.equalsIgnoreCase("sqlite")) {
                try (PreparedStatement stmtIndex = connection.prepareStatement("SHOW INDEX FROM `" + tableName + "` WHERE Key_name = ?")) {
                    stmtIndex.setString(1, indexName);
                    try (ResultSet rsIndex = stmtIndex.executeQuery()) {
                        if (rsIndex.next()) {
                            ChestShop.getBukkitLogger().info("Índice '" + indexName + "' já existe em '" + tableName + "'.");
                        } else {
                            ChestShop.getBukkitLogger().info("Criando índice '" + indexName + "' em '" + tableName + "'.");
                            dao.executeRaw("CREATE INDEX `" + indexName + "` ON `" + tableName + "` (`" + indexName.replace(tableName + "_", "") + "`)");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            ChestShop.getBukkitLogger().log(Level.SEVERE, "Erro ao verificar/criar tabela ou índice em '" + tableName + "'", e);
            throw e;
        } finally {
            if (result != null) try { result.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
        }

        return dao;
    }


    private static String getTableName(Class<?> clazz) {
        DatabaseTable annotation = clazz.getAnnotation(DatabaseTable.class);
        if (annotation != null && !annotation.tableName().isEmpty()) {
            return annotation.tableName();
        }
        return clazz.getSimpleName();
    }

}
