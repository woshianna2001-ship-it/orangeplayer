package com.orange.playerlibrary.torrent;

import android.util.Log;

import org.libtorrent4j.TorrentHandle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalHttpProxy {

    private static final String TAG = "LocalHttpProxy";

    private final TorrentHandle mTorrent;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    private ServerSocket mServerSocket;
    private Thread mThread;
    private int mPort;

    public LocalHttpProxy(TorrentHandle torrent) {
        mTorrent = torrent;
    }

    public void start() {
        if (mRunning.get()) return;
        try {
            mServerSocket = new ServerSocket(0);
            mPort = mServerSocket.getLocalPort();
            mRunning.set(true);
            mThread = new Thread(this::loop, "TorrentHttpProxy");
            mThread.start();
        } catch (IOException e) {
            Log.e(TAG, "start failed", e);
        }
    }

    public void stop() {
        mRunning.set(false);
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException ignored) {
            }
            mServerSocket = null;
        }
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    public int getPort() {
        return mPort;
    }

    public String getUrl(String fileName) {
        return "http://127.0.0.1:" + mPort + "/" + fileName;
    }

    private void loop() {
        while (mRunning.get()) {
            try {
                Socket client = mServerSocket.accept();
                new Thread(() -> handle(client), "TorrentHttpProxyClient").start();
            } catch (IOException e) {
                if (mRunning.get()) {
                    Log.e(TAG, "accept failed", e);
                }
            }
        }
    }

    private void handle(Socket client) {
        try {
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();

            String request = readRequest(input);
            if (request == null || request.isEmpty()) {
                client.close();
                return;
            }

            String[] lines = request.split("\r\n");
            String[] first = lines[0].split(" ");
            if (first.length < 2) {
                client.close();
                return;
            }

            String method = first[0];
            String path = first[1];
            if (path.startsWith("/")) path = path.substring(1);

            long rangeStart = 0;
            long rangeEnd = -1;
            boolean hasRange = false;

            for (String line : lines) {
                if (line.toLowerCase().startsWith("range:")) {
                    hasRange = true;
                    String rangeValue = line.substring(6).trim();
                    if (rangeValue.startsWith("bytes=")) {
                        String[] parts = rangeValue.substring(6).split("-");
                        if (parts.length >= 1 && !parts[0].isEmpty()) {
                            rangeStart = Long.parseLong(parts[0]);
                        }
                        if (parts.length >= 2 && !parts[1].isEmpty()) {
                            rangeEnd = Long.parseLong(parts[1]);
                        }
                    }
                }
            }

            File saveDir = getSaveDir();
            File file = new File(saveDir, path);
            if (!file.exists()) {
                writeNotFound(output);
                client.close();
                return;
            }

            long fileSize = file.length();
            if (rangeEnd < 0) rangeEnd = fileSize - 1;
            if (rangeEnd >= fileSize) rangeEnd = fileSize - 1;
            long contentLength = rangeEnd - rangeStart + 1;

            String header;
            if (hasRange) {
                header = String.format(Locale.US,
                        "HTTP/1.1 206 Partial Content\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Length: %d\r\n" +
                                "Content-Range: bytes %d-%d/%d\r\n" +
                                "Accept-Ranges: bytes\r\n" +
                                "Connection: close\r\n\r\n",
                        contentLength, rangeStart, rangeEnd, fileSize);
            } else {
                header = String.format(Locale.US,
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Length: %d\r\n" +
                                "Accept-Ranges: bytes\r\n" +
                                "Connection: close\r\n\r\n",
                        fileSize);
            }
            output.write(header.getBytes());

            if (!"HEAD".equalsIgnoreCase(method)) {
                sendFile(output, file, rangeStart, contentLength);
            }

            output.flush();
            client.close();

        } catch (Throwable t) {
            Log.e(TAG, "handle failed", t);
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private File getSaveDir() {
        try {
            // 1.2.x savePath() returns String
            String p = mTorrent.savePath();
            if (p != null) {
                return new File(p);
            }
        } catch (Throwable ignored) {
        }
        // fallback
        return new File("/");
    }

    private void writeNotFound(OutputStream output) throws IOException {
        String resp = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n";
        output.write(resp.getBytes());
    }

    private String readRequest(InputStream input) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, "UTF-8"));
            if (sb.indexOf("\r\n\r\n") >= 0) break;
        }
        return sb.toString();
    }

    private void sendFile(OutputStream output, File file, long start, long length) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = length;
            while (remaining > 0 && mRunning.get()) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) break;
                output.write(buffer, 0, read);
                remaining -= read;
            }
        } finally {
            raf.close();
        }
    }
}
