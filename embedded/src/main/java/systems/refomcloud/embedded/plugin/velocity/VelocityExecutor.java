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
package systems.refomcloud.embedded.plugin.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import systems.refomcloud.embedded.Embedded;
import systems.refomcloud.embedded.controller.ProcessEventHandler;
import systems.refomcloud.embedded.controller.ProxyServerController;
import systems.refomcloud.embedded.executor.PlayerAPIExecutor;
import systems.refomcloud.embedded.plugin.velocity.controller.VelocityProxyServerController;
import systems.refomcloud.embedded.plugin.velocity.event.PlayerListenerHandler;
import systems.refomcloud.embedded.plugin.velocity.executor.VelocityPlayerAPIExecutor;
import systems.refomcloud.embedded.shared.SharedInvalidPlayerFixer;
import systems.reformcloud.ExecutorType;
import systems.reformcloud.event.EventManager;
import systems.reformcloud.process.ProcessInformation;
import systems.reformcloud.shared.process.DefaultPlayer;

public final class VelocityExecutor extends Embedded {

  public static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder().character('§').extractUrls().build();

  private static VelocityExecutor instance;
  private final ProxyServer proxyServer;
  private final VelocityLauncher plugin;

  VelocityExecutor(VelocityLauncher launcher, ProxyServer proxyServer) {
    super.type = ExecutorType.API;
    PlayerAPIExecutor.setInstance(new VelocityPlayerAPIExecutor(proxyServer));

    instance = this;
    this.plugin = launcher;
    this.proxyServer = proxyServer;

    super.getServiceRegistry().setProvider(ProxyServerController.class, new VelocityProxyServerController(proxyServer), true);
    super.getServiceRegistry().getProviderUnchecked(EventManager.class).registerListener(new ProcessEventHandler());

    proxyServer.getEventManager().register(launcher, new PlayerListenerHandler());
    this.fixInvalidPlayers();
  }

  @NotNull
  public static VelocityExecutor getInstance() {
    return instance;
  }

  @Override
  public int getPlayerCount() {
    return this.proxyServer.getPlayerCount();
  }

  @Override
  protected int getMaxPlayersOfEnvironment() {
    return this.proxyServer == null ? 0 : this.proxyServer.getConfiguration().getShowMaxPlayers();
  }

  @Override
  protected void updatePlayersOfEnvironment(@NotNull ProcessInformation information) {
    if (this.proxyServer != null) {
      for (Player player : this.proxyServer.getAllPlayers()) {
        if (!information.getPlayerByUniqueId(player.getUniqueId()).isPresent()) {
          information.getPlayers().add(new DefaultPlayer(player.getUniqueId(), player.getUsername(), System.currentTimeMillis()));
        }
      }
    }
  }

  @NotNull
  public VelocityLauncher getPlugin() {
    return this.plugin;
  }

  @NotNull
  public ProxyServer getProxyServer() {
    return this.proxyServer;
  }

  private void fixInvalidPlayers() {
    SharedInvalidPlayerFixer.start(
      uuid -> this.proxyServer.getPlayer(uuid).isPresent(),
      this.proxyServer::getPlayerCount
    );
  }
}
