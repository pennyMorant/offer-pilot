package com.overud.domain

import com.overud.model.*
import com.overud.platform.*
import com.overud.valuation.*
import com.overud.acceptance.*
import com.overud.region.RegionCell

import org.junit.Assert.*
import org.junit.Test

class ValueEngineTest {
    private val offer = Offer("a", Platform.UBER_EATS, 1000, 1200, 8046, 1500, reliable = true)
    @Test fun goodOfferPassesAndComputesGrossAndContribution() {
        val result = EligibilityEvaluator.assess(offer, Policy(costCentsPerMile = 35), 1000)
        assertTrue(result.reasons.toString(), result.eligible)
        assertEquals(2880.0, result.valuation.grossCentsPerHour!!, 0.01)
        assertEquals(1025.0, result.valuation.contributionCents!!, 0.1)
    }
    @Test fun thresholdEqualityPasses() {
        assertTrue(EligibilityEvaluator.assess(offer.copy(distanceMeters = 1609, durationSeconds = 3600),
            Policy(minimumCents = 1200, minimumCentsPerMile = 1200, minimumCentsPerHour = 1200, maximumSeconds = 3600), 1000).eligible)
    }
    @Test fun missingStaleAndUnreliableNeverPass() {
        assertFalse(EligibilityEvaluator.assess(offer.copy(distanceMeters = null), Policy(), 1000).eligible)
        assertFalse(EligibilityEvaluator.assess(offer, Policy(), 11001).eligible)
        assertFalse(EligibilityEvaluator.assess(offer.copy(reliable = false), Policy(), 1000).eligible)
    }
    @Test fun excludedDestinationRequiresKnownLocation() {
        val policy = Policy(excludedDestinations = setOf("Airport"))
        assertFalse(EligibilityEvaluator.assess(offer, policy, 1000).eligible)
        assertFalse(EligibilityEvaluator.assess(offer.copy(destination = "Airport Rd"), policy, 1000).eligible)
    }
    @Test fun addonsAndOtherPlatformsDoNotSlipThrough() {
        assertFalse(EligibilityEvaluator.assess(offer.copy(isAddon = true), Policy(), 1000).eligible)
        assertFalse(EligibilityEvaluator.assess(offer, Policy(enabledPlatforms = setOf(Platform.DOORDASH)), 1000).eligible)
    }
    @Test fun parserRejectsAmbiguousAmountsAndMissingTotals() {
        val parsed = OfferParser.parse(Platform.UBER_EATS, "Accept $12.00 5 mi 25 min", 1000)!!
        assertFalse(parsed.reliable)
        val ambiguous = OfferParser.parse(Platform.DOORDASH, "Accept $12.00 $8.00 total 5 mi 25 min", 1000)!!
        assertNull(ambiguous.amountCents)
        assertFalse(ambiguous.reliable)
        assertTrue(OfferParser.parse(Platform.DOORDASH, "Accept $12.00 total 5 mi 25 min", 1000)!!.reliable)
    }
    @Test fun gateSerializesPlatformsAndDoesNotRetryUnknownDispatch() {
        val gate = AcceptanceGate()
        val decision = EligibilityEvaluator.assess(offer, Policy(), 1000)
        assertFalse(gate.reserve(offer, decision, false, true))
        assertTrue(gate.reserve(offer, decision, true, true))
        gate.dispatched()
        assertFalse(gate.reserve(offer.copy(id = "b", platform = Platform.DOORDASH), decision, true, true))
        gate.userResolved()
        assertFalse(gate.reserve(offer, decision, true, true))
    }
    @Test fun gateRestoredAfterCrashBlocksNewOrders() {
        val gate = AcceptanceGate(AcceptanceState.OUTCOME_UNKNOWN)
        val decision = EligibilityEvaluator.assess(offer, Policy(), 1000)
        assertFalse(gate.reserve(offer, decision, true, true))
        gate.userResolved()
        assertTrue(gate.reserve(offer, decision, true, true))
        gate.releaseBeforeDispatch()
        assertEquals(AcceptanceState.IDLE, gate.state)
    }
    @Test fun countdownChangesDoNotCreateANewCanonicalOffer() {
        val one = OfferParser.parse(Platform.DOORDASH, "Accept $12.00 total 5 mi 25 min\n10", 1000)!!
        val two = OfferParser.parse(Platform.DOORDASH, "Accept $12.00 total 5 mi 25 min\n9", 2000)!!
        assertEquals(one.id, two.id)
    }
    @Test fun kilometerConversionAndNegativeRegionCells() {
        val parsed = OfferParser.parse(Platform.UBER_EATS, "Accept $12.00 total 8.5 km 25 min", 1000)!!
        assertEquals(8500L, parsed.distanceMeters)
        val cell = RegionCell.at(34.055, -118.245)
        assertEquals("3405:-11825", cell.key)
    }
    @Test fun pickupLimitNeedsPickupEvidenceAndForeignCurrenciesFail() {
        assertFalse(EligibilityEvaluator.assess(offer, Policy(maximumPickupMeters = 3000), 1000).eligible)
        assertFalse(EligibilityEvaluator.assess(offer.copy(currency = "CAD"), Policy(), 1000).eligible)
    }
}
