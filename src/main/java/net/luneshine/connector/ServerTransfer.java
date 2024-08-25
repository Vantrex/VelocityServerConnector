package net.luneshine.connector;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luneshine.connector.listener.ManualTransferListener;
import net.luneshine.connector.listener.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple Velocity plugin to transfer players between two proxy servers
 */
@Plugin(id = "luneshineconnector", name = "Lune Shine Connector", version = "0.1.0-SNAPSHOT",
        url = "https://luneshine.net", description = "Velocity plugin to transfer players between two proxy servers",
        authors = {"LuneShine"})
public class ServerTransfer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTransfer.class);
    private final ProxyServer server;
    private final Config config;

    @Inject
    public ServerTransfer(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        URL resource = ServerTransfer.this.getClass().getResource("/config.toml");
        if (resource == null) {
            throw new IllegalStateException("config.toml not found in resources");
        }
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                Files.copy(resource.openStream(), new File(dataDirectory.toFile(), "config.toml").toPath());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to copy config.toml to data directory", e);
        }
        this.config = new Config(dataDirectory.resolve("config.toml").toFile());
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new ManualTransferListener(config));
        server.getEventManager().register(this, new TransferListener(config, LOGGER));
    }
}