package dev.xexanos.mealie.core.ui.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface NavigationCommand

class NavigationManager {
    private val _commands = MutableSharedFlow<NavigationCommand>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    fun navigate(command: NavigationCommand) {
        _commands.tryEmit(command)
    }
}
