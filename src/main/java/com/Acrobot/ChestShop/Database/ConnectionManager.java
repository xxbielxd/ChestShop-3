package com.Acrobot.ChestShop.Database;

import com.Acrobot.ChestShop.Configuration.Properties;

import java.io.File;

public class ConnectionManager {

    public static String getURI(File databaseFile) {
        String type = Properties.DATABASE_TYPE;

        if ("mysql".equalsIgnoreCase(type)) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?user=%s&password=%s",
                    Properties.MYSQL_HOST,
                    Properties.MYSQL_PORT,
                    Properties.MYSQL_DATABASE,
                    Properties.MYSQL_USERNAME,
                    Properties.MYSQL_PASSWORD
            );
        }

        // Fallback para SQLite
        return String.format("jdbc:sqlite:%s", databaseFile.getAbsolutePath());
    }
}
