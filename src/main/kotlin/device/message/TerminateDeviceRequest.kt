package device.message

class TerminateDeviceRequest(
    val groupId: String,
    val deviceId: String,
) : Command
