package org.luben93

import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Double.formatDecimal(decimals: Int): String {
    // Create format string without interpolation
    val format = "%." + decimals + "f"
    return NSString.stringWithFormat(format, NSNumber(this))
}

actual fun Float.formatDecimal(decimals: Int): String {
    // Create format string without interpolation
    val format = "%." + decimals + "f"
    return NSString.stringWithFormat(format, NSNumber(this))
}
