
package com.example.adminwebjava;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

public class App {

    private static Process spring;
    private static Process cloudflare;

    private static final String CLOUDFLARED =
        "C:\\Program Files (x86)\\cloudflared\\cloudflared.exe";

    private static final String LOCAL_URL =
        "http://localhost:8080";

    public static void main(String[] args) {

        try {
            // 1. Tự tìm thư mục project
            Path project = findProjectDirectory();

            System.out.println("Thu muc project: " + project);

            System.out.println("================================");
            System.out.println("       TOURISM WEB LAUNCHER");
            System.out.println("================================");

            // 2. Kiểm tra các file cần thiết
            if (!project.resolve("pom.xml").toFile().exists()) {
                throw new RuntimeException("Khong tim thay pom.xml!");
            }

            if (!project.resolve("mvnw.cmd").toFile().exists()) {
                throw new RuntimeException("Khong tim thay mvnw.cmd!");
            }

            if (!Path.of(CLOUDFLARED).toFile().exists()) {
                throw new RuntimeException(
                    "Khong tim thay cloudflared.exe!"
                );
            }

            // 3. Khởi động Spring Boot
            System.out.println("\n[1/3] Dang khoi dong Spring Boot...");

            ProcessBuilder springBuilder = new ProcessBuilder(
                "cmd.exe", "/c", "mvnw.cmd spring-boot:run"
            );

            springBuilder.directory(project.toFile());
            springBuilder.redirectErrorStream(true);

            spring = springBuilder.start();

            Thread springLogs = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(spring.getInputStream()))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        System.out.println("[SPRING] " + line);
                    }

                } catch (Exception e) {
                    System.out.println(
                        "Khong doc duoc log Spring Boot."
                    );
                }
            });

            springLogs.setDaemon(true);
            springLogs.start();

            // 4. Chờ website sẵn sàng
            System.out.println("\n[2/3] Dang cho website khoi dong...");

            boolean ready = false;

            for (int i = 0; i < 180; i++) {

                if (!spring.isAlive()) {
                    throw new RuntimeException(
                        "Spring Boot da dung. Hay kiem tra log."
                    );
                }

                try {
                    HttpURLConnection connection =
                        (HttpURLConnection) new URL(LOCAL_URL)
                            .openConnection();

                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.setRequestMethod("GET");

                    int status = connection.getResponseCode();
                    connection.disconnect();

                    if (status > 0) {
                        ready = true;
                        break;
                    }

                } catch (Exception ignored) {
                    // Website chưa sẵn sàng, tiếp tục chờ
                }

                Thread.sleep(1000);
            }

            if (!ready) {
                throw new RuntimeException(
                    "Spring Boot khoi dong qua lau!"
                );
            }

            System.out.println("Website da san sang!");

            // 5. Khởi động Cloudflare Tunnel
            System.out.println("\n[3/3] Dang ket noi Cloudflare...");

            ProcessBuilder tunnelBuilder = new ProcessBuilder(
                CLOUDFLARED,
                "tunnel",
                "--url",
                LOCAL_URL
            );

            tunnelBuilder.redirectErrorStream(true);
            cloudflare = tunnelBuilder.start();

            Pattern pattern = Pattern.compile(
                "https://[a-zA-Z0-9-]+\\.trycloudflare\\.com"
            );

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cloudflare.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    System.out.println("[CLOUDFLARE] " + line);

                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        System.out.println("\n================================");
                        System.out.println("       WEBSITE DA SAN SANG");
                        System.out.println("================================");
                        System.out.println("LOCAL: " + LOCAL_URL);
                        System.out.println("PUBLIC: " + matcher.group());
                        System.out.println("================================");
                    }
                }
            }

            if (!cloudflare.isAlive()) {
                throw new RuntimeException(
                    "Cloudflare Tunnel da dung!"
                );
            }

        } catch (Exception e) {

            System.out.println("\nLOI: " + e.getMessage());
            e.printStackTrace();

        } finally {

            stop(cloudflare);
            stop(spring);
        }
    }

    // Tự động tìm thư mục gốc chứa pom.xml và mvnw.cmd
    private static Path findProjectDirectory() {

        Path current = Path.of("").toAbsolutePath();

        while (current != null) {

            boolean hasPom =
                current.resolve("pom.xml").toFile().exists();

            boolean hasMavenWrapper =
                current.resolve("mvnw.cmd").toFile().exists();

            if (hasPom && hasMavenWrapper) {
                return current;
            }

            current = current.getParent();
        }

        throw new RuntimeException(
            "Khong tim thay thu muc project (pom.xml va mvnw.cmd)!"
        );
    }

    // Dừng tiến trình và các tiến trình con
    private static void stop(Process process) {

        if (process != null && process.isAlive()) {

            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();

            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.descendants()
                        .forEach(ProcessHandle::destroyForcibly);

                    process.destroyForcibly();
                }

            } catch (InterruptedException e) {

                process.descendants()
                    .forEach(ProcessHandle::destroyForcibly);

                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}