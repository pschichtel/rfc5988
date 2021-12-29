package tel.schich.rfc5988.rfc5646

import tel.schich.rfc5988.parsing.entireSliceOf
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeFirst
import tel.schich.rfc5988.parsing.takeWhile
import tel.schich.rfc5988.parsing.then
import tel.schich.rfc5988.rfc2616.Alpha
import tel.schich.rfc5988.rfc2616.Digit

private val AlphaNumericChars = Alpha + Digit

private val SingletonChars = AlphaNumericChars - setOf('x', 'X')

private val parseRegular = takeFirst(
    take("art-lojban"),
    take("cel-gaulish"),
    take("no-bok"),
    take("no-nyn"),
    take("zh-guoyu"),
    take("zh-hakka"),
    take("zh-min"),
    take("zh-min-nan"),
    take("zh-xiang"),
).map { it.toString() }

private val parseIrregular = takeFirst(
    take("en-GB-oed"),
    take("i-ami"),
    take("i-bnn"),
    take("i-default"),
    take("i-enochian"),
    take("i-hak"),
    take("i-klingon"),
    take("i-lux"),
    take("i-mingo"),
    take("i-navajo"),
    take("i-pwn"),
    take("i-tao"),
    take("i-tay"),
    take("i-tsu"),
    take("sgn-BE-FR"),
    take("sgn-BE-NL"),
    take("sgn-CH-DE"),
).map { it.toString() }

private val parseGrandfathered = parseIrregular or parseRegular

private val parsePrivateUse = take('x') then parseRepeatedly(take('-') then takeWhile(1, 8, AlphaNumericChars::contains), 1)

private val parseExtension = take(SingletonChars::contains) then parseRepeatedly(take('-') then takeWhile(2, 8, AlphaNumericChars::contains), 1)

private val parseVariant = takeWhile(5, 8, AlphaNumericChars::contains) or take(Digit::contains) then takeWhile(3, 3, AlphaNumericChars::contains)

private val parseRegion = takeWhile(2, 2, Alpha::contains) or takeWhile(3, 3, Digit::contains)

private val parseScript = takeWhile(4, 4, Alpha::contains)

private val parseExtlang = takeWhile(3, 3, Alpha::contains) then parseRepeatedly(take('-') then takeWhile(3, 3, Alpha::contains), 0, 2)

private val parseLanguage = takeWhile(2, 3, Alpha::contains) then (take('-') then parseExtlang).optional()

private val parseLangtag = parseLanguage then (take('-') then parseScript).optional() then (take('-') then parseRegion).optional() then parseRepeatedly(take('-') then parseVariant) then parseRepeatedly(take('-') then parseExtension) then (take('-') then parsePrivateUse).optional()

val parseLanguageTag = entireSliceOf(parseLangtag or parsePrivateUse or parseGrandfathered).map { it.toString() }