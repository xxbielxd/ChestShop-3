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

/**
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
}
