package com.mountainspotter.shared.repository

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual fun loadResource(resourcePath: String): String {
    val bundle = NSBundle.mainBundle
    println("Loading resource from path: $resourcePath")
    
    // Debug: Print bundle path and contents
    println("Bundle path: ${bundle.bundlePath}")
    bundle.pathsForResourcesOfType(null, null)?.let { paths ->
        println("Available resources in bundle:")
        (0 until paths.count.toInt()).forEach { index ->
            val resourcePath = paths.objectAtIndex(index.toULong())
            println("  - $resourcePath")
        }
    }
    
    // Try to find the resource
    val fileName = resourcePath.substringBeforeLast('.')
    val fileExtension = resourcePath.substringAfterLast('.')
    println("Looking for file: '$fileName' with extension: '$fileExtension'")
    
    val path = bundle.pathForResource(fileName, fileExtension)
    if (path == null) {
        // Try without extension splitting
        val pathNoSplit = bundle.pathForResource(resourcePath, null)
        if (pathNoSplit != null) {
            println("Found resource without extension splitting: $pathNoSplit")
            return NSString.stringWithContentsOfFile(pathNoSplit, NSUTF8StringEncoding, null)
                ?: throw IllegalStateException("Could not load resource content: $resourcePath")
        }
        throw IllegalStateException("Could not find resource: $resourcePath (tried both '$fileName.$fileExtension' and '$resourcePath')")
    }
    
    println("Found resource at path: $path")
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        ?: throw IllegalStateException("Could not load resource: $resourcePath")
}