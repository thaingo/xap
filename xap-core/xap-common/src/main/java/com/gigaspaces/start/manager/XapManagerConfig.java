package com.gigaspaces.start.manager;

import com.gigaspaces.CommonSystemProperties;

import java.util.Map;
import java.util.Properties;

public class XapManagerConfig {
    // List of servers.
    // Each server has a host, and a map of component-to-port
    private final String host;

    private final Properties properties;

    private static final String DEFAULT_REST = System.getProperty(CommonSystemProperties.MANAGER_REST_PORT, "8090");
    private static final boolean SSL_ENABLED = Boolean.getBoolean(CommonSystemProperties.MANAGER_REST_SSL_ENABLED);

    public XapManagerConfig(String host) {
        this(host, new Properties());
    }

    public XapManagerConfig(String host, Properties properties) {
        System.out.println("XapManagerConfig:Constrctor - host =" + host);
        this.host = host;
        this.properties = properties;
    }

    public static XapManagerConfig parse(String s) {

        System.out.println("XapManagerConfig:parse input =" + s);
        final String[] tokens = s.split(";");
        final String host = tokens[0];
        final Properties properties = new Properties();
        for (int i=1 ; i < tokens.length ; i++) {
            String token = tokens[i];
            int pos = token.indexOf('=');
            if (pos == -1)
                throw new IllegalArgumentException("Invalid manager config '" + s + "' - element '" + token + "' does not contain '='");
            String key = token.substring(0, pos);
            String value = token.substring(pos+1);
            System.out.println("XapManagerConfig:parse key=" + key+", val="+value );
            properties.setProperty(key, value);
        }


        return new XapManagerConfig(host, properties);
    }

    public String getHost() {
        return host;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getZookeeper() {
        return properties.getProperty("zookeeper");
    }

    public String getLookupService() {
        return properties.getProperty("lus");
    }

    public String getAdminRest() {
        return properties.getProperty("rest", DEFAULT_REST);
    }

    public String getAdminRestUrl() {
        return (SSL_ENABLED ? "https" : "http") + "://" + getHost() + ":" + getAdminRest();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String toString() {
        String result = host;
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result += ";" + entry.getKey() + "=" + entry.getValue();
        }

        return result;
    }
}

