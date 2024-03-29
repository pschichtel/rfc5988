package tel.schich.rfc5988.rfc3986

import tel.schich.parserkombinator.Parser
import tel.schich.parserkombinator.forTrace
import tel.schich.parserkombinator.map
import tel.schich.parserkombinator.takeUntil
import tel.schich.parserkombinator.trace
import tel.schich.rfc5988.rfc2616.SP

/**
 * Implement this properly based on: https://datatracker.ietf.org/doc/html/rfc3986#section-3
 *
 * The current implementation takes a function checking for the follow set, so we consume the entire URI without
 * understanding its content. As such no structural validation is performed on the URI.
 */
private fun parseAny(followSet: Set<Char>) =
    takeUntil(min = 1, oneOf = followSet + SP).map { it.toString() }

fun parseUri(followSet: Set<Char>): Parser<String> = trace("URI(followSet=${forTrace(followSet)})", parseAny(followSet))

fun parseUriReference(followSet: Set<Char>): Parser<String> =
    trace("URI-Reference(followSet=${forTrace(followSet)})", parseAny(followSet))