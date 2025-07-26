package org.luben93

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform