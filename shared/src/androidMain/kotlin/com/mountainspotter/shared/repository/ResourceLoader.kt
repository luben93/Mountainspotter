package com.mountainspotter.shared.repository

actual fun loadResource(resourcePath: String): String {
    // Try multiple classloaders to find the resource
    val classLoaders = listOf(
        Thread.currentThread().contextClassLoader,
        MountainRepository::class.java.classLoader,
        ClassLoader.getSystemClassLoader()
    )
    
    for (classLoader in classLoaders) {
        classLoader?.getResourceAsStream(resourcePath)?.use { stream ->
            return stream.bufferedReader().readText()
        }
    }
    
    throw IllegalStateException("Could not load resource: $resourcePath")
}