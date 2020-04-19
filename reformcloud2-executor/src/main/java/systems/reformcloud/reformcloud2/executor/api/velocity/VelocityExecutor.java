package systems.reformcloud.reformcloud2.executor.api.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.reformcloud.reformcloud2.executor.api.ExecutorType;
import systems.reformcloud.reformcloud2.executor.api.api.API;
import systems.reformcloud.reformcloud2.executor.api.common.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.common.api.basic.ExternalEventBusHandler;
import systems.reformcloud.reformcloud2.executor.api.common.api.basic.events.ProcessUpdatedEvent;
import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.event.EventManager;
import systems.reformcloud.reformcloud2.executor.api.common.event.basic.DefaultEventManager;
import systems.reformcloud.reformcloud2.executor.api.common.event.handler.Listener;
import systems.reformcloud.reformcloud2.executor.api.common.groups.messages.IngameMessages;
import systems.reformcloud.reformcloud2.executor.api.common.groups.template.Version;
import systems.reformcloud.reformcloud2.executor.api.common.groups.utils.PlayerAccessConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.network.challenge.shared.ClientChallengeAuthHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.PacketSender;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.manager.DefaultChannelManager;
import systems.reformcloud.reformcloud2.executor.api.common.network.client.DefaultNetworkClient;
import systems.reformcloud.reformcloud2.executor.api.common.network.client.NetworkClient;
import systems.reformcloud.reformcloud2.executor.api.common.network.messaging.ProxiedChannelMessageHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.defaults.DefaultPacketHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.handler.PacketHandler;
import systems.reformcloud.reformcloud2.executor.api.common.process.ProcessInformation;
import systems.reformcloud.reformcloud2.executor.api.common.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.common.utility.system.SystemHelper;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;
import systems.reformcloud.reformcloud2.executor.api.common.utility.thread.AbsoluteThread;
import systems.reformcloud.reformcloud2.executor.api.executor.PlayerAPIExecutor;
import systems.reformcloud.reformcloud2.executor.api.network.channel.APINetworkChannelReader;
import systems.reformcloud.reformcloud2.executor.api.network.packets.in.APIPacketInAPIAction;
import systems.reformcloud.reformcloud2.executor.api.network.packets.in.APIPacketInPluginAction;
import systems.reformcloud.reformcloud2.executor.api.network.packets.out.APIBungeePacketOutRequestIngameMessages;
import systems.reformcloud.reformcloud2.executor.api.velocity.event.PlayerListenerHandler;
import systems.reformcloud.reformcloud2.executor.api.velocity.event.ProcessEventHandler;
import systems.reformcloud.reformcloud2.executor.api.velocity.plugins.PluginExecutorContainer;
import systems.reformcloud.reformcloud2.executor.api.velocity.plugins.PluginUpdater;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public final class VelocityExecutor extends API implements PlayerAPIExecutor {

    private static final List<ProcessInformation> LOBBY_SERVERS = new ArrayList<>();

    private static VelocityExecutor instance;

    private final ProxyServer proxyServer;

    private final PacketHandler packetHandler = new DefaultPacketHandler();

    private final NetworkClient networkClient = new DefaultNetworkClient();

    private IngameMessages messages = new IngameMessages();

    private ProcessInformation thisProcessInformation;

    VelocityExecutor(VelocityLauncher launcher, ProxyServer proxyServer) {
        super.type = ExecutorType.API;

        instance = this;
        this.proxyServer = proxyServer;

        new ExternalEventBusHandler(packetHandler, new DefaultEventManager());
        getEventManager().registerListener(new ProcessEventHandler());
        getEventManager().registerListener(this);
        proxyServer.getEventManager().register(launcher, new PlayerListenerHandler());

        packetHandler.registerHandler(new ProxiedChannelMessageHandler());
        packetHandler.registerHandler(new APIPacketInAPIAction(this));
        packetHandler.registerHandler(new APIPacketInPluginAction(new PluginExecutorContainer()));

        String connectionKey = JsonConfiguration.read("reformcloud/.connection/key.json").getString("key");
        SystemHelper.deleteFile(new File("reformcloud/.connection/key.json"));
        JsonConfiguration connectionConfig = JsonConfiguration.read("reformcloud/.connection/connection.json");

        this.thisProcessInformation = connectionConfig.get("startInfo", ProcessInformation.TYPE);
        if (thisProcessInformation == null) {
            System.exit(0);
            return;
        }

        this.networkClient.connect(
                connectionConfig.getString("controller-host"),
                connectionConfig.getInteger("controller-port"),
                () -> new APINetworkChannelReader(this.packetHandler),
                new ClientChallengeAuthHandler(
                        connectionKey,
                        thisProcessInformation.getProcessDetail().getName(),
                        () -> new JsonConfiguration(),
                        context -> {
                        } // unused here
                )
        );
        ExecutorAPI.setInstance(this);
        awaitConnectionAndUpdate();
    }

    NetworkClient getNetworkClient() {
        return networkClient;
    }

    @Override
    public PacketHandler packetHandler() {
        return packetHandler;
    }

    @NotNull
    @Override
    public EventManager getEventManager() {
        return ExternalEventBusHandler.getInstance().getEventManager();
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    @NotNull
    @Override
    public ProcessInformation getCurrentProcessInformation() {
        return this.thisProcessInformation;
    }

    @NotNull
    public static VelocityExecutor getInstance() {
        return instance;
    }

    public void handleProcessUpdate(ProcessInformation processInformation) {
        Optional<RegisteredServer> server = this.proxyServer.getServer(processInformation.getProcessDetail().getName());
        if (server.isPresent()) {
            if (server.get().getServerInfo().getAddress().getPort() == processInformation.getNetworkInfo().getPort()) {
                return;
            }

            handleProcessRemove(processInformation);
        }

        if (processInformation.getNetworkInfo().isConnected() && processInformation.getProcessDetail().getTemplate().getVersion().getId() == 1) {
            ServerInfo serverInfo = new ServerInfo(
                    processInformation.getProcessDetail().getName(),
                    processInformation.getNetworkInfo().toInet()
            );
            proxyServer.registerServer(serverInfo);
            if (processInformation.isLobby()) {
                LOBBY_SERVERS.add(processInformation);
            }
        }
    }

    public void handleProcessRemove(ProcessInformation processInformation) {
        proxyServer.getServer(processInformation.getProcessDetail().getName())
                .ifPresent(registeredServer -> proxyServer.unregisterServer(registeredServer.getServerInfo()));

        if (processInformation.isLobby()) {
            LOBBY_SERVERS.remove(processInformation);
        }
    }

    private boolean isServerRegistered(String name) {
        return proxyServer.getServer(name).isPresent();
    }

    private void awaitConnectionAndUpdate() {
        Task.EXECUTOR.execute(() -> {
            PacketSender packetSender = DefaultChannelManager.INSTANCE.get("Controller").orElse(null);
            while (packetSender == null) {
                packetSender = DefaultChannelManager.INSTANCE.get("Controller").orElse(null);
                AbsoluteThread.sleep(100);
            }

            getAllProcesses().forEach(this::handleProcessUpdate);

            new PluginUpdater();

            thisProcessInformation.updateMaxPlayers(proxyServer.getConfiguration().getShowMaxPlayers());
            thisProcessInformation.updateRuntimeInformation();
            thisProcessInformation.getNetworkInfo().setConnected(true);
            thisProcessInformation.getProcessDetail().setProcessState(thisProcessInformation.getProcessDetail().getInitialState());
            ExecutorAPI.getInstance().getSyncAPI().getProcessSyncAPI().update(thisProcessInformation);

            DefaultChannelManager.INSTANCE.get("Controller").ifPresent(controller -> packetHandler.getQueryHandler().sendQueryAsync(controller, new APIBungeePacketOutRequestIngameMessages()).onComplete(packet -> {
                IngameMessages ingameMessages = packet.content().get("messages", IngameMessages.TYPE);
                setMessages(ingameMessages);
            }));
        });
    }

    public static ProcessInformation getBestLobbyForPlayer(ProcessInformation current, Function<String, Boolean> permissionCheck,
                                                           @Nullable String excluded) {
        final List<ProcessInformation> lobbies = new ArrayList<>(LOBBY_SERVERS);

        // Filter all non java servers if this is a java proxy else all mcpe servers
        Streams.others(lobbies, e -> {
            Version version = e.getProcessDetail().getTemplate().getVersion();
            if (version.equals(Version.NUKKIT_X) && current.getProcessDetail().getTemplate().getVersion().equals(Version.WATERDOG_PE)) {
                return true;
            }

            return version.getId() == 1 && current.getProcessDetail().getTemplate().getVersion().getId() == 2;
        }).forEach(lobbies::remove);

        // Filter out all lobbies with join permission which the player does not have
        Streams.others(lobbies, e -> {
            final PlayerAccessConfiguration configuration = e.getProcessGroup().getPlayerAccessConfiguration();
            if (!configuration.isJoinOnlyPerPermission()) {
                return true;
            }

            return permissionCheck.apply(configuration.getJoinPermission());
        }).forEach(lobbies::remove);

        // Filter out all lobbies which are in maintenance and not joinable for the player
        Streams.others(lobbies, e -> {
            final PlayerAccessConfiguration configuration = e.getProcessGroup().getPlayerAccessConfiguration();
            if (!configuration.isMaintenance()) {
                return true;
            }

            return permissionCheck.apply(configuration.getMaintenanceJoinPermission());
        }).forEach(lobbies::remove);

        // Filter out all full server which the player cannot access
        Streams.others(lobbies, e -> {
            final PlayerAccessConfiguration configuration = e.getProcessGroup().getPlayerAccessConfiguration();
            if (!configuration.isUseCloudPlayerLimit()) {
                return true;
            }

            if (e.getProcessPlayerManager().getOnlineCount() < e.getProcessDetail().getMaxPlayers()) {
                return true;
            }

            return permissionCheck.apply("reformcloud.join.full");
        }).forEach(lobbies::remove);

        // Filter out all excluded servers
        if (excluded != null) {
            Streams.allOf(lobbies, e -> e.getProcessDetail().getName().equals(excluded)).forEach(lobbies::remove);
        }

        if (lobbies.isEmpty()) {
            return null;
        }

        if (lobbies.size() == 1) {
            return lobbies.get(0);
        }

        return lobbies.get(new Random().nextInt(lobbies.size()));
    }

    @Listener
    public void handle(final ProcessUpdatedEvent event) {
        if (event.getProcessInformation().getProcessDetail().getProcessUniqueID().equals(thisProcessInformation.getProcessDetail().getProcessUniqueID())) {
            thisProcessInformation = event.getProcessInformation();
        }
    }

    @Override
    @Deprecated
    public ProcessInformation getThisProcessInformation() {
        return getCurrentProcessInformation();
    }

    public IngameMessages getMessages() {
        return messages;
    }

    private void setMessages(IngameMessages messages) {
        this.messages = messages;
    }

    public void setThisProcessInformation(ProcessInformation thisProcessInformation) {
        this.thisProcessInformation = thisProcessInformation;
    }

    /* ======================== Player API ======================== */

    @Override
    public void executeSendMessage(UUID player, String message) {
        proxyServer.getPlayer(player).ifPresent(player1 -> player1.sendMessage(TextComponent.of(message)));
    }

    @Override
    public void executeKickPlayer(UUID player, String message) {
        proxyServer.getPlayer(player).ifPresent(player1 -> player1.disconnect(TextComponent.of(message)));
    }

    @Override
    public void executePlaySound(UUID player, String sound, float f1, float f2) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public void executeSendTitle(UUID player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public void executePlayEffect(UUID player, String entityEffect) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public <T> void executePlayEffect(UUID player, String effect, T data) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public void executeRespawn(UUID player) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public void executeTeleport(UUID player, String world, double x, double y, double z, float yaw, float pitch) {
        throw new UnsupportedOperationException("Not supported on velocity");
    }

    @Override
    public void executeConnect(UUID player, String server) {
        proxyServer.getPlayer(player).ifPresent(player1 -> proxyServer.getServer(server).ifPresent(e -> player1.createConnectionRequest(e).fireAndForget()));
    }

    @Override
    public void executeConnect(UUID player, ProcessInformation server) {
        executeConnect(player, server.getProcessDetail().getName());
    }

    @Override
    public void executeConnect(UUID player, UUID target) {
        proxyServer.getPlayer(player).ifPresent(player1 -> proxyServer.getPlayer(target).ifPresent(targetPlayer -> {
            if (targetPlayer.getCurrentServer().isPresent()
                    && isServerRegistered(targetPlayer.getCurrentServer().get().getServerInfo().getName())) {
                player1.createConnectionRequest(
                        targetPlayer.getCurrentServer().get().getServer()
                ).fireAndForget();
            }
        }));
    }

    @Override
    public void executeSetResourcePack(UUID player, String pack) {
        proxyServer.getPlayer(player).ifPresent(player1 -> player1.sendResourcePack(pack));
    }
}
