package tel.schich.rfc5988.rfc3986

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.takeUntil
import tel.schich.rfc5988.parsing.trace
import tel.schich.rfc5988.rfc2616.SP

/**
 * Implement this properly based on: https://datatracker.ietf.org/doc/html/rfc3986#section-3
 *
 * The current implementation takes a function checking for the follow set, so we consume the entire URI without
 * understanding its content. As such no structural validation is performed on the URI.
 */
private fun parseAny(until: (Char) -> Boolean) =
    takeUntil(min = 1) { it == SP || until(it) }.map { it.toString() }

fun parseUri(until: (Char) -> Boolean): Parser<String> = trace("URI(until=$until)", parseAny(until))

fun parseUriReference(until: (Char) -> Boolean): Parser<String> =
    trace("URI-Reference(until=$until)", parseAny(until))