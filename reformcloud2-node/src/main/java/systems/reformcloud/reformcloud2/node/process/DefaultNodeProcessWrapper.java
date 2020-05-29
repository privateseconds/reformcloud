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
package systems.reformcloud.reformcloud2.node.process;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import systems.reformcloud.reformcloud2.executor.api.ExecutorAPI;
import systems.reformcloud.reformcloud2.executor.api.groups.template.Template;
import systems.reformcloud.reformcloud2.executor.api.groups.template.Version;
import systems.reformcloud.reformcloud2.executor.api.groups.template.backend.TemplateBackendManager;
import systems.reformcloud.reformcloud2.executor.api.io.IOUtils;
import systems.reformcloud.reformcloud2.executor.api.process.ProcessInformation;
import systems.reformcloud.reformcloud2.executor.api.process.ProcessState;
import systems.reformcloud.reformcloud2.executor.api.process.api.ProcessInclusion;
import systems.reformcloud.reformcloud2.executor.api.utility.StringUtil;
import systems.reformcloud.reformcloud2.executor.api.utility.list.Streams;
import systems.reformcloud.reformcloud2.executor.api.utility.process.JavaProcessHelper;
import systems.reformcloud.reformcloud2.executor.api.wrappers.ProcessWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class DefaultNodeProcessWrapper implements ProcessWrapper {

    private static final String LIB_PATH = Paths.get("").toAbsolutePath().toString();
    private static final String[] DEFAULT_SHUTDOWN_COMMANDS = new String[]{"end", "stop"};

    public DefaultNodeProcessWrapper(ProcessInformation processInformation) {
        this.processInformation = processInformation;

        this.path = processInformation.getProcessGroup().isStaticProcess()
                ? Paths.get("reformcloud/static", processInformation.getProcessDetail().getName())
                : Paths.get("reformcloud/temp", processInformation.getProcessDetail().getName() + "-" + processInformation.getProcessDetail().getProcessUniqueID());
        this.firstStart = Files.notExists(this.path);

        IOUtils.createDirectory(this.path);
    }

    private final Lock lock = new ReentrantLock();
    private final String connectionKey = StringUtil.generateString(16);
    private final Path path;
    private final boolean firstStart;

    private ProcessInformation processInformation;
    private ProcessState runtimeState;
    private Process process;

    @NotNull
    @Override
    public ProcessInformation getProcessInformation() {
        return this.processInformation;
    }

    void setProcessInformation(@NotNull ProcessInformation information) {
        this.processInformation = information;
    }

    @NotNull
    @Override
    public Optional<ProcessInformation> requestProcessInformation() {
        return Optional.of(this.processInformation);
    }

    @NotNull
    @Override
    public Optional<ProcessInformation> requestProcessInformationUpdate() {
        return Optional.empty(); // todo
    }

    @NotNull
    @Override
    public Optional<String> uploadLog() {
        return Optional.empty(); // todo
    }

    @NotNull
    @Override
    public @UnmodifiableView Queue<String> getLastLogLines() {
        return null; // todo
    }

    @Override
    public void setRuntimeState(@NotNull ProcessState state) {
        this.processInformation.getProcessDetail().setProcessState(state);
        ExecutorAPI.getInstance().getProcessProvider().updateProcessInformation(this.processInformation);

        if (state.isRuntimeState() && this.runtimeState != state) {
            this.runtimeState = state;
            this.callRuntimeStateUpdate();
        }
    }

    @Override
    public void copy(@NotNull Template template) {
        this.copy(this.processInformation.getProcessGroup().getName(), template.getName(), template.getBackend());
    }

    @Override
    public void copy(@NotNull String path) {
    }

    @Override
    public void copy(@NotNull String templateGroup, @NotNull String templateName) {
    }

    @Override
    public void copy(@NotNull String templateGroup, @NotNull String templateName, @NotNull String templateBackend) {
    }

    private void callRuntimeStateUpdate() {
        try {
            this.lock.lock();

            switch (this.runtimeState) {
                case PREPARED:
                    this.prepare();
                    break;
                case STARTED:
                    this.start();
                    break;
                case RESTARTING:
                    this.restart();
                    break;
                case PAUSED:
                    this.stop(false);
                    break;
                case STOPPED:
                    this.stop(true);
                    break;
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void prepare() {
        EnvironmentBuilder.constructEnvFor(this, this.firstStart);
    }

    private void start() {
        List<String> command = new ArrayList<>(Arrays.asList(
                this.processInformation.getProcessGroup().getStartupConfiguration().getJvmCommand(),

                "-DIReallyKnowWhatIAmDoingISwear=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-Dreformcloud.runner.version=" + System.getProperty("reformcloud.runner.version"),
                "-Dreformcloud.executor.type=3",
                "-Dreformcloud.lib.path=" + LIB_PATH,
                "-Dreformcloud.process.path=" + new File("reformcloud/files/" + Version.format(
                        this.processInformation.getProcessDetail().getTemplate().getVersion()
                )).getAbsolutePath()
        ));
        this.processInformation.getProcessDetail().getTemplate().getRuntimeConfiguration().getSystemProperties().forEach(
                (key, value) -> command.add(String.format("-D%s=%s", key, value))
        );

        command.addAll(this.processInformation.getProcessDetail().getTemplate().getRuntimeConfiguration().getJvmOptions());
        command.addAll(Arrays.asList(
                "-Xmx" + this.processInformation.getProcessDetail().getMaxMemory() + "M",
                "-cp", StringUtil.NULL_PATH,
                "-javaagent:runner.jar",
                "systems.reformcloud.reformcloud2.runner.RunnerExecutor"
        ));
        command.addAll(this.processInformation.getProcessDetail().getTemplate().getRuntimeConfiguration().getProcessParameters());

        if (this.processInformation.getProcessDetail().getTemplate().getVersion().getId() == 1) {
            command.add("nogui"); // Spigot server
        } else if (this.processInformation.getProcessDetail().getTemplate().getVersion().getId() == 3) {
            command.add("disable-ansi"); // Nukkit server
        }

        try {
            this.process = new ProcessBuilder()
                    .command(command)
                    .directory(this.path.toFile())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void restart() {
        this.stop(false);
        this.start();
    }

    private void stop(boolean finalStop) {
        JavaProcessHelper.shutdown(this.process, true, true, TimeUnit.SECONDS.toDays(5), this.getShutdownCommands());

        if (finalStop) {
            if (this.processInformation.getProcessDetail().getTemplate().isAutoReleaseOnClose()) {
                TemplateBackendManager.getOrDefault(this.processInformation.getProcessDetail().getTemplate().getBackend()).deployTemplate(
                        this.processInformation.getProcessGroup().getName(),
                        this.processInformation.getProcessDetail().getTemplate().getName(),
                        this.path,
                        this.processInformation.getPreInclusions().stream().map(ProcessInclusion::getName).collect(Collectors.toList())
                );
            }

            if (!this.processInformation.getProcessGroup().isStaticProcess()) {
                IOUtils.deleteDirectory(this.path);
            }
        }
    }

    private @NotNull String[] getShutdownCommands() {
        String[] shutdownCommands = this.processInformation.getProcessDetail().getTemplate().getRuntimeConfiguration().getShutdownCommands().toArray(new String[0]);
        return Streams.concat(shutdownCommands, DEFAULT_SHUTDOWN_COMMANDS);
    }

    Path getPath() {
        return this.path;
    }

    public String getConnectionKey() {
        return this.connectionKey;
    }
}
