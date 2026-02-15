package pwf.xenova.managers;

import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;

public class FlyTimeOnGroundManager {

    private final PowerFly plugin;
    private boolean decreaseOnGround;

    public FlyTimeOnGroundManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.decreaseOnGround = plugin.getConfig().getBoolean("decrease-flytime-on-ground", false);
    }

    public boolean shouldDecreaseFlyTime(Player player) {
        if (decreaseOnGround) {
            return true;
        }

        return player.isFlying();
    }

    public boolean isDecreaseOnGroundEnabled() {
        return decreaseOnGround;
    }

    public void setDecreaseOnGround(boolean enabled) {
        this.decreaseOnGround = enabled;
    }
}