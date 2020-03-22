package systems.reformcloud.reformcloud2.proxy.application.command;

import systems.reformcloud.reformcloud2.executor.api.common.commands.basic.GlobalCommand;
import systems.reformcloud.reformcloud2.executor.api.common.commands.source.CommandSource;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.manager.DefaultChannelManager;
import systems.reformcloud.reformcloud2.proxy.application.ProxyApplication;
import systems.reformcloud.reformcloud2.proxy.application.network.PacketOutConfigUpdate;

import javax.annotation.Nonnull;

public class CommandProxy extends GlobalCommand {

    public CommandProxy() {
        super("proxy", "reformcloud.external.command.proxy", "Command for the proxy app");
    }

    @Override
    public boolean handleCommand(@Nonnull CommandSource commandSource, @Nonnull String[] strings) {
        ProxyApplication.getInstance().reloadConfig();
        DefaultChannelManager.INSTANCE.getAllSender().forEach(e -> e.sendPacket(new PacketOutConfigUpdate()));
        commandSource.sendMessage("Updated the proxy config locally and on all proxies");
        return true;
    }
}
