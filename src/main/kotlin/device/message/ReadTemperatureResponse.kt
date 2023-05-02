package device.message

class ReadTemperatureResponse(
    val requestId: Long,
    val deviceId: String,
    val value: Double?
): Command
