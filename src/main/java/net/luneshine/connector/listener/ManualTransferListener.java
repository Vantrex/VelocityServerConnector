package net.luneshine.connector.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import net.kyori.adventure.key.Key;
import net.luneshine.connector.Config;

import java.net.InetSocketAddress;

/**
 * Listener to handle manual player transfers between two proxy servers
 */
@SuppressWarnings("PatternValidation") // Suppresses the warning about the pattern not being validated. We do validate the pattern in the Config class.
public class ManualTransferListener {

    private final Config config;

    public ManualTransferListener(Config config) {
        this.config = config;
    }

    /**
     * When a plugin message is received, we check if the message is a transfer message. If it is, we transfer the player to the target server.
     *
     * @param event the plugin message event
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        final ChannelMessageSource source = event.getSource();
        if (!(source instanceof Player player)) {
            return;
        }
        final ChannelIdentifier identifier = event.getIdentifier();
        if (!identifier.getId().equals("BungeeCord")) {
            return;
        }
        if (player.getProtocolVersion().getProtocol() < 767) {
            return;
        }
        final byte[] data = event.getData();
        if (data.length == 0) {
            return;
        }
        final String channel = new String(data, 0, data.length);
        if (!channel.equals(config.getBungeeChannelName())) {
            return;
        }
        final String server = new String(data, 17, data.length - 17);
        InetSocketAddress address = new InetSocketAddress(server, 25565);
        player.storeCookie(Key.key(config.getCookieName()), new byte[]{0x01});
        player.transferToHost(address);
    }

}
