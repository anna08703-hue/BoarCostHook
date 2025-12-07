package exonihility.client.module.combat;

import exonihility.client.AllyshipClient;
import exonihility.client.config.Config;
import exonihility.client.module.Extension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.world.ClientWorld;

import java.util.UUID;
import java.util.List;

public class KillAura extends Extension {
    private UUID targetPlayerUUID = null;
    private int attackCooldown = 0;
    private final List<String> protectedPlayers = AllyshipClient.ProtectedPlayers; // Access to protected players
    private final boolean isModEnabled = AllyshipClient.isModEnabled; // Check if protection mod is enabled

    public KillAura() {
        super("KillAura", "Attacks players in your aura", Extension.Category.COMBAT);
    }

    @Override
    public void tick() {
        // Check if the protection mod is enabled
        if (!isModEnabled) {
            System.out.println("[KillAura] Mod is disabled, exiting tick.");
            return;
        }

        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;

        if (world == null || localPlayer == null) {
            System.out.println("[KillAura] World or local player is null, exiting tick.");
            return;
        }

        double range = 4.0;
        int attackDelay = Config.getInt(Config.ATTACK_DELAY);

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        // Attempt to find and set a new target if none exists
        if (targetPlayerUUID == null) {
            Entity nearestPlayer = findNearestNonProtectedPlayer(localPlayer, range);
            if (nearestPlayer != null) {
                targetPlayerUUID = nearestPlayer.getUuid();
                System.out.println("[KillAura] New target set: " + targetPlayerUUID);
            } else {
                System.out.println("[KillAura] No valid non-protected target found.");
                return;
            }
        }

        // Retrieve the current target by UUID
        Entity targetPlayer = world.getPlayerByUuid(targetPlayerUUID);

        // Reset target if invalid, protected, or the local player itself
        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer == localPlayer || isProtected(targetPlayer)) {
            System.out.println("[KillAura] Target is invalid, protected, or same as local player. Resetting target.");
            targetPlayerUUID = null;
            return;
        }

        // Attack the target if within range
        if (localPlayer.squaredDistanceTo(targetPlayer) <= range * range) {
            // Simulate animations and execute the attack
            localPlayer.swingHand(Hand.MAIN_HAND, true);
            localPlayer.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            Vec3d entityPos = targetPlayer.getPos();
            world.addParticle(ParticleTypes.SWEEP_ATTACK, entityPos.getX(), entityPos.getY(), entityPos.getZ(), 0.0, 0.0, 0.0);
            MinecraftClient.getInstance().interactionManager.attackEntity(localPlayer, targetPlayer);
            attackCooldown = attackDelay;
            System.out.println("[KillAura] Attacked target: " + targetPlayerUUID);
        } else {
            System.out.println("[KillAura] Target out of range.");
        }

        super.tick();
    }

    /**
     * Checks if the entity is protected based on player names from the configuration.
     *
     * @param targetPlayer The player entity to check.
     * @return True if the player is protected, false otherwise.
     */
    private boolean isProtected(Entity targetPlayer) {
        // Convert the player's name to lowercase to ensure consistent matching
        String targetName = targetPlayer.getName().getString().toLowerCase();
        boolean isProtected = protectedPlayers.stream().anyMatch(name -> name.equalsIgnoreCase(targetName));

        if (isProtected) {
            System.out.println("[KillAura] Player " + targetName + " is protected and cannot be targeted.");
        }

        return isProtected;
    }

    /**
     * Finds the nearest non-protected player entity within a specified range.
     *
     * @param localPlayer The local player as the reference point.
     * @param range       The search range.
     * @return The nearest valid non-protected player entity, or null if none are found.
     */
    private Entity findNearestNonProtectedPlayer(ClientPlayerEntity localPlayer, double range) {
        ClientWorld world = localPlayer.clientWorld;
        Box searchBox = localPlayer.getBoundingBox().expand(range);

        // Filter entities to only include valid, non-protected players
        return world.getEntitiesByClass(Entity.class, searchBox, entity -> {
            if (entity == localPlayer) return false; // Exclude the local player
            if (!(entity instanceof OtherClientPlayerEntity || entity instanceof ClientPlayerEntity)) return false; // Exclude non-player entities
            return !isProtected(entity); // Exclude protected players
        }).stream().findFirst().orElse(null);
    }
}
