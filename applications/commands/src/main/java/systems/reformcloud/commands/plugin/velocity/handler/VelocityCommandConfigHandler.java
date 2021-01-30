/*
 * This file is part of reformcloud, licensed under the MIT License (MIT).
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
package systems.reformcloud.commands.plugin.velocity.handler;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import systems.reformcloud.commands.config.CommandsConfig;
import systems.reformcloud.commands.plugin.CommandConfigHandler;
import systems.reformcloud.commands.plugin.velocity.commands.CommandLeave;
import systems.reformcloud.commands.plugin.velocity.commands.CommandReformCloud;
import systems.reformcloud.base.Conditions;

import java.util.Arrays;

public class VelocityCommandConfigHandler extends CommandConfigHandler {

  private final ProxyServer proxyServer;
  private CommandLeave commandLeave;
  private CommandReformCloud commandReformCloud;

  public VelocityCommandConfigHandler(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Override
  public void handleCommandConfigRelease(@NotNull CommandsConfig commandsConfig) {
    this.unregisterAllCommands();
    if (commandsConfig.isLeaveCommandEnabled() && !commandsConfig.getLeaveCommands().isEmpty()) {
      this.commandLeave = new CommandLeave(commandsConfig.getLeaveCommands());
      this.proxyServer.getCommandManager().register(
        this.forAliases(commandsConfig.getLeaveCommands().toArray(new String[0])),
        this.commandLeave
      );
    }

    if (commandsConfig.isReformCloudCommandEnabled() && !commandsConfig.getReformCloudCommands().isEmpty()) {
      this.commandReformCloud = new CommandReformCloud(commandsConfig.getReformCloudCommands());
      this.proxyServer.getCommandManager().register(
        this.forAliases(commandsConfig.getReformCloudCommands().toArray(new String[0])),
        this.commandReformCloud
      );
    }
  }

  @Override
  public void unregisterAllCommands() {
    if (this.commandLeave != null) {
      this.commandLeave.getAliases().forEach(this.proxyServer.getCommandManager()::unregister);
      this.commandLeave = null;
    }

    if (this.commandReformCloud != null) {
      this.commandReformCloud.getAliases().forEach(this.proxyServer.getCommandManager()::unregister);
      this.commandReformCloud = null;
    }
  }

  private @NotNull CommandMeta forAliases(@NonNls String[] aliases) {
    Conditions.isTrue(aliases.length > 0);
    CommandMeta.Builder builder = this.proxyServer.getCommandManager().metaBuilder(aliases[0]);
    if (aliases.length > 1) {
      builder.aliases(Arrays.copyOfRange(aliases, 1, aliases.length));
    }

    return builder.build();
  }
}