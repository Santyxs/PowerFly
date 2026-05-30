package pwf.xenova.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;

public class WorldGuardFlags {

    public static boolean isFallDamageDenied(Player player) {
        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer();

            if (container == null) return false;

            RegionQuery query = container.createQuery();

            return !query.testState(
                    BukkitAdapter.adapt(player.getLocation()),
                    WorldGuardPlugin.inst().wrapPlayer(player),
                    Flags.FALL_DAMAGE
            );

        } catch (Exception e) {
            return false;
        }
    }
}