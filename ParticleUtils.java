package com.diasmp.diaswords.swords;

import com.diasmp.diaswords.DiaSwords;
import com.diasmp.diaswords.utils.ParticleUtils;
import com.diasmp.diaswords.utils.SoundUtils;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Vampire Sword: passive effect that procs on every melee hit.
 * Heals the attacker for a configurable percentage of damage dealt,
 * capped at a configurable max per hit.
 */
public class VampireSword {

    private final DiaSwords plugin;

    public VampireSword(DiaSwords plugin) {
        this.plugin = plugin;
    }

    /**
     * Called on every melee hit landed with this sword, after damage is applied.
     * @param damageDealt the final damage amount dealt to the target.
     */
    public void onHit(Player attacker, LivingEntity target, double damageDealt) {
        // Guard against healing a player who died from counter-damage (e.g. thorns)
        // that resolved before this MONITOR-priority handler ran.
        if (!attacker.isOnline() || attacker.isDead()) return;

        double healPercent = plugin.getConfig().getDouble("vampire-sword.heal-percent", 0.35);
        double maxHeal = plugin.getConfig().getDouble("vampire-sword.max-heal-per-hit", 3.0);
        boolean particles = plugin.getConfig().getBoolean("vampire-sword.particle-drain", true);
        boolean healSound = plugin.getConfig().getBoolean("vampire-sword.heal-sound", true);

        double healAmount = Math.min(damageDealt * healPercent, maxHeal);
        if (healAmount <= 0) return;

        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH) != null
                ? attacker.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;

        // Nothing to heal — skip the particle/sound feedback too, since no
        // actual life was restored (avoids misleading effects at full health).
        if (attacker.getHealth() >= maxHealth) return;

        double newHealth = Math.min(attacker.getHealth() + healAmount, maxHealth);
        attacker.setHealth(newHealth);

        if (particles) {
            ParticleUtils.vampireDrain(target.getLocation().add(0, 1, 0));
        }
        if (healSound) {
            SoundUtils.play(plugin, attacker, attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 2.0f);
        }
    }
}
