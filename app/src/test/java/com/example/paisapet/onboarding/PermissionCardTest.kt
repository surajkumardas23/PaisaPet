package com.example.paisapet.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCardTest {

    private val bools = listOf(false, true)

    @Test fun notRequired_isNotNeeded_whateverElse() {
        for (granted in bools) for (rationale in bools) for (denied in bools) {
            assertEquals(PermissionCard.NOT_NEEDED, permissionCard(false, granted, rationale, denied))
        }
    }

    @Test fun granted_isAllowed_evenIfDeniedThisVisit() {
        assertEquals(PermissionCard.ALLOWED, permissionCard(true, true, false, false))
        assertEquals(PermissionCard.ALLOWED, permissionCard(true, true, false, true))
        assertEquals(PermissionCard.ALLOWED, permissionCard(true, true, true, true))
    }

    @Test fun notGranted_noRationale_notDenied_isAsk() =
        assertEquals(PermissionCard.ASK, permissionCard(true, false, false, false))

    @Test fun notGranted_rationale_isDeniedCanAsk() {
        assertEquals(PermissionCard.DENIED_CAN_ASK, permissionCard(true, false, true, false))
        assertEquals(PermissionCard.DENIED_CAN_ASK, permissionCard(true, false, true, true))
    }

    @Test fun notGranted_noRationale_deniedThisVisit_isBlocked() =
        assertEquals(PermissionCard.BLOCKED, permissionCard(true, false, false, true))

    @Test fun notificationsNotRequiredOnAndroid12() = assertFalse(notificationsRequired(32))

    @Test fun notificationsRequiredOnAndroid13() = assertTrue(notificationsRequired(33))

    @Test fun smsDeniedCard_shownForDeniedAndBlocked() {
        assertTrue(showSmsDeniedCard(PermissionCard.DENIED_CAN_ASK))
        assertTrue(showSmsDeniedCard(PermissionCard.BLOCKED))
    }

    @Test fun smsDeniedCard_hiddenForAskAndAllowed() {
        assertFalse(showSmsDeniedCard(PermissionCard.ASK))
        assertFalse(showSmsDeniedCard(PermissionCard.ALLOWED))
    }

    @Test fun restrictedHint_blockedOnAndroid13() = assertTrue(showRestrictedHint(PermissionCard.BLOCKED, 33))

    @Test fun restrictedHint_notOnAndroid12() = assertFalse(showRestrictedHint(PermissionCard.BLOCKED, 32))

    @Test fun restrictedHint_notWhenCanAskAgain() = assertFalse(showRestrictedHint(PermissionCard.DENIED_CAN_ASK, 34))
}
