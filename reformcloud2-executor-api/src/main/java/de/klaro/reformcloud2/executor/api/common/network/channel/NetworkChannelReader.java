package de.klaro.reformcloud2.executor.api.common.network.channel;

import de.klaro.reformcloud2.executor.api.common.network.packet.Packet;
import de.klaro.reformcloud2.executor.api.common.network.packet.handler.PacketHandler;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public interface NetworkChannelReader {

    PacketHandler getPacketHandler();

    PacketSender sender();

    void setSender(PacketSender sender);

    void channelActive(ChannelHandlerContext context);

    void channelInactive(ChannelHandlerContext context);

    void read(ChannelHandlerContext context, Packet packet);

    default void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (!(cause instanceof IOException)) {
            System.err.println("Exception in channel " + context.channel().remoteAddress());
            cause.printStackTrace();
        }
    }

    default void readOperationCompleted(ChannelHandlerContext context) {
        context.flush();
    }
}
