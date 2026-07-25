package com.voro.houseassessment.util

import com.voro.houseassessment.data.RoomRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentEngineTest {
    @Test
    fun emptyRoomHasNoScore() {
        val result = AssessmentEngine.evaluate(RoomRecord())
        assertEquals(null, result.score)
        assertEquals("待评估", result.label)
    }

    @Test
    fun goodRoomScoresHighly() {
        val room = RoomRecord(
            rentMonthly = 4000.0,
            targetBudget = 5000.0,
            orientation = "南",
            hasBalcony = true,
            waterQuality = 5,
            acLevel = 3,
            hasWasher = true,
            spaceRating = 5,
            outletRating = 4,
            noiseRating = 5,
            lightingRating = 5,
            ventilationRating = 4,
            cleanlinessRating = 5,
            dampMoldRating = 5,
            bathroomRating = 4,
            kitchenRating = 4,
            securityRating = 5,
            transitRating = 5,
            neighborhoodRating = 4,
            networkRating = 4,
            storageRating = 4,
            furnishingRating = 4,
            leaseRiskRating = 5
        )
        val result = AssessmentEngine.evaluate(room)
        assertTrue((result.score ?: 0.0) >= 4.3)
        assertTrue(result.pros.isNotEmpty())
    }

    @Test
    fun severeRiskCapsScore() {
        val room = RoomRecord(
            noiseRating = 5,
            lightingRating = 5,
            securityRating = 1,
            dampMoldRating = 5,
            leaseRiskRating = 5
        )
        val result = AssessmentEngine.evaluate(room)
        assertTrue((result.score ?: 5.0) <= 2.5)
        assertTrue(result.warnings.isNotEmpty())
    }
}
