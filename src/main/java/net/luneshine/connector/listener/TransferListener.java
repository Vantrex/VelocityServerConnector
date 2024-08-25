package net.luneshine.connector.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.luneshine.connector.Config;
import net.luneshine.connector.concurrency.LockConditionPair;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Listener to handle automatic player transfers between two proxy servers
 */
@SuppressWarnings("PatternValidation") // Suppresses the warning about the pattern not being validated. We do validate the pattern in the Config class.
public class TransferListener {

    private final Set<UUID> redirections = ConcurrentHashMap.newKeySet();
    private final Map<UUID, LockConditionPair> locks = new ConcurrentHashMap<>();
    private final Config config;
    private final Logger logger;

    public TransferListener(Config config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * When a player logs in, we check if they have stored the cookie. If they have not, we redirect them to the
     *
     * @param event the login event
     * @param continuation the continuation to halt the login process until the server has received a cookie response
     */
    @Subscribe
    public void onServerConnect(LoginEvent event, Continuation continuation) {
        final Player player = event.getPlayer();
        if (!config.isDirectConnect()) {
            continuation.resume();
            return;
        }
        // If the player is on a minecraft version below 1.20.5, we can't use the transfer-packet
        if (player.getProtocolVersion().getProtocol() < 767) {
            continuation.resume();
            return;
        }
        player.requestCookie(Key.key(config.getCookieName()));
        // We need to wait for the cookie to be received before we can continue
        final ReentrantLock lock = new ReentrantLock(true);
        Condition condition = lock.newCondition();
        this.locks.put(player.getUniqueId(), new LockConditionPair(lock, condition));
        lock.lock();
        try {
            condition.await();
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for the cookie", e);
        } finally {
            lock.unlock();
        }
        continuation.resume();
    }

    /**
     * When a player is about to connect to a server, we check if they´re in the redirections set. If they are, we redirect them to the target server.
     * We need to do that here since it does not work in the LoginEvent.
     *
     * @param event the server pre connect event
     */
    @Subscribe
    public void onServerConnect(ServerPreConnectEvent event) {
        // If the player is coming from another backend server, we don't want to redirect them
        // The redirection should only happen on the first login (which tbh is actually already handled by the LoginEvent)
        if (event.getPreviousServer() != null) {
            return;
        }
        final Player player = event.getPlayer();
        if (!redirections.remove(player.getUniqueId())) {
            if (player.getProtocolVersion().getProtocol() < 767) {
                player.storeCookie(Key.key(config.getCookieName()), new byte[0]); // Empty cookie
            }
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        transferPlayer(player);
    }

    /**
     * When a player receives a cookie, we check if the redirection-cookie is set. If it is not, we add the player to the redirections set.
     *
     * @param event the cookie receive event
     */
    @Subscribe
    public void onCookie(CookieReceiveEvent event) {
        final Player player = event.getPlayer();
        if (!event.getResult().isAllowed()) {
            addRedirection(player);
            return;
        }
        final byte[] cookie = event.getOriginalData();
        if (cookie == null || cookie.length != 1 || cookie[0] != 0x01) {
            addRedirection(player);
            return;
        }
        unlockLock(player);
    }

    /**
     * Transfers a player to the redirecting server
     *
     * @param player the player to transfer
     */
    private void transferPlayer(Player player) {
        player.storeCookie(Key.key(config.getCookieName()), new byte[]{0x01});
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        player.transferToHost(address);
    }


    private void addRedirection(final Player player) {
        redirections.add(player.getUniqueId());
        unlockLock(player);
    }

    private void unlockLock(final Player player) {
        LockConditionPair pair = this.locks.remove(player.getUniqueId());
        if (pair == null) {
            return;
        }
        pair.lock().lock();
        try {
            pair.condition().signal();
        } finally {
            pair.lock().unlock();
        }
    }
}