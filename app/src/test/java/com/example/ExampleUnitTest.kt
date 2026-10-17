package com.example

import com.example.model.BeaconSignal
import com.example.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun beaconSignal_signalPercent_isWithinBounds() {
        val strongSignal = BeaconSignal(
            id = "test_1",
            name = "TestBeacon",
            type = SignalType.BLE,
            identifier = "AA:BB:CC:DD:EE:FF",
            rssi = -30
        )
        assertEquals(100, strongSignal.signalPercent)

        val weakSignal = BeaconSignal(
            id = "test_2",
            name = "TestBeacon2",
            type = SignalType.WIFI,
            identifier = "11:22:33:44:55:66",
            rssi = -100
        )
        assertEquals(0, weakSignal.signalPercent)
    }

    @Test
    fun beaconSignal_estimatedDistance_increasesWithLowerRssi() {
        val nearSignal = BeaconSignal(
            id = "near",
            name = "Near",
            type = SignalType.BLE,
            identifier = "00:11:22:33:44:55",
            rssi = -45
        )
        val farSignal = BeaconSignal(
            id = "far",
            name = "Far",
            type = SignalType.BLE,
            identifier = "00:11:22:33:44:66",
            rssi = -85
        )

        assertTrue(nearSignal.estimatedDistanceMeters < farSignal.estimatedDistanceMeters)
    }
}
