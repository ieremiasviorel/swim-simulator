package device.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import device.message.*
import device.process_actor.DeviceGroupQuery
import java.time.Duration

class DeviceGroup private constructor(
    context: ActorContext<Command>,
    private val groupId: String
) : AbstractBehavior<Command>(context) {
    init {
        context.log.info("$groupId started")
    }

    private val deviceIdToActor: MutableMap<String, ActorRef<Command>> = HashMap()

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(RegisterDeviceRequest::class.java, this::onRegisterDevice)
            .onMessage(TerminateDeviceRequest::class.java, this::onTerminateDevice)
            .onMessage(ListDevicesRequest::class.java, this::onListDevices)
            .onMessage(ReadGroupTemperatureRequest::class.java, this::onReadGroupTemperature)
            .onMessage(ReadGroupTemperatureResponse::class.java, this::onReadGroupTemperatureResponse)
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onRegisterDevice(request: RegisterDeviceRequest): DeviceGroup {
        val existingDeviceActor = deviceIdToActor[request.deviceId]
        if (existingDeviceActor != null) {
            request.replyTo.tell(RegisterDeviceResponse(existingDeviceActor))
        } else {
            context.log.info("Creating device ${request.deviceId}")
            val newDeviceActor = context.spawn(Device.create(groupId, request.deviceId), "device-" + request.deviceId)
            context.watchWith(newDeviceActor, TerminateDeviceRequest(groupId, request.deviceId))
            deviceIdToActor[request.deviceId] = newDeviceActor
            request.replyTo.tell(RegisterDeviceResponse(newDeviceActor))
        }
        return this
    }

    private fun onTerminateDevice(request: TerminateDeviceRequest): DeviceGroup {
        context.log.info("Device ${request.deviceId} terminated")
        deviceIdToActor.remove(request.deviceId)
        return this
    }

    private fun onListDevices(request: ListDevicesRequest): DeviceGroup {
        request.replyTo.tell(ListDevicesResponse(request.requestId, deviceIdToActor.keys))
        return this
    }

    private fun onReadGroupTemperature(request: ReadGroupTemperatureRequest): DeviceGroup {
        context.spawnAnonymous(
            DeviceGroupQuery.create(
                request.requestId,
                groupId,
                request.replyTo,
                deviceIdToActor,
                Duration.ofSeconds(3),
            )
        )
        return this
    }

    private fun onReadGroupTemperatureResponse(response: ReadGroupTemperatureResponse): DeviceGroup {
        context.log.info("Read group temperature ${response.temperatures.mapValues { it.value }}")
        return this
    }

    private fun onPostStop(): DeviceGroup {
        context.log.info("$groupId stopped")
        return this
    }

    companion object {
        fun create(groupId: String): Behavior<Command> {
            return Behaviors.setup { context -> DeviceGroup(context, groupId) }
        }
    }
}
