package net.luneshine.connector;

import com.moandjiezana.toml.Toml;
import org.intellij.lang.annotations.Subst;

import java.io.File;

/**
 * Configuration class for the connector plugin
 */
public class Config {

    private final boolean directConnect;
    private final String host;
    private final int port;
    private final String cookieName;
    private final String bungeeChannelName;

    /**
     * Creates a new configuration object from the given file
     *
     * @param file the configuration file
     */
    Config(File file) {
        Toml toml = new Toml().read(file);
        this.directConnect = toml.getBoolean("direct-connect");
        final String directConnectServerAddress = toml.getString("direct-connect-server-address");
        if (directConnectServerAddress != null) {
            final String[] split = directConnectServerAddress.split(":");
            this.host = split[0];
            this.port = Integer.parseInt(split[1]);
            this.cookieName = toml.getString("cookie-name");
            this.bungeeChannelName = toml.getString("bungee-channel-name");
        } else {
            this.host = null;
            this.port = 0;
            this.cookieName = "luneshine:transfer";
            this.bungeeChannelName = "luneshine:transfer:manual";
        }
        if (!this.cookieName.matches("(?:([a-z0-9_\\-.]+:)?|:)[a-z0-9_\\-./]+")) {
            throw new IllegalArgumentException("Invalid cookie name: " + this.cookieName
                    + ". The cookie name must match the pattern: (?:([a-z0-9_\\-.]+:)?|:)[a-z0-9_\\-./]+");
        }
    }

    /**
     * Returns whether the plugin should directly connect players to the target server
     *
     * @return whether the plugin should directly connect players to the target server
     */
    public boolean isDirectConnect() {
        return directConnect;
    }

    /**
     * Returns the host of the target server
     *
     * @return the host of the target server
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port of the target server
     *
     * @return the port of the target server
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the name of the cookie used to store the redirection information
     *
     * @return the name of the cookie used to store the redirection information
     */
    @Subst("")
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Returns the name of the BungeeCord channel used for manual transfers
     *
     * @return the name of the BungeeCord channel used for manual transfers
     */
    public String getBungeeChannelName() {
        return bungeeChannelName;
    }
}