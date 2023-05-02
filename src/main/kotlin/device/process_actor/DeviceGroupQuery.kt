package device.process_actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import device.data.*
import device.message.*
import java.time.Duration

class DeviceGroupQuery private constructor(
    context: ActorContext<Command>,
    private val requestId: Long,
    groupId: String,
    private val requester: ActorRef<Command>,
    deviceIdToActor: Map<String, ActorRef<Command>>,
    timeout: Duration,
    timers: TimerScheduler<Command>
) : AbstractBehavior<Command>(context) {

    private val repliesSoFar = mutableMapOf<String, TemperatureReading>()
    private val stillWaiting = mutableSetOf<String>()

    init {
        timers.startSingleTimer(ReadTemperatureTimeoutResponse.INSTANCE, timeout)
        val readTemperatureResponseAdapter = context.messageAdapter(ReadTemperatureResponse::class.java) { it }
        for ((key, value) in deviceIdToActor) {
            context.watchWith(value, TerminateDeviceRequest(groupId, key))
            value.tell(ReadTemperatureRequest(0L, readTemperatureResponseAdapter))
        }
        stillWaiting.addAll(deviceIdToActor.keys)
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(ReadTemperatureResponse::class.java, this::onReadTemperature)
            .onMessage(ReadTemperatureTimeoutResponse::class.java) { onReadTemperatureTimeout() }
            .onMessage(TerminateDeviceRequest::class.java, this::onTerminateDevice)
            .build()
    }

    private fun onReadTemperature(request: ReadTemperatureResponse): Behavior<Command> {
        val temperatureReading = if (request.value != null) {
            TemperatureRead(request.value)
        } else {
            TemperatureNotAvailable()
        }
        repliesSoFar[request.deviceId] = temperatureReading
        stillWaiting.remove(request.deviceId)
        return respondWhenAllCollected()
    }

    private fun onTerminateDevice(request: TerminateDeviceRequest): Behavior<Command> {
        if (stillWaiting.contains(request.deviceId)) {
            repliesSoFar[request.deviceId] = DeviceNotAvailable()
            stillWaiting.remove(request.deviceId)
        }
        return respondWhenAllCollected()
    }

    private fun onReadTemperatureTimeout(): Behavior<Command> {
        for (deviceId in stillWaiting) {
            repliesSoFar[deviceId] = DeviceTimedOut()
        }
        stillWaiting.clear()
        return respondWhenAllCollected()
    }

    private fun respondWhenAllCollected(): Behavior<Command> {
        return if (stillWaiting.isEmpty()) {
            requester.tell(ReadGroupTemperatureResponse(requestId, repliesSoFar.toMap()))
            repliesSoFar.clear()
            Behaviors.stopped()
        } else {
            this
        }
    }

    companion object {
        fun create(
            requestId: Long,
            groupId: String,
            requester: ActorRef<Command>,
            deviceIdToActor: Map<String, ActorRef<Command>>,
            timeout: Duration
        ): Behavior<Command> {
            return Behaviors.setup { context ->
                Behaviors.withTimers { timers ->
                    DeviceGroupQuery(context, requestId, groupId, requester, deviceIdToActor, timeout, timers)
                }
            }
        }
    }
}
