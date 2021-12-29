package tel.schich.rfc5988.rfc5646

import tel.schich.rfc5988.parsing.entireSliceOf
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeString
import tel.schich.rfc5988.parsing.takeWhile
import tel.schich.rfc5988.parsing.then
import tel.schich.rfc5988.rfc2616.Alpha
import tel.schich.rfc5988.rfc2616.Digit

private val AlphaNumericChars = Alpha + Digit

private val SingletonChars = AlphaNumericChars - setOf('x', 'X')

private val parseRegular = takeString(
    setOf(
        "art-lojban",
        "cel-gaulish",
        "no-bok",
        "no-nyn",
        "zh-guoyu",
        "zh-hakka",
        "zh-min",
        "zh-min-nan",
        "zh-xiang",
    )
).map { it.toString() }

private val parseIrregular = takeString(
    setOf(
        "en-GB-oed",
        "i-ami",
        "i-bnn",
        "i-default",
        "i-enochian",
        "i-hak",
        "i-klingon",
        "i-lux",
        "i-mingo",
        "i-navajo",
        "i-pwn",
        "i-tao",
        "i-tay",
        "i-tsu",
        "sgn-BE-FR",
        "sgn-BE-NL",
        "sgn-CH-DE",
    )
).map { it.toString() }

private val parseGrandfathered = parseIrregular or parseRegular

private val parsePrivateUse = take('x') then parseRepeatedly(take('-') then takeWhile(1, 8, AlphaNumericChars), 1)

private val parseExtension = take(SingletonChars) then parseRepeatedly(take('-') then takeWhile(2, 8, AlphaNumericChars), 1)

private val parseVariant = takeWhile(5, 8, AlphaNumericChars) or take(Digit) then takeWhile(3, 3, AlphaNumericChars)

private val parseRegion = takeWhile(2, 2, Alpha) or takeWhile(3, 3, Digit)

private val parseScript = takeWhile(4, 4, Alpha)

private val parseExtlang = takeWhile(3, 3, Alpha) then parseRepeatedly(take('-') then takeWhile(3, 3, Alpha), 0, 2)

private val parseLanguage = takeWhile(2, 3, Alpha) then (take('-') then parseExtlang).optional()

private val parseLangtag = parseLanguage then (take('-') then parseScript).optional() then (take('-') then parseRegion).optional() then parseRepeatedly(take('-') then parseVariant) then parseRepeatedly(take('-') then parseExtension) then (take('-') then parsePrivateUse).optional()

val parseLanguageTag = entireSliceOf(parseLangtag or parsePrivateUse or parseGrandfathered).map { it.toString() }