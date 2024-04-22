package device.data

interface TemperatureReading

data class TemperatureRead(val value: Double) : TemperatureReading

enum class TemperatureNotAvailable : TemperatureReading {
    INSTANCE
}

enum class DeviceNotAvailable : TemperatureReading {
    INSTANCE
}

enum class DeviceTimedOut : TemperatureReading {
    INSTANCE
}
