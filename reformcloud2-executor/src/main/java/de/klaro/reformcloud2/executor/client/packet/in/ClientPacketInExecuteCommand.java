package de.klaro.reformcloud2.executor.client.packet.in;

import de.klaro.reformcloud2.executor.api.common.network.channel.PacketSender;
import de.klaro.reformcloud2.executor.api.common.network.channel.handler.NetworkHandler;
import de.klaro.reformcloud2.executor.api.common.network.packet.Packet;
import de.klaro.reformcloud2.executor.client.ClientExecutor;

import java.util.function.Consumer;

public final class ClientPacketInExecuteCommand implements NetworkHandler {

    @Override
    public int getHandlingPacketID() {
        return 48;
    }

    @Override
    public void handlePacket(PacketSender packetSender, Packet packet, Consumer<Packet> responses) {
        String name = packet.content().getString("name");
        String command = packet.content().getString("command");

        ClientExecutor.getInstance().getProcessManager().getProcess(name).ifPresent(runningProcess -> runningProcess.sendCommand(command));
    }
}
