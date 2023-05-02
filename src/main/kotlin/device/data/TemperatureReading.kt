package device.data

interface TemperatureReading

data class TemperatureRead(val value: Double) : TemperatureReading

class TemperatureNotAvailable : TemperatureReading

class DeviceNotAvailable : TemperatureReading

class DeviceTimedOut : TemperatureReading
