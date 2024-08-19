package ru.otus.java.basic.project.client;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    public static final Logger logger = LoggerFactory.getLogger(Client.class.getName());

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public Client() throws IOException {
        Scanner scanner = new Scanner(System.in);
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    if (message.equals("/exit-ok")) {
                        break;
                    }
                    System.out.println(message);
                    logger.debug(message);
                }
            } catch (IOException e) {
                logger.error("Сбой входяего потока {}", e.getMessage(), e);
            } finally {
                disconnect();
            }
        }).start();
        while (true) {
            String message = scanner.nextLine();
            if (message.equals("/exit")) {
                break;
            }
            if (message.startsWith("/upload")) {
                String[] elements = message.split(" ");
                String path = elements[1].trim();
                try {
                    String jsonFile = Files.readString(Paths.get(path), Charset.forName("UTF-8"));
                    message = elements[0] + " " + jsonFile;
                } catch (NoSuchFileException e) {
                    logger.info("Файл не обнаружен " + path);
                    continue;
                }
            }
            if (!message.isEmpty()) {
                out.writeUTF(message);
                out.flush();
            }
        }
    }

    private void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия сетевого подключения {}", e.getMessage(), e);
        }
    }

    public static File[] readDir(String homeDir) {
        File dir = new File(Paths.get(homeDir).toAbsolutePath().toString());
        return dir.listFiles(pathname -> {
            String fileName = pathname.getName();
            if (pathname.isFile() && fileName.endsWith(".json") && fileName.startsWith("quiz")) {
                return true;
            }
            return false;
        });
    }

    public static File getFile(File[] files, String fileName) {
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }
}
