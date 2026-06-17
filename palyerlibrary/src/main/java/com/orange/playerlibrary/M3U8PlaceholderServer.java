package com.orange.playerlibrary;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Minimal local HTTP server to serve placeholder.ts for M3U8 ad removal.
 */
public class M3U8PlaceholderServer {

    private static final String TAG = "M3U8PlaceholderServer";

    private static final String PATH_PLACEHOLDER = "placeholder.ts";

    private static final String PATH_CLEANED_PREFIX = "cleaned/";


    private static final Charset HTTP_HEADER_CHARSET = StandardCharsets.ISO_8859_1;

    private static volatile M3U8PlaceholderServer sInstance;

    private final Context mContext;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private final ExecutorService mClientExecutor = Executors.newCachedThreadPool();
    private final Set<String> mRegisteredCleanedFiles = ConcurrentHashMap.newKeySet();

    private volatile File mCurrentCleanedM3u8File;


    private ServerSocket mServerSocket;
    private Thread mAcceptThread;
    private int mPort;

    public static M3U8PlaceholderServer getInstance(Context context) {
        if (sInstance == null) {
            synchronized (M3U8PlaceholderServer.class) {
                if (sInstance == null) {
                    sInstance = new M3U8PlaceholderServer(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private M3U8PlaceholderServer(Context context) {
        mContext = context;
    }

    public void startIfNeeded() {
        if (mRunning.get()) {
            return;
        }
        synchronized (this) {
            if (mRunning.get()) {
                return;
            }
            try {
                mServerSocket = new ServerSocket(0);
                mPort = mServerSocket.getLocalPort();
                mRunning.set(true);
                mAcceptThread = new Thread(this::acceptLoop, "M3U8PlaceholderServer");
                mAcceptThread.start();
                Log.d(TAG, "Started on port=" + mPort);
            } catch (IOException e) {
                Log.e(TAG, "start failed", e);
            }
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
        if (mAcceptThread != null) {
            mAcceptThread.interrupt();
            mAcceptThread = null;
        }
    }

    public int getPort() {
        return mPort;
    }

    public String getPlaceholderUrl() {
        if (!mRunning.get()) {
            startIfNeeded();
        }
        return "http://127.0.0.1:" + mPort + "/" + PATH_PLACEHOLDER;
    }

    public String getCleanedM3u8Url(File cleanedM3u8File) {
        if (!mRunning.get()) {
            startIfNeeded();
        }
        if (cleanedM3u8File == null) {
            Log.w(TAG, "getCleanedM3u8Url called with null file");
            return null;
        }

        String fileName = cleanedM3u8File.getName();
        mRegisteredCleanedFiles.add(fileName);
        mCurrentCleanedM3u8File = cleanedM3u8File;

        String url = "http://127.0.0.1:" + mPort + "/" + PATH_CLEANED_PREFIX + fileName;
        Log.d(TAG, "getCleanedM3u8Url: " + url + " for file=" + cleanedM3u8File.getAbsolutePath()
                + ", exists=" + cleanedM3u8File.exists());
        return url;
    }

    public String getCurrentCleanedM3u8Path() {
        return mCurrentCleanedM3u8File != null ? mCurrentCleanedM3u8File.getAbsolutePath() : null;
    }


    private void acceptLoop() {
        while (mRunning.get()) {
            try {
                Socket client = mServerSocket.accept();
                mClientExecutor.execute(() -> handleClient(client));
            } catch (IOException e) {
                if (mRunning.get()) {
                    Log.e(TAG, "accept failed", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();

            String request = readRequest(input);
            if (request == null || request.isEmpty()) {
                safeClose(client);
                return;
            }

            String[] lines = request.split("\r\n");
            String[] first = lines[0].split(" ");
            if (first.length < 2) {
                safeClose(client);
                return;
            }

            String method = first[0];
            String path = first[1];
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            Log.d(TAG, "Request: " + method + " /" + path);

            File file;
            String contentType;
            if (PATH_PLACEHOLDER.equals(path)) {
                file = new File(mContext.getCacheDir(), "m3u8_cache/" + PATH_PLACEHOLDER);
                contentType = "video/MP2T";
                Log.d(TAG, "Placeholder file path: " + file.getAbsolutePath() + ", exists=" + file.exists());
            } else if (path.startsWith(PATH_CLEANED_PREFIX)) {
                file = getCleanedM3u8FileFromPath(path);
                contentType = "application/x-mpegURL";
                Log.d(TAG, "Resolved cleaned m3u8 path /" + path + " -> "
                        + (file != null ? file.getAbsolutePath() : "null")
                        + ", exists=" + (file != null && file.exists()));
            } else {

                writeNotFound(output);
                safeClose(client);
                return;
            }

            if (file == null || !file.exists()) {
                if (file == null) {
                    Log.w(TAG, "File not set for path: " + path);
                }
                Log.w(TAG, "File not found for path: " + path + ", file=" + (file == null ? "null" : file.getAbsolutePath()));
                writeNotFound(output);
                safeClose(client);
                return;
            }

            long fileSize = file.length();
            long rangeStart = 0;
            long rangeEnd = -1;
            boolean hasRange = false;

            for (String line : lines) {
                if (line.toLowerCase(Locale.US).startsWith("range:")) {
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

            if (rangeEnd < 0) {
                rangeEnd = fileSize - 1;
            }
            if (rangeEnd >= fileSize) {
                rangeEnd = fileSize - 1;
            }
            long contentLength = rangeEnd - rangeStart + 1;

            if (hasRange) {
                Log.d(TAG, "Range: bytes=" + rangeStart + "-" + rangeEnd + "/" + fileSize);
            }

            String header;
            if (hasRange) {
                header = String.format(Locale.US,
                        "HTTP/1.1 206 Partial Content\r\n" +
                                "Content-Type: %s\r\n" +
                                "Content-Length: %d\r\n" +
                                "Content-Range: bytes %d-%d/%d\r\n" +
                                "Accept-Ranges: bytes\r\n" +
                                "Cache-Control: no-cache\r\n" +
                                "Pragma: no-cache\r\n" +
                                "Server: M3U8PlaceholderServer\r\n" +
                                "Connection: close\r\n\r\n",
                        contentType, contentLength, rangeStart, rangeEnd, fileSize);
                Log.d(TAG, "Respond 206 for /" + path + ", length=" + contentLength);
            } else {
                header = String.format(Locale.US,
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: %s\r\n" +
                                "Content-Length: %d\r\n" +
                                "Accept-Ranges: bytes\r\n" +
                                "Cache-Control: no-cache\r\n" +
                                "Pragma: no-cache\r\n" +
                                "Server: M3U8PlaceholderServer\r\n" +
                                "Connection: close\r\n\r\n",
                        contentType, fileSize);
                Log.d(TAG, "Respond 200 for /" + path + ", length=" + fileSize);
            }
            output.write(header.getBytes(HTTP_HEADER_CHARSET));

            if (!"HEAD".equalsIgnoreCase(method)) {
                sendFile(output, file, rangeStart, contentLength);
            }

            output.flush();
            safeClose(client);
        } catch (Throwable t) {
            Log.e(TAG, "handleClient failed", t);
            safeClose(client);
        }
    }

    private File getCleanedM3u8FileFromPath(String path) {
        String fileName = path.substring(PATH_CLEANED_PREFIX.length());
        if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            Log.w(TAG, "Invalid cleaned m3u8 path: " + path);
            return null;
        }
        if (!mRegisteredCleanedFiles.contains(fileName)) {
            Log.w(TAG, "Unregistered cleaned m3u8 file requested: " + fileName);
            return null;
        }

        File file = new File(mContext.getCacheDir(), "m3u8_cache/" + fileName);
        if (file.exists()) {
            mCurrentCleanedM3u8File = file;
        }
        return file;
    }

    private String readRequest(InputStream input) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            if (sb.indexOf("\r\n\r\n") >= 0) {
                break;
            }
        }
        return sb.toString();
    }


    private void writeNotFound(OutputStream output) throws IOException {
        String resp = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n";
        output.write(resp.getBytes(StandardCharsets.UTF_8));
    }

    private void sendFile(OutputStream output, File file, long start, long length) throws IOException {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
        try {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = length;
            while (remaining > 0 && mRunning.get()) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) {
                    break;
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        } finally {
            raf.close();
        }
    }

    private void safeClose(Socket s) {
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }
}
