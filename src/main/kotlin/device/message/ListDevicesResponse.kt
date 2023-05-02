package device.message

class ListDevicesResponse(
    val requestId: Long,
    val deviceIds: Set<String>
)
