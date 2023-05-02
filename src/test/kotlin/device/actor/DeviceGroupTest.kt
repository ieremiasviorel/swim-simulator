package device.actor

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import device.actor.DeviceGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import device.data.TemperatureNotAvailable
import device.data.TemperatureRead
import device.message.*
import java.util.stream.Collectors
import java.util.stream.Stream


class DeviceGroupTest {
    @Test
    fun testRegisterDeviceWithMultipleDevices() {
        // given
        val registerDeviceProbe = testKit.createTestProbe(RegisterDeviceResponse::class.java)
        val recordTemperatureProbe = testKit.createTestProbe(RecordTemperatureResponse::class.java)
        val deviceGroupActor = testKit.spawn(DeviceGroup.create("group"))

        // when
        val registerDeviceRequest1 = RegisterDeviceRequest("group", "device1", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest1)
        val registerDeviceResponse1 = registerDeviceProbe.receiveMessage()
        val registerDeviceRequest2 = RegisterDeviceRequest("group", "device2", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest2)
        val registerDeviceResponse2 = registerDeviceProbe.receiveMessage()
        // then
        assertNotEquals(registerDeviceResponse1.device, registerDeviceResponse2.device)

        // when
        val recordTemperatureRequest1 = RecordTemperatureRequest(0L, 1.0, recordTemperatureProbe.ref)
        registerDeviceResponse1.device.tell(recordTemperatureRequest1)
        val recordTemperatureResponse1 = recordTemperatureProbe.receiveMessage()
        // then
        assertEquals(recordTemperatureRequest1.requestId, recordTemperatureResponse1.requestId)

        // when
        val recordTemperatureRequest2 = RecordTemperatureRequest(1L, 2.0, recordTemperatureProbe.ref)
        registerDeviceResponse2.device.tell(recordTemperatureRequest2)
        val recordTemperatureResponse2 = recordTemperatureProbe.receiveMessage()
        // then
        assertEquals(recordTemperatureRequest2.requestId, recordTemperatureResponse2.requestId)
    }

    @Test
    fun testRegisterDeviceReturnSameActorForSameDeviceId() {
        // given
        val registerDeviceProbe = testKit.createTestProbe(RegisterDeviceResponse::class.java)
        val deviceGroupActor = testKit.spawn(DeviceGroup.create("group"))

        // when
        val registerDeviceRequest1 = RegisterDeviceRequest("group", "device", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest1)
        val registerDeviceResponse1 = registerDeviceProbe.receiveMessage()
        val registerDeviceRequest2 = RegisterDeviceRequest("group", "device", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest2)
        val registerDeviceResponse2 = registerDeviceProbe.receiveMessage()

        // then
        assertEquals(registerDeviceResponse1.device, registerDeviceResponse2.device)
    }

    @Test
    fun testListDevices() {
        // given
        val registerDeviceProbe = testKit.createTestProbe(RegisterDeviceResponse::class.java)
        val listDevicesProbe = testKit.createTestProbe(ListDevicesResponse::class.java)
        val deviceGroupActor = testKit.spawn(DeviceGroup.create("group"))

        val registerDeviceRequest1 = RegisterDeviceRequest("group", "device1", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest1)
        registerDeviceProbe.receiveMessage()
        val registerDeviceRequest2 = RegisterDeviceRequest("group", "device2", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest2)
        registerDeviceProbe.receiveMessage()

        // when
        val listDevicesRequest = ListDevicesRequest(0L, "group", listDevicesProbe.ref)
        deviceGroupActor.tell(listDevicesRequest)
        val listDevicesResponse = listDevicesProbe.receiveMessage()

        // then
        assertEquals(listDevicesRequest.requestId, listDevicesResponse.requestId)
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), listDevicesResponse.deviceIds)
    }

    @Test
    fun testListDevicesAfterOneShutsDown() {
        // given
        val registerDeviceProbe = testKit.createTestProbe(RegisterDeviceResponse::class.java)
        val listDevicesProbe = testKit.createTestProbe(ListDevicesResponse::class.java)
        val deviceGroupActor = testKit.spawn(DeviceGroup.create("group"))

        val registerDeviceRequest1 = RegisterDeviceRequest("group", "device1", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest1)
        val registerDeviceResponse1 = registerDeviceProbe.receiveMessage()
        val registerDeviceRequest2 = RegisterDeviceRequest("group", "device2", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest2)
        registerDeviceProbe.receiveMessage()

        // when
        val listDevicesRequest1 = ListDevicesRequest(0L, "group", listDevicesProbe.ref)
        deviceGroupActor.tell(listDevicesRequest1)
        val listDevicesResponse1 = listDevicesProbe.receiveMessage()

        // then
        assertEquals(listDevicesRequest1.requestId, listDevicesResponse1.requestId)
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), listDevicesResponse1.deviceIds)

        // when
        val deviceToShutdown = registerDeviceResponse1.device
        deviceToShutdown.tell(PassivateDeviceRequest.INSTANCE)

        // then
        registerDeviceProbe.expectTerminated(deviceToShutdown, registerDeviceProbe.remainingOrDefault)

        // using awaitAssert to retry because it might take longer for the DeviceGroup Actor
        // to see the Terminated, that order is undefined
        registerDeviceProbe.awaitAssert {
            // when
            val listDevicesRequest2 = ListDevicesRequest(1L, "group", listDevicesProbe.ref)
            deviceGroupActor.tell(listDevicesRequest2)
            val listDevicesResponse2 = listDevicesProbe.receiveMessage()

            // then
            assertEquals(listDevicesRequest2.requestId, listDevicesResponse2.requestId)
            assertEquals(Stream.of("device2").collect(Collectors.toSet()), listDevicesResponse2.deviceIds)
        }
    }

    @Test
    fun testCollectTemperatureFromAllDevices() {
        // given
        val registerDeviceProbe = testKit.createTestProbe(RegisterDeviceResponse::class.java)
        val recordTemperatureProbe = testKit.createTestProbe(RecordTemperatureResponse::class.java)
        val readGroupTemperatureProbe = testKit.createTestProbe(ReadGroupTemperatureResponse::class.java)

        val deviceGroupActor = testKit.spawn(DeviceGroup.create("group"))

        val registerDeviceRequest1 = RegisterDeviceRequest("group", "device1", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest1)
        val deviceActor1 = registerDeviceProbe.receiveMessage().device
        val registerDeviceRequest2 = RegisterDeviceRequest("group", "device2", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest2)
        val deviceActor2 = registerDeviceProbe.receiveMessage().device
        val registerDeviceRequest3 = RegisterDeviceRequest("group", "device3", registerDeviceProbe.ref)
        deviceGroupActor.tell(registerDeviceRequest3)

        val recordTemperatureRequest1 = RecordTemperatureRequest(0L, 1.0, recordTemperatureProbe.ref)
        deviceActor1.tell(recordTemperatureRequest1)
        val recordTemperatureResponse1 = recordTemperatureProbe.receiveMessage()
        assertEquals(recordTemperatureRequest1.requestId, recordTemperatureResponse1.requestId)
        val recordTemperatureRequest2 = RecordTemperatureRequest(1L, 2.0, recordTemperatureProbe.ref)
        deviceActor2.tell(recordTemperatureRequest2)
        val recordTemperatureResponse2 = recordTemperatureProbe.receiveMessage()
        assertEquals(recordTemperatureRequest2.requestId, recordTemperatureResponse2.requestId)
        // No record temperature for device 3

        // when
        val readGroupTemperatureRequest = ReadGroupTemperatureRequest(
            0L, "group", readGroupTemperatureProbe.ref
        )
        deviceGroupActor.tell(readGroupTemperatureRequest)
        val readGroupTemperatureResponse = readGroupTemperatureProbe.receiveMessage()

        // then
        assertEquals(readGroupTemperatureRequest.requestId, readGroupTemperatureResponse.requestId)
        val expectedTemperatures = mapOf(
            "device1" to TemperatureRead(1.0),
            "device2" to TemperatureRead(2.0),
            "device3" to TemperatureNotAvailable.INSTANCE,
        )
        assertEquals(expectedTemperatures, readGroupTemperatureResponse.temperatures)
    }

    companion object {
        val testKit = TestKitJunitResource()
    }
}
