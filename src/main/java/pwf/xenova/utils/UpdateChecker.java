package pwf.xenova.utils;

import org.bukkit.Bukkit;
import pwf.xenova.PowerFly;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UpdateChecker {

    private static final String API_URL = "https://api.spiget.org/v2/resources/%s/versions/latest";
    private static final String SPIGOT_URL = "https://www.spigotmc.org/resources/%s";
    private static final String USER_AGENT = "PowerFly-UpdateChecker";
    private static final int TIMEOUT = 5000;

    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private final PowerFly plugin;
    private final String resourceId;

    private volatile String latestVersion;
    private volatile String downloadUrl;
    private volatile boolean updateAvailable;

    public UpdateChecker(PowerFly plugin, String resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates(Consumer<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            boolean success = false;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URI(String.format(API_URL, resourceId)).toURL().openConnection();
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);

                int statusCode = conn.getResponseCode();

                if (statusCode != HttpURLConnection.HTTP_OK) {
                    String errorBody = readStream(conn.getErrorStream());
                    plugin.getLogger().warning("UpdateChecker: API returned HTTP " + statusCode + " for resource " + resourceId + ". Response: " + truncate(errorBody));
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                    }
                    return;
                }

                String response = readStream(conn.getInputStream());

                String parsed = parseVersion(response);

                if (parsed == null) {
                    plugin.getLogger().warning("UpdateChecker: Could not parse version from API response. Raw response: "
                            + truncate(response));
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                    }
                    return;
                }

                latestVersion = parsed;
                downloadUrl = String.format(SPIGOT_URL, resourceId);

                String normalizedLatest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
                String currentVersion = getCurrentVersion();

                updateAvailable = !currentVersion.equalsIgnoreCase(normalizedLatest);
                success = true;

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }

            if (callback != null) {
                final boolean result = success;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            }
        });
    }

    private String readStream(InputStream stream) throws java.io.IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private String parseVersion(String json) {
        if (json == null || json.isEmpty()) return null;

        Matcher matcher = NAME_PATTERN.matcher(json);
        if (!matcher.find()) return null;

        String version = matcher.group(1).trim();
        return version.isEmpty() ? null : version;
    }

    @SuppressWarnings("deprecation")
    private String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion != null ? latestVersion : "Unknown";
    }

    public String getDownloadUrl() {
        return downloadUrl != null ? downloadUrl : "Unknown";
    }
}