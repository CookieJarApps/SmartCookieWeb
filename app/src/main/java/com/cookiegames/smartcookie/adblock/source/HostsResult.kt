package com.cookiegames.smartcookie.adblock.source

import com.cookiegames.smartcookie.database.adblock.Host

/**
 * The result of a request for the hosts to block.
 */
sealed class HostsResult {

    /**
     * A successful request.
     *
     * @param hosts The hosts to block.
     */
    data class Success(val hosts: List<Host>) : HostsResult()

    /**
     * An unsuccessful request.
     *
     * @param cause The cause of the failure.
     */
    data class Failure(val cause: Exception) : HostsResult()

}
