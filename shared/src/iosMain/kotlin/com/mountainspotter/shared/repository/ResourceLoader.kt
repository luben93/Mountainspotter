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
    val path = bundle.pathForResource(resourcePath.substringBeforeLast('.'), resourcePath.substringAfterLast('.'))
        ?: throw IllegalStateException("Could not find resource: $resourcePath")
    
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        ?: throw IllegalStateException("Could not load resource: $resourcePath")
}