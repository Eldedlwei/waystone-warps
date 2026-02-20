package dev.mizarc.waystonewarps.infrastructure.services

import dev.mizarc.waystonewarps.application.services.PlayerAttributeService
import dev.mizarc.waystonewarps.application.services.PlayerParticleService
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*


class PlayerParticleServiceBukkit(private val plugin: JavaPlugin,
                                  private val playerAttributeService: PlayerAttributeService): PlayerParticleService {
    private val activeParticles: MutableMap<UUID, BukkitTask> = mutableMapOf()

    override fun spawnPreParticles(playerId: UUID) {
        var teleportTime = playerAttributeService.getTeleportTimer(playerId) * 20
        val particles = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val player = Bukkit.getPlayer(playerId)
            if (player == null || !player.isOnline) {
                removeParticles(playerId)
                return@Runnable
            }

            teleportTime -= 1
            if (teleportTime == 80) {
                player.world.playSound(player.location, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.0f)
            }

            player.world.spawnParticle(Particle.PORTAL, player.location, 1)
        }, 0L, 1L)
        activeParticles[playerId] = particles
    }

    override fun spawnPostParticles(playerId: UUID) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val playerLocation = player.location
        playerLocation.world.spawnParticle(Particle.REVERSE_PORTAL, playerLocation, 100, 0.5, 1.0, 0.5, 1.0, null as Any?, true)

        // Play teleport sound
        playerLocation.world.playSound(playerLocation, Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 1.0f)
    }

    override fun removeParticles(playerId: UUID) {
        val particles = activeParticles[playerId] ?: return
        particles.cancel()
        activeParticles.remove(playerId)
    }
}
