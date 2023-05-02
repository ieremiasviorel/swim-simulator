package device.message

import device.data.TemperatureReading

class ReadGroupTemperatureResponse(
    val requestId: Long,
    val temperatures: Map<String, TemperatureReading>
): Command
