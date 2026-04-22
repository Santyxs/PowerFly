package pwf.xenova.utils;

import org.bukkit.Bukkit;
import pwf.xenova.PowerFly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UpdateChecker {

    private static final String API_URL = "https://api.spiget.org/v2/resources/%s/versions/latest";
    private static final String SPIGOT_URL = "https://www.spigotmc.org/resources/%s";
    private static final String USER_AGENT = "PowerFly-UpdateChecker";
    private static final int TIMEOUT = 5000;

    private final PowerFly plugin;
    private final String resourceId;

    private volatile String latestVersion;
    private volatile String downloadUrl;
    private volatile boolean updateAvailable;

    public UpdateChecker(PowerFly plugin, String resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates() {
        checkForUpdates((Consumer<Boolean>) null);
    }

    public void checkForUpdates(Runnable callback) {
        checkForUpdates(callback != null ? success -> { if (success) callback.run(); } : null);
    }

    public void checkForUpdates(Consumer<Boolean> callback) {
        CompletableFuture.runAsync(() -> {
            boolean success = false;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URI(String.format(API_URL, resourceId)).toURL().openConnection();
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);

                String response;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    response = reader.lines().collect(Collectors.joining());
                }

                latestVersion = response.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
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