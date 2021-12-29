package tel.schich.rfc5988.rfc5987

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.andThenTake
import tel.schich.rfc5988.parsing.concat
import tel.schich.rfc5988.parsing.flatMap
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.surroundedBy
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeString
import tel.schich.rfc5988.parsing.takeWhile
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
            is Result.Ok -> {
                builder.add(result.value)
                rest = result.rest
                continue
            }
            is Result.Error -> {}
        }

        when (val result = take(AttrChars)(rest)) {
            is Result.Ok -> {
                for (c in result.value) {
                    builder.add(c.code.toByte())
                }
                rest = result.rest
                continue
            }
            is Result.Error -> {}
        }

        break
    }

    Result.Ok(String(builder.toByteArray(), charset), rest)
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