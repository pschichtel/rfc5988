package tel.schich.rfc5988.rfc4288

import tel.schich.parserkombinator.takeWhile
import tel.schich.rfc5988.rfc2616.Alpha
import tel.schich.rfc5988.rfc2616.Digit


private val RegisteredNameChars = Alpha + Digit + setOf(
    '!',
    '#', '$', '&', '.',
    '+', '-', '^', '_',
)

val parseRegName = takeWhile(min = 1, max = 127, oneOf = RegisteredNameChars)