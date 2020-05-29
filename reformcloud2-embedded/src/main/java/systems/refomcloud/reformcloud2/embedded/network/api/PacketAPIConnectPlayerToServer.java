/*
 * MIT License
 *
 * Copyright (c) ReformCloud-Team
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package systems.refomcloud.reformcloud2.embedded.network.api;

import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.refomcloud.reformcloud2.embedded.executor.PlayerAPIExecutor;
import systems.reformcloud.reformcloud2.executor.api.api.basic.ExternalAPIImplementation;
import systems.reformcloud.reformcloud2.executor.api.network.challenge.ChallengeAuthHandler;
import systems.reformcloud.reformcloud2.executor.api.network.channel.EndpointChannelReader;
import systems.reformcloud.reformcloud2.executor.api.network.channel.PacketSender;
import systems.reformcloud.reformcloud2.executor.api.network.data.ProtocolBuffer;
import systems.reformcloud.reformcloud2.executor.api.network.netty.NettyChannelEndpoint;
import systems.reformcloud.reformcloud2.executor.api.network.packet.Packet;

import java.util.UUID;

public class PacketAPIConnectPlayerToServer extends Packet {

    private UUID targetPlayer;
    private String targetServer;

    public PacketAPIConnectPlayerToServer() {
    }

    public PacketAPIConnectPlayerToServer(UUID targetPlayer, String targetServer) {
        this.targetPlayer = targetPlayer;
        this.targetServer = targetServer;
    }

    @Override
    public int getId() {
        return ExternalAPIImplementation.EXTERNAL_PACKET_ID + 201;
    }

    @Override
    public void handlePacketReceive(@NotNull EndpointChannelReader reader, @NotNull ChallengeAuthHandler authHandler, @NotNull NettyChannelEndpoint parent, @Nullable PacketSender sender, @NotNull ChannelHandlerContext channel) {
        PlayerAPIExecutor.getInstance().executeConnect(this.targetPlayer, this.targetServer);
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeUniqueId(this.targetPlayer);
        buffer.writeString(this.targetServer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.targetPlayer = buffer.readUniqueId();
        this.targetServer = buffer.readString();
    }
}
