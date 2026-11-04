package com.cinema.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a URL that a phone can use to reach this local application. */
@Component
public class PublicUrlResolver {

    private static final String CLOUDFLARED_METRICS = "http://127.0.0.1:20241/metrics";
    private static final Pattern QUICK_TUNNEL_HOST = Pattern.compile(
            "userHostname=\\\"(https://[^\\\"]+\\.trycloudflare\\.com)\\\"");

    private final String configuredPublicBaseUrl;

    public PublicUrlResolver(@Value("${app.public-base-url:}") String configuredPublicBaseUrl) {
        this.configuredPublicBaseUrl = trimTrailingSlashes(configuredPublicBaseUrl);
    }

    public String resolve(HttpServletRequest request) {
        String requestBase = publicRequestBase(request);
        if (requestBase != null) return requestBase;

        // Quick Tunnel hostnames change after every restart. Reading cloudflared's
        // local metrics makes newly rendered QR codes follow the current tunnel.
        String quickTunnel = discoverQuickTunnelUrl();
        if (quickTunnel != null) return quickTunnel;

        if (!configuredPublicBaseUrl.isBlank()) return configuredPublicBaseUrl;

        String scheme = request.getScheme();
        String host = findLanIpv4Address();
        if (host == null) host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port) + request.getContextPath();
    }

    private String publicRequestBase(HttpServletRequest request) {
        String scheme = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        if (scheme.isBlank()) scheme = request.getScheme();
        String host = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (host.isBlank()) host = firstHeaderValue(request.getHeader("Host"));
        if (host.isBlank()) host = request.getServerName();
        String hostname = host.split(":", 2)[0];
        if ("localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname)
                || "0.0.0.0".equals(hostname)) return null;
        return scheme + "://" + host + request.getContextPath();
    }

    private String discoverQuickTunnelUrl() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(CLOUDFLARED_METRICS).toURL().openConnection();
            connection.setConnectTimeout(300);
            connection.setReadTimeout(500);
            try (var reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = QUICK_TUNNEL_HOST.matcher(line);
                    if (matcher.find()) return trimTrailingSlashes(matcher.group(1));
                }
            }
        } catch (Exception ignored) {
            // cloudflared may be stopped or still registering; use the next fallback.
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private static String firstHeaderValue(String value) {
        return value == null ? "" : value.split(",", 2)[0].trim();
    }

    private static String trimTrailingSlashes(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    private static String findLanIpv4Address() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                for (var address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
            // Fall through to the request host.
        }
        return null;
    }
}
