package swim_simulator

import akka.actor.typed.ActorSystem
import device.actor.DeviceGroup
import device.message.ReadGroupTemperatureRequest
import device.message.RegisterDeviceRequest
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SwimSimulatorApplication

fun main(args: Array<String>) {
    val deviceGroup = ActorSystem.create(DeviceGroup.create("group-1"), "device-group")

    val registerDeviceRequest1 = RegisterDeviceRequest("group-1", "device-1-1", deviceGroup)
    deviceGroup.tell(registerDeviceRequest1)

    val registerDeviceRequest2 = RegisterDeviceRequest("group-1", "device-1-2", deviceGroup)
    deviceGroup.tell(registerDeviceRequest2)

    val registerDeviceRequest3 = RegisterDeviceRequest("group-1", "device-1-3", deviceGroup)
    deviceGroup.tell(registerDeviceRequest3)

    val readGroupTemperatureRequest = ReadGroupTemperatureRequest(1, "group-1", deviceGroup)
    deviceGroup.tell(readGroupTemperatureRequest)

    runApplication<SwimSimulatorApplication>(*args)
}
