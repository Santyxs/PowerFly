package pwf.xenova.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import pwf.xenova.PowerFly;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final PowerFly plugin;
    private final String resourceId;

    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable;

    public UpdateChecker(PowerFly plugin, String resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(Runnable callback) {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = new URI("https://api.spiget.org/v2/resources/" + resourceId + "/versions/latest");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("User-Agent", "PowerFly-UpdateChecker");

                JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                latestVersion = json.get("name").getAsString();
                downloadUrl = "https://www.spigotmc.org/resources/" + resourceId;

                String normalizedLatest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
                String currentVersion = getCurrentVersion();

                updateAvailable = !currentVersion.equalsIgnoreCase(normalizedLatest);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }

            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, callback);
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
