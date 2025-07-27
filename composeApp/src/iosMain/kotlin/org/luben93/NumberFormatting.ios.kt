package org.luben93

import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun Double.formatDecimal(decimals: Int): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterDecimalStyle
    formatter.minimumFractionDigits = decimals.toULong()
    formatter.maximumFractionDigits = decimals.toULong()
    return formatter.stringFromNumber(platform.Foundation.NSNumber(this)) ?: "0.0"
}

actual fun Float.formatDecimal(decimals: Int): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterDecimalStyle
    formatter.minimumFractionDigits = decimals.toULong()
    formatter.maximumFractionDigits = decimals.toULong()
    return formatter.stringFromNumber(platform.Foundation.NSNumber(this)) ?: "0.0"
}
