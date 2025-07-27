package com.mountainspotter.shared.repository

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun loadResource(resourcePath: String): String {
    val bundle = NSBundle.mainBundle
    val path = bundle.pathForResource(resourcePath.substringBeforeLast('.'), resourcePath.substringAfterLast('.'))
        ?: throw IllegalStateException("Could not find resource: $resourcePath")
    
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        ?: throw IllegalStateException("Could not load resource: $resourcePath")
}