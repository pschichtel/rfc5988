package tel.schich.rfc5988.rfc5987

import tel.schich.parserkombinator.Parser
import tel.schich.parserkombinator.ParserResult
import tel.schich.parserkombinator.andThenTake
import tel.schich.parserkombinator.concat
import tel.schich.parserkombinator.flatMap
import tel.schich.parserkombinator.map
import tel.schich.parserkombinator.optional
import tel.schich.parserkombinator.or
import tel.schich.parserkombinator.surroundedBy
import tel.schich.parserkombinator.take
import tel.schich.parserkombinator.takeString
import tel.schich.parserkombinator.takeWhile
import tel.schich.rfc5988.rfc2234.HexDigitChars
import tel.schich.rfc5988.rfc2616.Alpha
import tel.schich.rfc5988.rfc2616.Digit
import tel.schich.rfc5988.rfc5646.LanguageTag
import tel.schich.rfc5988.rfc5646.parseLanguageTag
import java.nio.charset.Charset

val AttrChars = Alpha + Digit + setOf(
    '!', '#', '$', '+', '-', '.',
    '^', '_', '`', '|', '~',
)

val MimeCharsetChars = Alpha + Digit + setOf(
    '!', '#', '$', '%', '&',
    '+', '-', '^', '_', '`',
    '{', '}', '~'
)

private val parseMimeCharset = takeWhile(min = 1, oneOf = MimeCharsetChars)

private val parseCharset = (takeString("UTF-8") or takeString("ISO-8859-1") or parseMimeCharset)
    .map { it.toString() }

private val parsePctEncoded = take('%').andThenTake(take(HexDigitChars) concat take(HexDigitChars))
    .map { it.toString().toInt(16).toByte() }

private fun parseValueChars(charset: Charset): Parser<String> = { input ->
    val builder = mutableListOf<Byte>()
    var rest = input

    while (true) {
        when (val result = parsePctEncoded(rest)) {
            is ParserResult.Ok -> {
                builder.add(result.value)
                rest = result.rest
                continue
            }
            is ParserResult.Error -> {}
        }

        when (val result = take(AttrChars)(rest)) {
            is ParserResult.Ok -> {
                for (c in result.value) {
                    builder.add(c.code.toByte())
                }
                rest = result.rest
                continue
            }
            is ParserResult.Error -> {}
        }

        break
    }

    ParserResult.Ok(String(builder.toByteArray(), charset), rest)
}

val parseParamName = takeWhile(min = 1, oneOf = AttrChars)

val parseExtendedValue = parseCharset.flatMap { charsetName ->
    parseLanguageTag.optional().surroundedBy(take('\'')).flatMap { language ->
        val charset = Charset.forName(charsetName)
        parseValueChars(charset).map { chars ->
            ExtendedValue(charset, language, chars)
        }
    }
}

data class ExtendedValue(val charset: Charset, val language: LanguageTag?, val value: String)