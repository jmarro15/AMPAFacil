// File: app/src/main/java/com/ampafacil/app/navigation/Routes.kt
package com.ampafacil.app.navigation

object Routes {
    const val START = "start"
    const val AUTH = "auth"
    const val AMPA_CODE = "ampa_code"
    const val CREATE_AMPA = "create_ampa"
    const val FAMILY_CHILDREN = "family_children"
    const val AMPA_SPLASH = "ampa_splash"
    const val APPEARANCE = "appearance"
    const val HOME = "home"
    const val PERSONAL_DATA = "personal_data"
    const val FAMILY_DIRECTORY = "family_directory"
    const val BOARD_MANAGEMENT = "board_management"

    const val INITIAL_BOARD_INVITES = "initial_board_invites/{ampaCode}/{creatorRole}"

    fun initialBoardInvites(ampaCode: String, creatorRole: String): String {
        return "initial_board_invites/$ampaCode/$creatorRole"
    }
}
