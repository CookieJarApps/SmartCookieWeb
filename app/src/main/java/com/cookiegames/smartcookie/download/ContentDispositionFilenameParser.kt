package com.cookiegames.smartcookie.download

import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.text.Charsets.ISO_8859_1
import kotlin.text.Charsets.UTF_8

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Parses "filename" parameter of the Content-Disposition HTTP header as defined in RFC 6266.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
internal object ContentDispositionFileNameParser {
    private const val INVALID_HEADER_FIELD_PARAMETER_FORMAT =
        "Invalid header field parameter format (as defined in RFC 5987)"

    /**
     * Parse a Content-Disposition header value as defined in RFC 2183.
     *
     * @param contentDisposition the Content-Disposition header value
     * @return Return the value of the filename parameter (or the value of the
     * filename* one decoded as defined in the RFC 5987), or `null` if not defined.
     */
    fun parse(contentDisposition: String): String? {
        val parts = tokenize(contentDisposition)
        var filename: String? = null
        var charset: Charset
        for (i in 1 until parts.size) {
            val part = parts[i]
            val eqIndex = part.indexOf('=')
            if (eqIndex != -1) {
                val attribute = part.substring(0, eqIndex)
                val value =
                    if (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"")) part.substring(
                        eqIndex + 2,
                        part.length - 1
                    ) else part.substring(eqIndex + 1)
                if (attribute == "filename*") {
                    val idx1 = value.indexOf('\'')
                    val idx2 = value.indexOf('\'', idx1 + 1)
                    if (idx1 != -1 && idx2 != -1) {
                        charset = Charset.forName(value.substring(0, idx1).trim { it <= ' ' })
                        require(UTF_8.equals(charset) || ISO_8859_1.equals(charset)) { "Charset should be UTF-8 or ISO-8859-1" }
                        filename = decodeFilename(value.substring(idx2 + 1), charset)
                    } else {
                        // US ASCII
                        filename = decodeFilename(value, StandardCharsets.US_ASCII)
                    }
                } else if (attribute == "filename" && filename == null) {
                    filename = value
                }
            } else {
                throw IllegalArgumentException("Invalid content disposition format")
            }
        }
        return filename
    }

    private fun tokenize(headerValue: String): List<String> {
        var index = headerValue.indexOf(';')
        val type =
            (if (index >= 0) headerValue.substring(0, index) else headerValue).trim { it <= ' ' }
        require(!type.isEmpty()) { "Content-Disposition header must not be empty" }
        val parts: MutableList<String> = ArrayList()
        parts.add(type)
        if (index >= 0) {
            do {
                var nextIndex = index + 1
                var quoted = false
                var escaped = false
                while (nextIndex < headerValue.length) {
                    val ch = headerValue[nextIndex]
                    if (ch == ';') {
                        if (!quoted) {
                            break
                        }
                    } else if (!escaped && ch == '"') {
                        quoted = !quoted
                    }
                    escaped = !escaped && ch == '\\'
                    nextIndex++
                }
                val part = headerValue.substring(index + 1, nextIndex).trim { it <= ' ' }
                if (!part.isEmpty()) {
                    parts.add(part)
                }
                index = nextIndex
            } while (index < headerValue.length)
        }
        return parts
    }

    /**
     * Decode the given header field param as described in RFC 5987.
     *
     * Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
     *
     * @param filename the filename
     * @param charset  the charset for the filename
     * @return the encoded header field param
     */
    private fun decodeFilename(filename: String?, charset: Charset?): String {
        requireNotNull(filename) { "'input' String` should not be null" }
        requireNotNull(charset) { "'charset' should not be null" }
        val value: ByteArray = filename.toByteArray(charset)
        val byteArrayOutputStream = ByteArrayOutputStream()
        var index = 0
        while (index < value.size) {
            val b = value[index]
            if (isRFC5987AttrChar(b)) {
                byteArrayOutputStream.write(byteArrayOf(b))
                index++
            } else if (b == '%'.code.toByte() && index < value.size - 2) {
                val array = charArrayOf(
                    value[index + 1].toChar(),
                    value[index + 2].toChar()
                )
                try {
                    byteArrayOutputStream.write(String(array).toInt(16))
                } catch (ex: NumberFormatException) {
                    throw IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex)
                }
                index += 3
            } else {
                throw IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT)
            }
        }
        return try {
            byteArrayOutputStream.toString(charset.name())
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(
                "Failed to copy contents of ByteArrayOutputStream into a String",
                e
            )
        }
    }

    private fun isRFC5987AttrChar(c: Byte): Boolean {
        return c >= '0'.code.toByte() && c <= '9'.code.toByte() || c >= 'a'.code.toByte() && c <= 'z'.code.toByte() || c >= 'A'.code.toByte() && c <= 'Z'.code.toByte() || c == '!'.code.toByte() || c == '#'.code.toByte() || c == '$'.code.toByte() || c == '&'.code.toByte() || c == '+'.code.toByte() || c == '-'.code.toByte() || c == '.'.code.toByte() || c == '^'.code.toByte() || c == '_'.code.toByte() || c == '`'.code.toByte() || c == '|'.code.toByte() || c == '~'.code.toByte()
    }
}