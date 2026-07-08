// This is free and unencumbered software released into the public domain.
package net.trueog.havenog;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.FlagValueCalculator;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.regions.RegionQuery.QueryOption;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import io.papermc.paper.event.entity.EntityMoveEvent;

final class HavenListener implements Listener {

    private final RegionQuery query;
    private final StateFlag havenFlag;
    private final boolean despawnHostileMobs;
    private final boolean preventHostileTargeting;
    private final double targetClearRadius;
    private final double targetClearRadiusSquared;

    HavenListener(HavenOG plugin, RegionQuery query, StateFlag havenFlag) {

        this.query = query;
        this.havenFlag = havenFlag;

        despawnHostileMobs = plugin.getConfig().getBoolean("despawn-hostile-mobs", true);
        preventHostileTargeting = plugin.getConfig().getBoolean("prevent-hostile-targeting", true);
        targetClearRadius = Math.max(0.0D, plugin.getConfig().getDouble("target-clear-radius", 64.0D));
        targetClearRadiusSquared = targetClearRadius * targetClearRadius;

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHostileMove(EntityMoveEvent event) {

        if (!despawnHostileMobs || !event.hasChangedBlock() || !isHostile(event.getEntity())) {

            return;

        }

        if (!isHaven(event.getTo()) || hasAllowedCombatTarget(event.getEntity())) {

            return;

        }

        event.setCancelled(true);
        event.getEntity().remove();

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHostileTarget(EntityTargetEvent event) {

        final Entity target = event.getTarget();

        if (!preventHostileTargeting || target == null || !isProtectedTarget(target) || !isHostile(event.getEntity())) {

            return;

        }

        if (!isHaven(target.getLocation())) {

            return;

        }

        event.setCancelled(true);

        clearMobTarget(event.getEntity(), target);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProtectedEntityMove(EntityMoveEvent event) {

        if (!preventHostileTargeting || !event.hasChangedBlock() || isHostile(event.getEntity())
                || !isProtectedTarget(event.getEntity()) || !isHaven(event.getTo()))
        {

            return;

        }

        clearNearbyTargets(event.getEntity());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProtectedEntityTeleport(EntityTeleportEvent event) {

        final Location to = event.getTo();

        if (!preventHostileTargeting || to == null || isHostile(event.getEntity())
                || !isProtectedTarget(event.getEntity()) || !isHaven(to))
        {

            return;

        }

        clearNearbyTargets(event.getEntity());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (!preventHostileTargeting || !changedBlock(event.getFrom(), event.getTo()) || !isHaven(event.getTo())) {

            return;

        }

        clearNearbyTargets(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        final Location to = event.getTo();

        if (!preventHostileTargeting || to == null || !isHaven(to)) {

            return;

        }

        clearNearbyTargets(event.getPlayer());

    }

    private boolean isHaven(Location location) {

        final ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location), QueryOption.SORT);
        if (regions.isVirtual() || regions.size() == 0) {

            return false;

        }

        final int highestPriority = highestPriority(regions);
        State state = null;

        for (final ProtectedRegion region : regions) {

            if (FlagValueCalculator.getPriorityOf(region) != highestPriority) {

                continue;

            }

            state = StateFlag.combine(state, FlagValueCalculator.getEffectiveFlagOf(region, havenFlag, null));
            if (state == State.DENY) {

                return false;

            }

        }

        return state == State.ALLOW;

    }

    private static int highestPriority(ApplicableRegionSet regions) {

        int highestPriority = Integer.MIN_VALUE;

        for (final ProtectedRegion region : regions) {

            final int priority = FlagValueCalculator.getPriorityOf(region);
            if (priority > highestPriority) {

                highestPriority = priority;

            }

        }

        return highestPriority;

    }

    private static boolean isHostile(Entity entity) {

        return entity instanceof Enemy;

    }

    private static boolean isProtectedTarget(Entity entity) {

        return entity instanceof Player || isPlayerPet(entity) || isPlayerMount(entity);

    }

    private boolean hasAllowedCombatTarget(Entity entity) {

        if (!(entity instanceof Mob mob)) {

            return false;

        }

        final Entity target = mob.getTarget();
        return target != null && isProtectedTarget(target) && !isHaven(target.getLocation());

    }

    private static boolean isPlayerPet(Entity entity) {

        return entity instanceof Tameable tameable && tameable.isTamed() && tameable.getOwnerUniqueId() != null;

    }

    private static boolean isPlayerMount(Entity entity) {

        return entity.getPassengers().stream().filter(passenger -> passenger instanceof Player).findFirst()
                .map(passenger -> true).orElse(false);

    }

    private static boolean changedBlock(Location from, Location to) {

        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ() || from.getWorld() != to.getWorld();

    }

    private void clearNearbyTargets(Entity protectedTarget) {

        final Location targetLocation = protectedTarget.getLocation();
        final World world = protectedTarget.getWorld();
        world.getNearbyEntities(targetLocation, targetClearRadius, targetClearRadius, targetClearRadius)
                .forEach(nearbyEntity ->
                {

                    if (nearbyEntity instanceof Mob mob && isHostile(mob) && protectedTarget.equals(mob.getTarget())
                            && nearbyEntity.getLocation().distanceSquared(targetLocation) <= targetClearRadiusSquared)
                {

                        mob.setTarget(null);

                    }

                });

    }

    private void clearMobTarget(Entity entity, Entity protectedTarget) {

        if (entity instanceof Mob mob && protectedTarget.equals(mob.getTarget())) {

            mob.setTarget(null);

        }

    }

}
