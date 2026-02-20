package dev.mizarc.waystonewarps.infrastructure.services.scheduling

import dev.mizarc.waystonewarps.application.services.scheduling.SchedulerService
import dev.mizarc.waystonewarps.application.services.scheduling.Task
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class SchedulerServiceBukkit(private val plugin: Plugin): SchedulerService {
    override fun schedule(delayTicks: Long, task: () -> Unit): Task {
        val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { task() }, delayTicks)
        return TaskBukkit(bukkitTask)
    }
}
