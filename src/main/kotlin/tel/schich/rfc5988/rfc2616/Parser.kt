package tel.schich.rfc5988.rfc2616

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.StringSlice
import tel.schich.rfc5988.parsing.andThenIgnore
import tel.schich.rfc5988.parsing.andThenTake
import tel.schich.rfc5988.parsing.concat
import tel.schich.rfc5988.parsing.entireSliceOf
import tel.schich.rfc5988.parsing.forTrace
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.surroundedBy
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeString
import tel.schich.rfc5988.parsing.takeFirst
import tel.schich.rfc5988.parsing.traced

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

private fun parseString(input: StringSlice): Result<String> {
    val builder = StringBuilder()
    var rest = input
    while (true) {
        when (val result = parseQuotedPair(rest)) {
            is Result.Ok -> {
                builder.append(result.value)
                rest = result.rest
                continue
            }
            is Result.Error -> {}
        }

        when (val result = parseQdtext(rest)) {
            is Result.Ok -> {
                builder.append(result.value)
                rest = result.rest
                continue
            }
            is Result.Error -> {}
        }

        break
    }

    return Result.Ok(builder.toString(), rest)
}

val parseQuotedString: Parser<String> =
    take('"').andThenTake(::parseString).andThenIgnore(take('"'))

private val parseCommaWithLws = takeWithLws(',')

fun <T : Any> parseCommaSeparatedList(min: Int = 0, max: Int = -1, parser: Parser<T>): Parser<List<T>> {
    val optionalElement = parser.optional()

    return { input ->
        when (max) {
            in 0 until min -> Result.Error("Min ($min) can't be larger than max ($max)!", input)
            0 -> Result.Ok(emptyList(), input)
            else -> {
                val output = mutableListOf<T>()
                var maxRemaining = max
                var rest = input

                when (val first = optionalElement(input)) {
                    is Result.Ok -> {
                        val firstValue = first.value
                        rest = first.rest
                        if (firstValue != null) {
                            output.add(firstValue)
                            maxRemaining -= 1
                        }
                    }
                    is Result.Error -> {
                    }
                }


                while (output.size != max) {
                    when (val result = parseCommaWithLws(rest)) {
                        is Result.Ok -> {
                            rest = result.rest
                        }
                        is Result.Error -> {
                            break
                        }
                    }
                    when (val result = optionalElement(rest)) {
                        is Result.Ok -> {
                            val value = result.value
                            rest = result.rest
                            if (value != null) {
                                output.add(value)
                            }
                        }
                        is Result.Error -> {
                        }
                    }
                }

                if (output.size < min) Result.Error("only matched ${output.size} times, $min required!", input)
                else Result.Ok(output.toList(), rest)
            }
        }
    }
}