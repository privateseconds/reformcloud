package systems.reformcloud.reformcloud2.signs.application.packets.in;

import systems.reformcloud.reformcloud2.executor.api.common.network.channel.PacketSender;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.handler.NetworkHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.manager.DefaultChannelManager;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.Packet;
import systems.reformcloud.reformcloud2.signs.application.ReformCloudApplication;
import systems.reformcloud.reformcloud2.signs.application.packets.out.PacketOutDeleteSign;
import systems.reformcloud.reformcloud2.signs.packets.PacketUtil;
import systems.reformcloud.reformcloud2.signs.util.sign.CloudSign;

import java.util.function.Consumer;

public class PacketInDeleteSign implements NetworkHandler {

    @Override
    public int getHandlingPacketID() {
        return PacketUtil.SIGN_BUS + 1;
    }

    @Override
    public void handlePacket(PacketSender packetSender, Packet packet, Consumer<Packet> responses) {
        CloudSign sign = packet.content().get("sign", CloudSign.TYPE);
        if (sign == null) {
            return;
        }

        ReformCloudApplication.delete(sign);
        DefaultChannelManager.INSTANCE.getAllSender().forEach(e -> e.sendPacket(new PacketOutDeleteSign(sign)));
    }
}