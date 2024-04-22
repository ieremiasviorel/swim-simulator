package device.process_actor

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.ActorRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import device.data.DeviceNotAvailable
import device.data.DeviceTimedOut
import device.data.TemperatureNotAvailable
import device.data.TemperatureRead
import device.message.Command
import device.message.ReadGroupTemperatureResponse
import device.message.ReadTemperatureResponse
import device.process_actor.DeviceGroupQuery
import java.time.Duration


class DeviceGroupQueryTest {
    @Test
    fun testReadGroupTemperatureForDevicesWithReadings() {
        // given
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)
        val device1 = testKit.createTestProbe(Command::class.java)
        val device2 = testKit.createTestProbe(Command::class.java)

        val deviceGroupQueryActor = testKit.spawn(
            DeviceGroupQuery.create(
                1L,
                "group",
                readGroupTemperatureProbe.ref as ActorRef<Command>,
                mapOf("device1" to device1.ref, "device2" to device2.ref),
                Duration.ofSeconds(3),
            )
        )

        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device1", 1.0))
        deviceGroupQueryActor.tell(ReadTemperatureResponse(1L, "device2", 2.0))

        // when
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(1L, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureRead(1.0),
            "device2" to TemperatureRead(2.0),
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    @Test
    fun testReadGroupTemperatureForDevicesWithNoReadings() {
        // given
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)
        val device1 = testKit.createTestProbe(Command::class.java)
        val device2 = testKit.createTestProbe(Command::class.java)

        val deviceGroupQueryActor = testKit.spawn(
            DeviceGroupQuery.create(
                1L,
                "group",
                readGroupTemperatureProbe.ref as ActorRef<Command>,
                mapOf("device1" to device1.ref, "device2" to device2.ref),
                Duration.ofSeconds(3),
            )
        )

        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device1", null))
        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device2", 2.0))

        // when
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(1L, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureNotAvailable.INSTANCE,
            "device2" to TemperatureRead(2.0),
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    @Test
    fun testReadGroupTemperatureForDevicesStoppedBefore() {
        // given
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)
        val device1 = testKit.createTestProbe(Command::class.java)
        val device2 = testKit.createTestProbe(Command::class.java)

        val deviceGroupQueryActor = testKit.spawn(
            DeviceGroupQuery.create(
                1L,
                "group",
                readGroupTemperatureProbe.ref as ActorRef<Command>,
                mapOf("device1" to device1.ref, "device2" to device2.ref),
                Duration.ofSeconds(3),
            )
        )

        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device1", 1.0))
        device2.stop()

        // when
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(1L, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureRead(1.0),
            "device2" to DeviceNotAvailable.INSTANCE,
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    @Test
    fun testReadGroupTemperatureForDevicesStoppedAfter() {
        // given
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)
        val device1 = testKit.createTestProbe(Command::class.java)
        val device2 = testKit.createTestProbe(Command::class.java)

        val deviceGroupQueryActor = testKit.spawn(
            DeviceGroupQuery.create(
                1L,
                "group",
                readGroupTemperatureProbe.ref as ActorRef<Command>,
                mapOf("device1" to device1.ref, "device2" to device2.ref),
                Duration.ofSeconds(3),
            )
        )

        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device1", 1.0))
        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device2", 2.0))
        device2.stop()

        // when
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(1L, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureRead(1.0),
            "device2" to TemperatureRead(2.0),
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    @Test
    fun testReadGroupTemperatureForDevicesThatTimeout() {
        // given
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)
        val device1 = testKit.createTestProbe(Command::class.java)
        val device2 = testKit.createTestProbe(Command::class.java)

        val deviceGroupQueryActor = testKit.spawn(
            DeviceGroupQuery.create(
                1L,
                "group",
                readGroupTemperatureProbe.ref as ActorRef<Command>,
                mapOf("device1" to device1.ref, "device2" to device2.ref),
                Duration.ofSeconds(2),
            )
        )

        deviceGroupQueryActor.tell(ReadTemperatureResponse(0L, "device1", 1.0))
        // no reply from device2

        // when
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(1L, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureRead(1.0),
            "device2" to DeviceTimedOut.INSTANCE,
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    companion object {
        val testKit = TestKitJunitResource()
    }
}
