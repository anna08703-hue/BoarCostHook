package net.zeta.hacks.module.combat; // <--- Adjust your package, you badass

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.zeta.hacks.module.Module; // <--- Assuming you have a base Module class

import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    // --- Configuration (Adjust these fucking values) ---
    private final double range = 4.5;
    private final int hitDelay = 4; // Ticks between hits (1 is max fucking speed)
    // --- End Config ---

    private int tickCounter = 0;

    public KillAura() {
        super("Killaura", "Automatically attacks nearby entities.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        // Only run this shit if the module is enabled and the player exists
        if (!this.isEnabled() || mc.player == null || mc.world == null) {
            return;
        }

        // Fucking wait for the hit delay
        if (++tickCounter < hitDelay) {
            return;
        }

        tickCounter = 0; // Reset the counter for the next glorious hit

        // 1. Find the target!
        LivingEntity target = findTarget();

        if (target != null) {
            // 2. Aim at the target (Optional but makes it less fucking obvious)
            // You'd need a separate mixin or packet hook to change the player's rotation, 
            // but for a basic one, we'll just fucking hit 'em.

            // 3. Fucking attack the target
            mc.interactionManager.attackEntity(mc.player, target);
            
            // Swing the hand to show the attack animation (makes it look less shitty)
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private LivingEntity findTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Fucking calculate the search bounding box around the player
        Box searchBox = mc.player.getBoundingBox().expand(range, range, range);
        
        // Get all living entities within the range
        List<LivingEntity> potentialTargets = mc.world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> 
            // Exclude the fucking player (us) and dead/spectating entities
            entity != mc.player &&
            entity.isAlive() &&
            !mc.player.isTeammate(entity) && // Exclude teammates (if you give a shit)
            !entity.isSpectator() &&
            mc.player.squaredDistanceTo(entity) <= range * range && // Final distance check
            // Only attack other fucking players or hostile mobs, exclude armor stands and other bullshit
            (entity instanceof PlayerEntity || entity.isAttackable())
        );

        // Sort by distance to find the closest one to attack first, the fucking coward
        return potentialTargets.stream()
            .min(Comparator.comparingDouble(mc.player::squaredDistanceTo))
            .orElse(null);
    }

    // You'd add the necessary enable/disable logic here, but you're Alpha, you know this shit.
}
