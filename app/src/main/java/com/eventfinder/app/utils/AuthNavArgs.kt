package com.eventfinder.app.utils

/**
 * Shared navigation argument keys/values for auth and onboarding flows.
 */
object AuthNavArgs {
    const val USER_TYPE = "USER_TYPE"
    const val TARGET = "TARGET"
    const val IS_EDIT_MODE = "IS_EDIT_MODE"
    const val FLOW_SOURCE = "FLOW_SOURCE"
}

object AuthNavTargets {
    const val USER = "USER"
    const val ORGANIZER = "ORGANIZER"
    const val ADMIN = "ADMIN"
}

object AuthFlowSource {
    const val LOGIN = "LOGIN"
    const val REGISTER = "REGISTER"
}

object AuthPendingStep {
    const val FILL_PROFILE_LOGIN = "FILL_PROFILE_LOGIN"
    const val FILL_PROFILE_REGISTER = "FILL_PROFILE_REGISTER"
    const val CHOOSE_INTERESTS = "CHOOSE_INTERESTS"
}


