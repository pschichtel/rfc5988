package tel.schich.rfc5988.rfc2616

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.StringSlice
import tel.schich.rfc5988.parsing.andThenIgnore
import tel.schich.rfc5988.parsing.andThenTake
import tel.schich.rfc5988.parsing.concat
import tel.schich.rfc5988.parsing.entireSliceOf
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.surroundedBy
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeFirst

val LowercaseAlpha = ('a'..'z').toSet()
val UppercaseAlpha = ('A'..'Z').toSet()
val Alpha = UppercaseAlpha + LowercaseAlpha
val Digit = ('0'..'9').toSet()
fun isCtl(c: Char) = c.code in 0..31
fun isChar(c: Char) = c.code in 0..127
fun isOctet(c: Char) = c.code in 0..255
fun isCr(c: Char) = c.code == 13
fun isLf(c: Char) = c.code == 10
fun isSp(c: Char) = c.code == 32
fun isHt(c: Char) = c.code == 9
val parseCrLf = entireSliceOf(take(::isCr) concat take(::isLf))
val parseLws = parseCrLf.optional().concat(entireSliceOf(parseRepeatedly(take(::isSp) or take(::isHt), 1)))
val parseImpliedLws = parseRepeatedly(parseLws)
fun parseText(extraPredicate: (Char) -> Boolean = { true }): Parser<StringSlice> =
    takeFirst(parseLws, take { isOctet(it) && !isCtl(it) && extraPredicate(it) })

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

private val parseCommaWithLws = take(',').surroundedBy(parseImpliedLws)

fun <T : Any> parseCommaSeparatedList(min: Int = 0, max: Int = -1, parser: Parser<T>): Parser<List<T>> = { input ->
    when (max) {
        in 0 until min -> Result.Error("Min ($min) can't be larger than max ($max)!", input)
        0 -> Result.Ok(emptyList(), input)
        else -> {
            val optionalElement = parser.optional()

            when (val first = optionalElement(input)) {
                is Result.Error -> first
                is Result.Ok -> {
                    val output = mutableListOf<T>()
                    var maxRemaining = max
                    var rest = first.rest
                    val firstValue = first.value
                    if (firstValue != null) {
                        output.add(firstValue)
                        maxRemaining -= 1
                    }

                    val optionalElementWithSeparator = parseCommaWithLws.andThenTake(optionalElement)
                    while (true) {
                        when (val result = optionalElementWithSeparator(rest)) {
                            is Result.Ok -> {
                                val value = result.value
                                rest = result.rest
                                if (value != null) {
                                    output.add(result.value)
                                    if (max >= 0 && output.size == max) {
                                        break
                                    }
                                }
                            }
                            is Result.Error -> {
                                break
                            }
                        }
                    }

                    if (output.size < min) Result.Error("only matched ${output.size} times, $min required!", input)
                    else Result.Ok(output.toList(), rest)
                }
            }

        }
    }
}