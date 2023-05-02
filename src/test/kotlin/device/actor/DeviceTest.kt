package device.actor

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import device.actor.Device
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import device.message.ReadTemperatureRequest
import device.message.ReadTemperatureResponse
import device.message.RecordTemperatureRequest
import device.message.RecordTemperatureResponse

class DeviceTest {

    @Test
    fun testReadTemperatureWhenNoTemperatureHasBeenRecorded() {
        // given
        val readTemperatureProbe = testKit.createTestProbe(ReadTemperatureResponse::class.java)
        val deviceActor = testKit.spawn(Device.create("group", "device"))

        //when
        val readTemperatureRequest = ReadTemperatureRequest(42L, readTemperatureProbe.ref)
        deviceActor.tell(readTemperatureRequest)
        val readTemperatureResponse = readTemperatureProbe.receiveMessage()

        // then
        assertEquals(readTemperatureRequest.requestId, readTemperatureResponse.requestId)
        assertNull(readTemperatureResponse.value)
    }

    @Test
    fun testReadTemperatureWhenTemperaturesHaveBeenRecorded() {
        // given
        val recordTemperatureProbe = testKit.createTestProbe(RecordTemperatureResponse::class.java)
        val readTemperatureProbe = testKit.createTestProbe(ReadTemperatureResponse::class.java)
        val deviceActor = testKit.spawn(Device.create("group", "device"))

        // when
        val recordTemperatureRequest1 = RecordTemperatureRequest(1L, 24.0, recordTemperatureProbe.ref)
        deviceActor.tell(recordTemperatureRequest1)
        val recordTemperatureResponse1 = recordTemperatureProbe.receiveMessage()
        // then
        assertEquals(recordTemperatureRequest1.requestId, recordTemperatureResponse1.requestId)

        // when
        val readTemperatureRequest1 = ReadTemperatureRequest(2L, readTemperatureProbe.ref)
        deviceActor.tell(readTemperatureRequest1)
        val readTemperatureResponse1 = readTemperatureProbe.receiveMessage()
        // then
        assertEquals(readTemperatureRequest1.requestId, readTemperatureResponse1.requestId)
        assertEquals(recordTemperatureRequest1.value, readTemperatureResponse1.value)

        // when
        val recordTemperatureRequest2 = RecordTemperatureRequest(3L, 55.0, recordTemperatureProbe.ref)
        deviceActor.tell(recordTemperatureRequest2)
        val recordTemperatureResponse2 = recordTemperatureProbe.receiveMessage()
        // then
        assertEquals(recordTemperatureRequest2.requestId, recordTemperatureResponse2.requestId)

        // when
        val readTemperatureRequest2 = ReadTemperatureRequest(4L, readTemperatureProbe.ref)
        deviceActor.tell(readTemperatureRequest2)
        val readTemperatureResponse2 = readTemperatureProbe.receiveMessage()
        // then
        assertEquals(readTemperatureRequest2.requestId, readTemperatureResponse2.requestId)
        assertEquals(recordTemperatureRequest2.value, readTemperatureResponse2.value)
    }

    companion object {
        val testKit = TestKitJunitResource()
    }
}
