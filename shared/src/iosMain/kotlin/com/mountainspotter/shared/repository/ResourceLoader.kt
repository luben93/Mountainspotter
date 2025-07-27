package com.mountainspotter.shared.repository

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual fun loadResource(resourcePath: String): String {
    return try {
        val bundle = NSBundle.mainBundle
        println("iOS ResourceLoader: Loading resource from path: $resourcePath")
        
        // Debug: Print bundle path and contents
        println("iOS ResourceLoader: Bundle path: ${bundle.bundlePath}")
        bundle.pathsForResourcesOfType(null, null).let { paths ->
            println("iOS ResourceLoader: Available resources in bundle:")
            (0 until paths.count()).forEach { index ->
                val resourcePath = paths[index]
                println("iOS ResourceLoader:   - $resourcePath")
            }
        }
        
        // Try multiple approaches to find the resource
        val fileName = if (resourcePath.contains('.')) {
            resourcePath.substringBeforeLast('.')
        } else {
            resourcePath
        }
        val fileExtension = if (resourcePath.contains('.')) {
            resourcePath.substringAfterLast('.') 
        } else {
            null
        }
        
        println("iOS ResourceLoader: Looking for file: '$fileName' with extension: '$fileExtension'")
        
        // Try 1: With file name and extension
        var path = bundle.pathForResource(fileName, fileExtension)
        if (path == null) {
            println("iOS ResourceLoader: Method 1 failed, trying without extension")
            // Try 2: Without extension splitting
            path = bundle.pathForResource(resourcePath, null)
        }
        if (path == null) {
            println("iOS ResourceLoader: Method 2 failed, trying with just filename")
            // Try 3: Just the filename without any extension
            path = bundle.pathForResource(fileName, null)
        }
        
        if (path == null) {
            println("iOS ResourceLoader: All methods failed, resource not found in bundle")
            throw IllegalStateException("Could not find resource: $resourcePath in iOS bundle")
        }
        
        println("iOS ResourceLoader: Found resource at path: $path")
        val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        if (content == null) {
            println("iOS ResourceLoader: Failed to read content from file")
            throw IllegalStateException("Could not read resource content: $resourcePath")
        }
        
        println("iOS ResourceLoader: Successfully loaded resource, content length: ${content.length}")
        content
        
    } catch (e: Exception) {
        println("iOS ResourceLoader: Error loading resource $resourcePath: ${e.message}")
        println("iOS ResourceLoader: This error will be handled by fallback mechanism")
        throw e
    }
}