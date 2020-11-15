/*
 * This file is part of reformcloud2, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/ReformCloud>
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
package systems.reformcloud.reformcloud2.protocol.node;

import org.jetbrains.annotations.NotNull;
import systems.reformcloud.reformcloud2.executor.api.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.network.PacketIds;
import systems.reformcloud.reformcloud2.executor.api.network.channel.NetworkChannel;
import systems.reformcloud.reformcloud2.executor.api.network.channel.listener.ChannelListener;
import systems.reformcloud.reformcloud2.executor.api.network.data.ProtocolBuffer;
import systems.reformcloud.reformcloud2.executor.api.process.ProcessInformation;
import systems.reformcloud.reformcloud2.protocol.ProtocolPacket;
import systems.reformcloud.reformcloud2.shared.collect.Entry2;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ApiToNodeGetCurrentPlayerProcessUniqueIds extends ProtocolPacket {

    private UUID playerUniqueId;

    public ApiToNodeGetCurrentPlayerProcessUniqueIds() {
    }

    public ApiToNodeGetCurrentPlayerProcessUniqueIds(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId;
    }

    @Override
    public int getId() {
        return PacketIds.EMBEDDED_BUS + 47;
    }

    @Override
    public void handlePacketReceive(@NotNull ChannelListener reader, @NotNull NetworkChannel channel) {
        channel.sendQueryResult(this.getQueryUniqueID(), new ApiToNodeGetCurrentPlayerProcessUniqueIdsResult(this.getPlayerProcess().orElse(null)));
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeUniqueId(this.playerUniqueId);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.playerUniqueId = buffer.readUniqueId();
    }

    @NotNull
    private Optional<Map.Entry<UUID, UUID>> getPlayerProcess() {
        UUID proxy = null;
        UUID server = null;

        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            if (process.getPrimaryTemplate().getVersion().getVersionType().isServer()
                && process.getPlayerByUniqueId(this.playerUniqueId).isPresent()
                && server == null) {
                server = process.getId().getUniqueId();
            } else if (process.getPrimaryTemplate().getVersion().getVersionType().isProxy()
                && process.getPlayerByUniqueId(this.playerUniqueId).isPresent()
                && proxy == null) {
                proxy = process.getId().getUniqueId();
            }
        }

        return proxy == null || server == null ? Optional.empty() : Optional.of(new Entry2<>(proxy, server));
    }
}
