package device.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import device.message.*

class DeviceManager private constructor(
    context: ActorContext<Command>
) : AbstractBehavior<Command>(context) {
    init {
        context.log.info("Started")
    }

    private val groupIdToActor: MutableMap<String, ActorRef<Command>> = HashMap()

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(RegisterDeviceRequest::class.java, this::onRegisterDevice)
            .onMessage(RegisterDeviceResponse::class.java, this::onRegisterDeviceResponse)
            .onMessage(TerminateDeviceGroupRequest::class.java, this::onTerminateDeviceGroup)
            .onMessage(ListDevicesRequest::class.java, this::onListDevices)
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onRegisterDevice(request: RegisterDeviceRequest): DeviceManager {
        val groupId = request.groupId
        val existingDeviceGroupActor = groupIdToActor[groupId]
        if (existingDeviceGroupActor != null) {
            existingDeviceGroupActor.tell(request)
        } else {
            context.log.info("Creating device group $groupId")
            val newDeviceGroupActor = context.spawn(DeviceGroup.create(groupId), "group-$groupId")
            context.watchWith(newDeviceGroupActor, TerminateDeviceGroupRequest(groupId))
            newDeviceGroupActor.tell(request)
            groupIdToActor[groupId] = newDeviceGroupActor
        }
        return this
    }

    private fun onRegisterDeviceResponse(response: RegisterDeviceResponse): DeviceManager {
        return this
    }

    private fun onTerminateDeviceGroup(request: TerminateDeviceGroupRequest): DeviceManager {
        context.log.info("Device group ${request.groupId} terminated")
        groupIdToActor.remove(request.groupId)
        return this
    }

    private fun onListDevices(request: ListDevicesRequest): DeviceManager {
        val deviceGroupActor = groupIdToActor[request.groupId]
        if (deviceGroupActor != null) {
            deviceGroupActor.tell(request)
        } else {
            request.replyTo.tell(ListDevicesResponse(request.requestId, emptySet()))
        }
        return this
    }

    private fun onPostStop(): DeviceManager {
        context.log.info("Stopped")
        return this
    }

    fun getDeviceGroups(): Map<String, ActorRef<Command>> {
        return groupIdToActor
    }

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup { context: ActorContext<Command> -> DeviceManager(context) }
        }
    }
}
