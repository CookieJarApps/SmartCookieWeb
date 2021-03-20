package com.cookiegames.smartcookie.adblock

import com.cookiegames.smartcookie.adblock.parser.HostsFileParser
import com.cookiegames.smartcookie.database.adblock.Host
import com.cookiegames.smartcookie.log.NoOpLogger
import org.junit.Test
import java.io.InputStreamReader

class HostsFileParserTest {

    @Test
    fun `line parsing is valid`() {
        val testInput = """
            127.0.0.1 localhost #comment comment
            0.0.0.0 localhost #comment comment
            ::1 localhost #comment comment
            #
            # comment
            #
            127.0.0.1	fake.domain1.com
            127.0.0.1	fake.domain2.com    # comment
            0.0.0.0	fake.domain3.com    # comment
            # comment
            ::1 example.com
            0.0.0.0 multi1.com multi2.com # comment
            0.0.0.0 close.comment.com#comment
            """

        val inputStreamReader = InputStreamReader(testInput.trimIndent().byteInputStream())
        val hostsFileParser = HostsFileParser(NoOpLogger())
        val mutableList = hostsFileParser.parseInput(inputStreamReader)

        assert(mutableList.size == 7)
        assert(mutableList.containsAll(
                listOf(
                Host("fake.domain1.com"),
                Host("fake.domain2.com"),
                Host("fake.domain3.com"),
                Host("example.com"),
                Host("multi1.com"),
                Host("multi2.com"),
                Host("close.comment.com")
                )
        ))
    }
}