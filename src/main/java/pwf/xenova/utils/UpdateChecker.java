package pwf.xenova.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String repoOwner;
    private final String repoName;

    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable;

    public UpdateChecker(JavaPlugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(Runnable callback) {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = new URI("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                latestVersion = json.get("tag_name").getAsString();
                downloadUrl = json.get("html_url").getAsString();

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
