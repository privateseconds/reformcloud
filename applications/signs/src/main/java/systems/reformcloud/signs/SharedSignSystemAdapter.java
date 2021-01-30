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
package systems.reformcloud.signs;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import systems.refomcloud.embedded.Embedded;
import systems.reformcloud.ExecutorAPI;
import systems.reformcloud.event.EventManager;
import systems.reformcloud.group.process.ProcessGroup;
import systems.reformcloud.network.packet.PacketProvider;
import systems.reformcloud.process.ProcessInformation;
import systems.reformcloud.process.ProcessState;
import systems.reformcloud.utility.MoreCollections;
import systems.reformcloud.signs.application.packets.PacketCreateSign;
import systems.reformcloud.signs.application.packets.PacketDeleteBulkSigns;
import systems.reformcloud.signs.application.packets.PacketDeleteSign;
import systems.reformcloud.signs.listener.CloudListener;
import systems.reformcloud.signs.packets.PacketReloadSignConfig;
import systems.reformcloud.signs.util.LayoutUtil;
import systems.reformcloud.signs.util.SignSystemAdapter;
import systems.reformcloud.signs.util.Utils;
import systems.reformcloud.signs.util.sign.CloudLocation;
import systems.reformcloud.signs.util.sign.CloudSign;
import systems.reformcloud.signs.util.sign.config.SignConfig;
import systems.reformcloud.signs.util.sign.config.SignLayout;
import systems.reformcloud.signs.util.sign.config.SignSubLayout;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SharedSignSystemAdapter<T> implements SignSystemAdapter<T> {

  private static final String[] EMPTY_SIGN = new String[]{"", "", "", ""};
  protected final UUID ownUniqueID = Embedded.getInstance().getCurrentProcessInformation().getId().getUniqueId();
  protected final Collection<CloudSign> signs = new CopyOnWriteArrayList<>();
  protected final Set<ProcessInformation> allProcesses = Collections.synchronizedSet(new HashSet<>());
  protected final Map<String, AtomicInteger[]> perGroupLayoutCounter = new HashMap<>();
  protected SignConfig signConfig;

  public SharedSignSystemAdapter(@NotNull SignConfig signConfig) {
    this.signConfig = signConfig;

    SignSystemAdapter.instance.set(this);

    ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(EventManager.class).registerListener(new CloudListener());
    ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).registerPackets(Arrays.asList(
      PacketCreateSign.class,
      PacketDeleteSign.class,
      PacketReloadSignConfig.class
    ));

    this.start();
  }

  @Override
  public void handleProcessStart(@NotNull ProcessInformation processInformation) {
    this.allProcesses.add(processInformation);

    if (processInformation.getPrimaryTemplate().getVersion().getVersionType().isProxy() || processInformation.getId().getUniqueId().equals(this.ownUniqueID)) {
      return;
    }

    if (!Utils.canConnectPerState(processInformation)) {
      return;
    }

    this.tryAssign(processInformation);
  }

  @Override
  public void handleProcessUpdate(@NotNull ProcessInformation processInformation) {
    if (processInformation.getPrimaryTemplate().getVersion().getVersionType().isProxy() || processInformation.getId().getUniqueId().equals(this.ownUniqueID)) {
      return;
    }

    CloudSign sign = this.getSignOf(processInformation);
    if (sign == null && Utils.canConnectPerState(processInformation)) {
      this.tryAssign(processInformation);
      return;
    }

    if (sign != null && !Utils.canConnectPerState(processInformation)) {
      sign.setCurrentTarget(null);
      this.updateSigns();
      this.tryAssignUnassigned();
      return;
    }

    if (sign == null) {
      return;
    }

    sign.setCurrentTarget(processInformation);
    this.updateSigns();
  }

  @Override
  public void handleProcessStop(@NotNull ProcessInformation processInformation) {
    this.allProcesses.remove(processInformation);

    CloudSign sign = this.getSignOf(processInformation);
    if (sign != null) {
      sign.setCurrentTarget(null);
      this.updateSigns();
      this.tryAssignUnassigned();
    }
  }

  @Override
  public @NotNull CloudSign createSign(@NotNull T t, @NotNull String group) {
    CloudSign cloudSign = this.getSignAt(this.getSignConverter().to(t));
    if (cloudSign != null) {
      return cloudSign;
    }

    cloudSign = this.getSignConverter().to(t, group);
    Embedded.getInstance().sendPacket(new PacketCreateSign(cloudSign));

    return cloudSign;
  }

  @Override
  public void deleteSign(@NotNull CloudLocation location) {
    CloudSign sign = this.getSignAt(location);
    if (sign != null) {
      Embedded.getInstance().sendPacket(new PacketDeleteSign(sign));
    }
  }

  @Override
  public void deleteAll() {
    this.bulkDelete(this.signs);
  }

  @Override
  public void cleanSigns() {
    this.bulkDelete(this.signs.stream()
      .filter(e -> this.getSignConverter().from(e) == null)
      .collect(Collectors.toList()));
  }

  private void bulkDelete(@NotNull Collection<CloudSign> signs) {
    if (signs.isEmpty()) {
      return;
    }

    Embedded.getInstance().sendPacket(new PacketDeleteBulkSigns(signs));
  }

  @Override
  public @Nullable CloudSign getSignAt(@NotNull CloudLocation location) {
    for (CloudSign sign : this.signs) {
      if (sign.getLocation().equals(location)) {
        return sign;
      }
    }

    return null;
  }

  @Override
  public boolean canConnect(@NotNull CloudSign cloudSign, @NotNull Function<String, Boolean> permissionChecker) {
    SignLayout layout = this.getSignLayout(cloudSign.getGroup());
    if (layout == null) {
      return cloudSign.getCurrentTarget() != null && Utils.canConnect(cloudSign.getCurrentTarget(), permissionChecker);
    }

    if (cloudSign.getCurrentTarget() == null || !Utils.canConnect(cloudSign.getCurrentTarget(), permissionChecker)) {
      return !layout.isSearchingLayoutWhenFull();
    }

    ProcessInformation process = cloudSign.getCurrentTarget();
    return process.getOnlineCount() < process.getProcessGroup().getPlayerAccessConfiguration().getMaxPlayers() || !layout.isSearchingLayoutWhenFull();
  }

  @Override
  public void handleInternalSignCreate(@NotNull CloudSign cloudSign) {
    this.signs.add(cloudSign);
    this.tryAssignUnassigned();
  }

  @Override
  public void handleInternalSignDelete(@NotNull CloudSign cloudSign) {
    CloudSign other = MoreCollections.filter(this.signs, e -> e.equals(cloudSign));

    this.signs.remove(cloudSign);
    this.setSignLines(cloudSign, EMPTY_SIGN);
    if (other == null) {
      return;
    }

    this.removeAssignment(other);
    this.tryAssignUnassigned();
  }

  protected void removeAssignment(@NotNull CloudSign sign) {
    if (sign.getCurrentTarget() == null) {
      return;
    }

    sign.setCurrentTarget(null);
  }

  protected void tryAssignUnassigned() {
    for (ProcessInformation notAssignedProcess : this.allProcesses) {
      if (this.isProcessAssigned(notAssignedProcess) || !Utils.canConnectPerState(notAssignedProcess)) {
        continue;
      }

      if (notAssignedProcess.getPrimaryTemplate().getVersion().getVersionType().isProxy() || notAssignedProcess.getId().getUniqueId().equals(this.ownUniqueID)) {
        continue;
      }

      CloudSign sign = this.getFreeSignForGroup(notAssignedProcess.getProcessGroup());
      if (sign == null) {
        continue;
      }

      sign.setCurrentTarget(notAssignedProcess);
      this.updateSigns();
    }
  }

  protected abstract void setSignLines(@NotNull CloudSign cloudSign, @NotNull String[] lines);

  protected void updateSigns() {
    for (CloudSign sign : this.signs) {
      SignLayout layout = this.getSignLayout(sign.getGroup());
      if (layout == null) {
        System.err.println("Unable to find global layout / sign layout for group " + sign.getGroup());
        continue;
      }

      this.perGroupLayoutCounter.putIfAbsent(sign.getGroup(), new AtomicInteger[]{
        new AtomicInteger(), // start
        new AtomicInteger(), // connecting
        new AtomicInteger(), // empty
        new AtomicInteger(), // online
        new AtomicInteger(), // full
        new AtomicInteger()  // maintenance
      });
      AtomicInteger[] counter = this.perGroupLayoutCounter.get(sign.getGroup());

      final SignSubLayout searching = LayoutUtil.getNextAndCheckFor(layout.getSearchingLayouts(), counter[0])
        .orElseThrow(() -> new RuntimeException("Waiting layout for current group not present"));

      final SignSubLayout maintenance = LayoutUtil.getNextAndCheckFor(layout.getMaintenanceLayout(), counter[5])
        .orElseThrow(() -> new RuntimeException("Maintenance layout for current group not present"));

      final SignSubLayout connecting = LayoutUtil.getNextAndCheckFor(layout.getWaitingForConnectLayout(), counter[1])
        .orElseThrow(() -> new RuntimeException("Connecting layout for current group not present"));

      final SignSubLayout empty = LayoutUtil.getNextAndCheckFor(layout.getEmptyLayout(), counter[2])
        .orElseThrow(() -> new RuntimeException("Empty layout for current group not present"));

      final SignSubLayout full = LayoutUtil.getNextAndCheckFor(layout.getFullLayout(), counter[4])
        .orElseThrow(() -> new RuntimeException("Full layout for current group not present"));

      final SignSubLayout online = LayoutUtil.getNextAndCheckFor(layout.getOnlineLayout(), counter[3])
        .orElseThrow(() -> new RuntimeException("Online layout for current group not present"));

      if (sign.getCurrentTarget() == null) {
        this.setLines(sign, searching);
        continue;
      }

      if (sign.getCurrentTarget().getProcessGroup().getPlayerAccessConfiguration().isMaintenance()) {
        if (layout.isShowMaintenanceProcessesOnSigns()) {
          this.setLines(sign, maintenance);
          continue;
        }

        this.setLines(sign, searching);
        continue;
      }

      if (sign.getCurrentTarget().getCurrentState() == ProcessState.STARTED) {
        this.setLines(sign, connecting);
        continue;
      }

      if (!sign.getCurrentTarget().getCurrentState().isOnline()) {
        this.setLines(sign, searching);
        continue;
      }

      if (sign.getCurrentTarget().getOnlineCount() == 0) {
        this.setLines(sign, empty);
        continue;
      }

      if (sign.getCurrentTarget().getOnlineCount() >= sign.getCurrentTarget().getProcessGroup().getPlayerAccessConfiguration().getMaxPlayers()) {
        if (layout.isSearchingLayoutWhenFull()) {
          this.setLines(sign, searching);
          continue;
        }

        this.setLines(sign, full);
        continue;
      }

      this.setLines(sign, online);
    }

    this.signs.stream().map(CloudSign::getGroup).distinct().forEach(group -> {
      SignLayout layout = this.getSignLayout(group);
      if (layout == null) {
        return;
      }

      AtomicInteger[] counter = this.perGroupLayoutCounter.get(group);
      if (counter == null) {
        return;
      }

      LayoutUtil.flush(layout.getSearchingLayouts(), counter[0]);
      LayoutUtil.flush(layout.getMaintenanceLayout(), counter[5]);
      LayoutUtil.flush(layout.getWaitingForConnectLayout(), counter[1]);
      LayoutUtil.flush(layout.getEmptyLayout(), counter[2]);
      LayoutUtil.flush(layout.getFullLayout(), counter[4]);
      LayoutUtil.flush(layout.getOnlineLayout(), counter[3]);
    });
  }

  protected abstract void runTasks();

  @NotNull
  protected abstract String replaceAll(@NotNull String line, @NotNull String group, @Nullable ProcessInformation processInformation);

  public abstract void changeBlock(@NotNull CloudSign sign, @NotNull SignSubLayout layout);

  private void setLines(@NotNull CloudSign sign, @NotNull SignSubLayout layout) {
    String[] copy = layout.getLines().clone();
    if (copy.length != 4) {
      return;
    }

    for (int i = 0; i <= 3; i++) {
      copy[i] = this.replaceAll(copy[i], sign.getGroup(), sign.getCurrentTarget());
    }

    this.setSignLines(sign, copy);
    this.changeBlock(sign, layout);
  }

  @Nullable
  protected SignLayout getSignLayout(@NotNull String group) {
    return LayoutUtil.getLayoutFor(group, this.signConfig).orElse(null);
  }

  private void start() {
    ExecutorAPI.getInstance().getDatabaseProvider().getDatabase(SignSystemAdapter.table).getAsync("signs", "")
      .onComplete(result -> result.ifPresent(configuration -> {
        Collection<CloudSign> cloudSigns = configuration.get("signs", new TypeToken<Collection<CloudSign>>() {
        }.getType());
        if (cloudSigns == null) {
          return;
        }

        this.signs.addAll(cloudSigns.stream().filter(e -> {
          String current = Embedded.getInstance().getCurrentProcessInformation().getProcessGroup().getName();
          return current.equals(e.getLocation().getGroup());
        }).collect(Collectors.toList()));

        ExecutorAPI.getInstance().getProcessProvider().getProcesses().forEach(this::handleProcessStart);
        this.runTasks();
      }));
  }

  @Nullable
  private CloudSign getSignOf(@NotNull ProcessInformation other) {
    for (CloudSign sign : this.signs) {
      if (sign.getCurrentTarget() != null && sign.getCurrentTarget().getId().getUniqueId().equals(other.getId().getUniqueId())) {
        return sign;
      }
    }
    return null;
  }

  private @Nullable CloudSign getFreeSignForGroup(@NotNull ProcessGroup processGroup) {
    return MoreCollections.filter(this.signs, e -> e.getCurrentTarget() == null && e.getGroup().equals(processGroup.getName()));
  }

  private boolean isProcessAssigned(@NotNull ProcessInformation process) {
    return this.getSignOf(process) != null;
  }

  private void tryAssign(@NotNull ProcessInformation processInformation) {
    synchronized (this) {
      for (CloudSign sign : this.signs) {
        if (sign.getCurrentTarget() == null && sign.getGroup().equals(processInformation.getProcessGroup().getName())) {
          sign.setCurrentTarget(processInformation);
          this.updateSigns();
          break;
        }
      }
    }
  }
}