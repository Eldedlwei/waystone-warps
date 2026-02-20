package dev.mizarc.waystonewarps.infrastructure.services.scheduling

import dev.mizarc.waystonewarps.application.services.scheduling.Task
import org.bukkit.scheduler.BukkitTask

class TaskBukkit(private val task: BukkitTask) : Task {
    override fun cancel() {
        task.cancel()
    }
}
