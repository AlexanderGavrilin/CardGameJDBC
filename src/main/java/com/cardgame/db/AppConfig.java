package com.cardgame.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загружает application.properties из classpath и даёт типобезопасный доступ
 * к настройкам приложения.
 */
public final class AppConfig {

    private static final String PROPERTIES_FILE = "application.properties";
    private static final Properties PROPS = load();

    private AppConfig() {}

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new IllegalStateException("Файл " + PROPERTIES_FILE + " не найден в classpath!");
            }
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения " + PROPERTIES_FILE, e);
        }
        return props;
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String def) {
        return PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        String v = PROPS.getProperty(key);
        return v == null ? def : Integer.parseInt(v.trim());
    }

    public static long getLong(String key, long def) {
        String v = PROPS.getProperty(key);
        return v == null ? def : Long.parseLong(v.trim());
    }

    public static boolean getBoolean(String key, boolean def) {
        String v = PROPS.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
}
