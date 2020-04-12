package systems.reformcloud.reformcloud2.signs.nukkit.commands;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import systems.reformcloud.reformcloud2.executor.api.common.ExecutorAPI;
import systems.reformcloud.reformcloud2.signs.nukkit.adapter.NukkitSignSystemAdapter;
import systems.reformcloud.reformcloud2.signs.util.SignSystemAdapter;
import systems.reformcloud.reformcloud2.signs.util.sign.CloudSign;

public class NukkitCommandSigns implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender.hasPermission("reformcloud.command.signs")) || !(commandSender instanceof Player)) {
            return true;
        }

        NukkitSignSystemAdapter signSystemAdapter = NukkitSignSystemAdapter.getInstance();
        Player player = (Player) commandSender;

        if (strings.length == 2 && strings[0].equalsIgnoreCase("create")) {
            if (ExecutorAPI.getInstance().getSyncAPI().getGroupSyncAPI().getProcessGroup(strings[1]) == null) {
                commandSender.sendMessage("§7The process group " + strings[1] + " does not exists");
                return true;
            }

            Block block = player.getTargetBlock(15);
            if (block == null || !(block.getLevel().getBlockEntity(block.getLocation()) instanceof BlockEntitySign)) {
                commandSender.sendMessage("§cThe target Block is not a sign");
                return true;
            }

            BlockEntitySign entitySign = (BlockEntitySign) block.getLevel().getBlockEntity(block.getLocation());
            CloudSign cloudSign = signSystemAdapter.getSignAt(signSystemAdapter.getSignConverter().to(entitySign));
            if (cloudSign != null) {
                commandSender.sendMessage("§cThe sign already exists");
                return true;
            }

            signSystemAdapter.createSign(entitySign, strings[1]);
            commandSender.sendMessage("§7Created the sign successfully, please wait a second...");
            return true;
        }

        if (strings.length == 1 && strings[0].equalsIgnoreCase("delete")) {
            Block block = player.getTargetBlock(15);
            if (block == null || !(block.getLevel().getBlockEntity(block.getLocation()) instanceof BlockEntitySign)) {
                commandSender.sendMessage("§cThe target Block is not a sign");
                return true;
            }

            BlockEntitySign entitySign = (BlockEntitySign) block.getLevel().getBlockEntity(block.getLocation());
            CloudSign cloudSign = signSystemAdapter.getSignAt(signSystemAdapter.getSignConverter().to(entitySign));
            if (cloudSign == null) {
                commandSender.sendMessage("§cThe sign does not exists");
                return true;
            }

            signSystemAdapter.deleteSign(cloudSign.getLocation());
            commandSender.sendMessage("§7Deleted sign, please wait a second...");
            return true;
        }

        if (strings.length == 1 && strings[0].equalsIgnoreCase("deleteall")) {
            SignSystemAdapter.getInstance().deleteAll();
            commandSender.sendMessage("§7Deleting all signs, please wait...");
            return true;
        }

        if (strings.length == 1 && strings[0].equalsIgnoreCase("clean")) {
            SignSystemAdapter.getInstance().cleanSigns();
            commandSender.sendMessage("§7Cleaning signs, please wait...");
            return true;
        }

        commandSender.sendMessage("§7/signs create [group]");
        commandSender.sendMessage("§7/signs delete");
        commandSender.sendMessage("§7/signs deleteAll");
        commandSender.sendMessage("§7/signs clean");
        return true;
    }
}
