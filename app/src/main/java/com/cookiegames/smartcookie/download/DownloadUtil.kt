package com.cookiegames.smartcookie.download

import java.io.Closeable
import java.text.DecimalFormat

object DownloadUtil {
    fun getDataSize(size: Long): String {
        var size = size
        if (size < 0) {
            size = 0
        }
        val format = DecimalFormat("####.00")
        return if (size < 1024) {
            size.toString() + "bytes"
        } else if (size < 1024 * 1024) {
            val kbsize = size / 1024f
            format.format(kbsize).toString() + "KB"
        } else if (size < 1024 * 1024 * 1024) {
            val mbsize = size / 1024f / 1024f
            format.format(mbsize).toString() + "MB"
        } else if (size < 1024 * 1024 * 1024 * 1024) {
            val gbsize = size / 1024f / 1024f / 1024f
            format.format(gbsize).toString() + "GB"
        } else {
            "size: error"
        }
    }

    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (rethrown: RuntimeException) {
                throw rethrown
            } catch (ignored: Exception) {
            }
        }
    }
}