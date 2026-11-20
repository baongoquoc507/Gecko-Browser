package com.example.geckobrowser;

import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Tls12SocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    private Tls12SocketFactory(SSLSocketFactory base) {
        this.delegate = base;
    }

    public static void enableTls12OnPreLollipop(HttpsURLConnection connection) {
        if (connection == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, null);
            connection.setSSLSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));
        } catch (NoSuchAlgorithmException | KeyManagementException ignored) {
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return patch(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return patch(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return patch(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket patch(Socket s) {
        if (s instanceof SSLSocket) {
            SSLSocket ssl = (SSLSocket) s;
            String[] supported = ssl.getSupportedProtocols();
            if (supported != null) {
                List<String> enabled = new ArrayList<>();
                for (String p : supported) {
                    if ("TLSv1.3".equals(p) || "TLSv1.2".equals(p)) {
                        enabled.add(p);
                    }
                }
                if (!enabled.isEmpty()) {
                    ssl.setEnabledProtocols(enabled.toArray(new String[0]));
                }
            }
        }
        return s;
    }
}
