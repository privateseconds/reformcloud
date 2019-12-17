package systems.reformcloud.reformcloud2.permissions.packets.api.in;

import java.util.function.Consumer;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.PacketSender;
import systems.reformcloud.reformcloud2.executor.api.common.network.channel.handler.DefaultNetworkHandler;
import systems.reformcloud.reformcloud2.executor.api.common.network.packet.Packet;
import systems.reformcloud.reformcloud2.permissions.PermissionAPI;
import systems.reformcloud.reformcloud2.permissions.packets.PacketHelper;
import systems.reformcloud.reformcloud2.permissions.packets.util.PermissionAction;
import systems.reformcloud.reformcloud2.permissions.util.group.PermissionGroup;

public class APIPacketInGroupAction extends DefaultNetworkHandler {

  public APIPacketInGroupAction() { super(PacketHelper.PERMISSION_BUS + 2); }

  @Override
  public void handlePacket(PacketSender packetSender, Packet packet,
                           Consumer<Packet> responses) {
    final PermissionGroup permissionGroup =
        packet.content().get("group", PermissionGroup.TYPE);
    final PermissionAction permissionAction =
        packet.content().get("action", PermissionAction.class);

    switch (permissionAction) {
    case UPDATE: {
      PermissionAPI.getInstance()
          .getPermissionUtil()
          .handleInternalPermissionGroupUpdate(permissionGroup);
      break;
    }

    case DELETE: {
      PermissionAPI.getInstance()
          .getPermissionUtil()
          .handleInternalPermissionGroupDelete(permissionGroup);
      break;
    }

    case CREATE: {
      PermissionAPI.getInstance()
          .getPermissionUtil()
          .handleInternalPermissionGroupCreate(permissionGroup);
      break;
    }

    case DEFAULT_GROUPS_CHANGED: {
      PermissionAPI.getInstance()
          .getPermissionUtil()
          .handleInternalDefaultGroupsUpdate();
      break;
    }
    }
  }
}
