package tel.schich.rfc5988.rfc2616

import tel.schich.parserkombinator.Parser
import tel.schich.parserkombinator.ParserResult
import tel.schich.parserkombinator.StringSlice
import tel.schich.parserkombinator.andThenIgnore
import tel.schich.parserkombinator.andThenTake
import tel.schich.parserkombinator.concat
import tel.schich.parserkombinator.entireSliceOf
import tel.schich.parserkombinator.forTrace
import tel.schich.parserkombinator.map
import tel.schich.parserkombinator.optional
import tel.schich.parserkombinator.or
import tel.schich.parserkombinator.parseRepeatedly
import tel.schich.parserkombinator.surroundedBy
import tel.schich.parserkombinator.take
import tel.schich.parserkombinator.takeString
import tel.schich.parserkombinator.takeFirst
import tel.schich.parserkombinator.traced

val LowercaseAlpha = ('a'..'z').toSet()
val UppercaseAlpha = ('A'..'Z').toSet()
val Alpha = UppercaseAlpha + LowercaseAlpha
val Digit = ('0'..'9').toSet()
fun isCtl(c: Char) = c.code in 0..31
fun isChar(c: Char) = c.code in 0..127
fun isOctet(c: Char) = c.code in 0..255
const val CR = 13.toChar()
const val LF = 10.toChar()
const val CRLF = "$CR$LF"
const val SP = 32.toChar()
const val HT = 9.toChar()
val parseCrLf = takeString(CRLF)
val parseLws = parseCrLf.optional().concat(entireSliceOf(parseRepeatedly(take(SP) or take(HT), 1)))
val parseImpliedLws = parseRepeatedly(parseLws).traced("implied *LWS", shallow = true)
fun parseText(extraPredicate: (Char) -> Boolean = { true }): Parser<StringSlice> =
    takeFirst(parseLws, take { isOctet(it) && !isCtl(it) && extraPredicate(it) })

fun takeWithLws(c: Char) = take(c).surroundedBy(parseImpliedLws)
    .traced("takeWithLws(c='${forTrace("$c")}')", shallow = true)

private val parseQdtext: Parser<StringSlice> = parseText { it != '"' }

private val parseQuotedPair: Parser<Char> = take('\\').andThenTake(take(::isChar)).map { it[0] }

private fun parseString(input: StringSlice): ParserResult<String> {
    val builder = StringBuilder()
    var rest = input
    while (true) {
        when (val result = parseQuotedPair(rest)) {
            is ParserResult.Ok -> {
                builder.append(result.value)
                rest = result.rest
                continue
            }
            is ParserResult.Error -> {}
        }

        when (val result = parseQdtext(rest)) {
            is ParserResult.Ok -> {
                builder.append(result.value)
                rest = result.rest
                continue
            }
            is ParserResult.Error -> {}
        }

        break
    }

    return ParserResult.Ok(builder.toString(), rest)
}

val parseQuotedString: Parser<String> =
    take('"').andThenTake(::parseString).andThenIgnore(take('"'))

private val parseCommaWithLws = takeWithLws(',')

fun <T : Any> parseCommaSeparatedList(min: Int = 0, max: Int = -1, parser: Parser<T>): Parser<List<T>> {
    val optionalElement = parser.optional()

    return { input ->
        when (max) {
            in 0 until min -> ParserResult.Error("Min ($min) can't be larger than max ($max)!", input)
            0 -> ParserResult.Ok(emptyList(), input)
            else -> {
                val output = mutableListOf<T>()
                var maxRemaining = max
                var rest = input

                when (val first = optionalElement(input)) {
                    is ParserResult.Ok -> {
                        val firstValue = first.value
                        rest = first.rest
                        if (firstValue != null) {
                            output.add(firstValue)
                            maxRemaining -= 1
                        }
                    }
                    is ParserResult.Error -> {
                    }
                }


                while (output.size != max) {
                    when (val result = parseCommaWithLws(rest)) {
                        is ParserResult.Ok -> {
                            rest = result.rest
                        }
                        is ParserResult.Error -> {
                            break
                        }
                    }
                    when (val result = optionalElement(rest)) {
                        is ParserResult.Ok -> {
                            val value = result.value
                            rest = result.rest
                            if (value != null) {
                                output.add(value)
                            }
                        }
                        is ParserResult.Error -> {
                        }
                    }
                }

                if (output.size < min) ParserResult.Error("only matched ${output.size} times, $min required!", input)
                else ParserResult.Ok(output.toList(), rest)
            }
        }
    }
}