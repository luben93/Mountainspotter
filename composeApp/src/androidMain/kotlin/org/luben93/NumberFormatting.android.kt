package org.luben93

actual fun Double.formatDecimal(decimals: Int): String {
    return java.lang.String.format("%.${decimals}f", this)
}

actual fun Float.formatDecimal(decimals: Int): String {
    return java.lang.String.format("%.${decimals}f", this)
}
