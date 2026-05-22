package com.sunnyb.cardvault.util

enum class PermissionAction {
    LAUNCH_CAMERA,
    SHOW_RATIONALE,
    SHOW_SETTINGS_PROMPT
}

fun getPermissionAction(isGranted: Boolean, shouldShowRationale: Boolean): PermissionAction {
    if (isGranted) return PermissionAction.LAUNCH_CAMERA
    return if (shouldShowRationale) PermissionAction.SHOW_RATIONALE
    else PermissionAction.SHOW_SETTINGS_PROMPT
}