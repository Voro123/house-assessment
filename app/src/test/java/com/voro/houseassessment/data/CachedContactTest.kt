package com.voro.houseassessment.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedContactTest {
    @Test
    fun `phone formatting does not create duplicate identities`() {
        val first = CachedContact(name = "中介 A", phone = "+60 12-345 6789")
        val second = CachedContact(name = "另一备注", phone = "60123456789")

        assertEquals(first.identityKey(), second.identityKey())
    }

    @Test
    fun `name and channel identify contacts without a phone`() {
        val first = CachedContact(name = "房东李先生", channel = "WeChat: landlord-li")
        val second = CachedContact(name = " 房东李先生 ", channel = "wechat: LANDLORD-LI")

        assertEquals(first.identityKey(), second.identityKey())
    }

    @Test
    fun `blank notes alone are not treated as a reusable contact`() {
        assertFalse(CachedContact(notes = "周五再问").isMeaningful())
        assertTrue(CachedContact(name = "房东").isMeaningful())
    }
}
