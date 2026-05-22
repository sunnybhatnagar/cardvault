package com.sunnyb.cardvault.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRationaleTest {

    @Test
    fun `when permission is granted return LAUNCH_CAMERA`() {
        val action = getPermissionAction(isGranted = true, shouldShowRationale = false)
        assertEquals(PermissionAction.LAUNCH_CAMERA, action)
    }

    @Test
    fun `when denied with rationale return SHOW_RATIONALE`() {
        val action = getPermissionAction(isGranted = false, shouldShowRationale = true)
        assertEquals(PermissionAction.SHOW_RATIONALE, action)
    }

    @Test
    fun `when denied permanently return SHOW_SETTINGS_PROMPT`() {
        val action = getPermissionAction(isGranted = false, shouldShowRationale = false)
        assertEquals(PermissionAction.SHOW_SETTINGS_PROMPT, action)
    }

    @Test
    fun `granted takes priority over rationale flag`() {
        val action = getPermissionAction(isGranted = true, shouldShowRationale = true)
        assertEquals(PermissionAction.LAUNCH_CAMERA, action)
    }
}