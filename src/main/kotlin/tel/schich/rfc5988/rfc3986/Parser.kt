package tel.schich.rfc5988.rfc3986

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.or

val parseUri: Parser<String> = { input ->
    TODO("URI: RFC 3986 unimplemented")
}

val parseUriReference: Parser<String> = parseUri or { input ->
    TODO("URI-Reference: RFC 3986 unimplemented")
}