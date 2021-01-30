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
package systems.reformcloud.node.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.reformcloud.ExecutorAPI;
import systems.reformcloud.command.Command;
import systems.reformcloud.command.CommandSender;
import systems.reformcloud.language.TranslationHolder;
import systems.reformcloud.process.Player;
import systems.reformcloud.process.ProcessInformation;
import systems.reformcloud.shared.Constants;
import systems.reformcloud.shared.collect.Entry3;
import systems.reformcloud.shared.parser.Parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class CommandPlayers implements Command {

  private static final String FORMAT_STRING = " > %s/%s is connected to %s <-> %s";

  public void describeCommandToSender(@NotNull CommandSender source) {
    source.sendMessages((
      "players [list]               | Lists all connected players\n" +
        "players <name | uuid> [info] | Shows information about a specific online player"
    ).split("\n"));
  }

  @Override
  public void process(@NotNull CommandSender sender, String[] strings, @NotNull String commandLine) {
    if (strings.length == 0) {
      this.describeCommandToSender(sender);
      return;
    }

    if (strings[0].equalsIgnoreCase("list")) {
      sender.sendMessage("Online-Players: ");
      ExecutorAPI.getInstance().getProcessProvider().getProcesses()
        .stream()
        .filter(e -> e.getPrimaryTemplate().getVersion().getVersionType().isProxy())
        .map(ProcessInformation::getPlayers)
        .map(players -> players.stream().map(p -> this.findPlayer(p.getUniqueID())).filter(Objects::nonNull))
        .forEach(trioStream -> {
          StringBuilder stringBuilder = new StringBuilder();
          trioStream.filter(e -> e.getThird() != null).forEach(player -> stringBuilder.append(String.format(
            FORMAT_STRING,
            player.getThird().getName(),
            player.getThird().getUniqueID().toString(),
            player.getSecond().getName(),
            player.getFirst().getName()
          )).append("\n"));

          if (stringBuilder.length() > 0) {
            sender.sendMessages(stringBuilder.substring(0, stringBuilder.length() - 1).split("\n"));
          }
        });
      return;
    }

    if (strings.length == 2 && strings[1].equalsIgnoreCase("info")) {
      Entry3<ProcessInformation, ProcessInformation, Player> entry;
      UUID uniqueID;
      if ((uniqueID = Parsers.UNIQUE_ID.parse(strings[0])) != null) {
        entry = this.findPlayer(uniqueID);
      } else {
        entry = this.findPlayer(strings[0]);
      }

      if (entry == null) {
        sender.sendMessage(TranslationHolder.translate("command-players-player-not-found", strings[0]));
        return;
      }

      Player subServerPlayer = entry.getFirst().getPlayers().stream().filter(e -> uniqueID == null
        ? e.getName().equals(strings[0]) : e.getUniqueID().equals(uniqueID)).findAny().orElse(null);
      if (subServerPlayer == null) {
        sender.sendMessage(TranslationHolder.translate("command-players-player-not-found", strings[0]));
        return;
      }

      AtomicReference<StringBuilder> stringBuilder = new AtomicReference<>(new StringBuilder());
      stringBuilder.get().append(" > Name               - ").append(entry.getThird().getName()).append("\n");
      stringBuilder.get().append(" > UUID               - ").append(entry.getThird().getUniqueID()).append("\n");
      stringBuilder.get().append(" > Proxy              - ").append(entry.getSecond().getName()).append("\n");
      stringBuilder.get().append(" > Connected (Proxy)  - ").append(Constants.FULL_DATE_FORMAT.format(entry.getThird().getJoined())).append("\n");
      stringBuilder.get().append(" > Server             - ").append(entry.getFirst().getName()).append("\n");
      stringBuilder.get().append(" > Connected (Server) - ").append(Constants.FULL_DATE_FORMAT.format(subServerPlayer.getJoined())).append("\n");
      sender.sendMessages(stringBuilder.get().toString().split("\n"));
      return;
    }

    this.describeCommandToSender(sender);
  }

  @Override
  public @NotNull List<String> suggest(@NotNull CommandSender commandSender, String[] strings, int bufferIndex, @NotNull String commandLine) {
    List<String> result = new ArrayList<>();
    switch (bufferIndex) {
      case 0:
        result.add("list");
        break;
      case 1:
        result.add("info");
        break;
      default:
        break;
    }

    return result;
  }

  @Nullable
  private Entry3<ProcessInformation, ProcessInformation, Player> findPlayer(UUID uniqueID) {
    ProcessInformation server = this.getProcess(null, uniqueID, false);
    ProcessInformation proxy = this.getProcess(null, uniqueID, true);
    return server == null || proxy == null
      ? null
      : new Entry3<>(server, proxy, proxy.getPlayerByUniqueId(uniqueID).orElse(null));
  }

  @Nullable
  private Entry3<ProcessInformation, ProcessInformation, Player> findPlayer(@NotNull String name) {
    ProcessInformation server = this.getProcess(name, null, false);
    ProcessInformation proxy = this.getProcess(name, null, true);
    return server == null || proxy == null
      ? null
      : new Entry3<>(server, proxy, proxy.getPlayerByName(name).orElse(null));
  }

  @Nullable
  private ProcessInformation getProcess(@Nullable String name, @Nullable UUID uuid, boolean proxy) {
    if (name == null && uuid == null) {
      return null;
    }

    return ExecutorAPI.getInstance().getProcessProvider().getProcesses()
      .stream()
      .filter(e -> proxy == e.getPrimaryTemplate().getVersion().getVersionType().isProxy())
      .filter(e -> name == null ? e.getPlayerByUniqueId(uuid).isPresent() : e.getPlayerByName(name).isPresent())
      .findFirst()
      .orElse(null);
  }
}