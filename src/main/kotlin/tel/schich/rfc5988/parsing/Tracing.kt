package tel.schich.rfc5988.parsing

private val enableTracing = System.getProperty("tel.schich.rfc5988.parsing.trace", "false").toBoolean()

private var suppressTraces = false
private var traceDepth = 0

@JvmName("forTraceChars")
fun forTrace(chars: Set<Char>): String =
    chars.joinToString(prefix = "{", separator = ", ", postfix = "}") { forTrace(it) }

@JvmName("forTraceStrings")
fun forTrace(strings: Set<CharSequence>): String =
    strings.joinToString(prefix = "{", separator = ", ", postfix = "}") { forTrace(it, "\"") }

fun forTrace(c: Char): String = "'${forTrace("$c", "'")}'"

fun forTrace(c: String): String = "\"${forTrace(c, "\"")}\""

fun forTrace(s: CharSequence, quote: String) = forTrace(s).replace(quote, "\\$quote")

fun forTrace(s: CharSequence) = s.toString()
    .replace("\\", "\\\\")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

fun <T> trace(msg: String, parser: Parser<T>): Parser<T> = trace(msg, shallow = false, parser)

fun <T> trace(msg: String, shallow: Boolean, parser: Parser<T>): Parser<T> {
    // disappear from parser call stacks
    if (!enableTracing) {
        return parser
    }
    return { input ->
        if (suppressTraces) {
            parser(input)
        } else {
            val depth = traceDepth
            traceDepth += 1
            val prefix = "|\t".repeat(depth) + ">"
            println("$prefix $msg")
            suppressTraces = shallow
            val result = parser(input)
            suppressTraces = false
            traceDepth -= 1
            when (result) {
                is Result.Ok -> {
                    val len = input.length - result.rest.length
                    val content = input.string.substring(input.offset, input.offset + len)
                    println("$prefix ✓ (len=$len, content=\"${ forTrace(content, "\"")}\")")
                }
                is Result.Error -> {
                    println("$prefix ${forTrace(result.message)}")
                }
            }
            result
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Parser<T>.traced(msg: String, shallow: Boolean = false): Parser<T> = trace(msg, shallow, this)