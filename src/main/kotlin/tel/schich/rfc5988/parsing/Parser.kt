package tel.schich.rfc5988.parsing

typealias Parser<T> = (StringSlice) -> Result<T>

operator fun <T> Parser<T>.invoke(s: String) = this(StringSlice.of(s))

fun <T, U> Parser<T>.flatMap(f: (T) -> Parser<U>): Parser<U> = trace("flatMap()") { input ->
    when (val result = this(input)) {
        is Result.Ok -> f(result.value)(result.rest)
        is Result.Error -> result
    }
}

fun <T, U> Parser<T>.map(f: (T) -> U): Parser<U> = { input ->
    when (val result = this(input)) {
        is Result.Ok -> Result.Ok(f(result.value), result.rest)
        is Result.Error -> result
    }
}

fun <T> Parser<T>.andThenIgnore(next: Parser<*>): Parser<T> = trace("andThenIgnore()") { input ->
    when (val result = this(input)) {
        is Result.Ok -> when (val nextResult = next(result.rest)) {
            is Result.Ok -> Result.Ok(result.value, nextResult.rest)
            is Result.Error -> Result.Error(nextResult.message, input)
        }
        is Result.Error -> result
    }
}

infix fun <T> Parser<T>.then(next: Parser<*>): Parser<T> = andThenIgnore(next)

infix fun Parser<StringSlice?>.concat(next: Parser<StringSlice?>): Parser<StringSlice> = trace("concat()") { input ->
    when (val first = this(input)) {
        is Result.Ok -> when (val second = next(first.rest)) {
            is Result.Ok -> {
                val firstValue = first.value ?: ""
                val secondValue = second.value ?: ""
                Result.Ok(input.subSlice(0, firstValue.length + secondValue.length), second.rest)
            }
            is Result.Error -> Result.Error(second.message, input)
        }
        is Result.Error -> first
    }
}

fun <T, U> Parser<T>.andThenTake(trailer: Parser<U>): Parser<U> = trace("andThenTake()") { input ->
    when (val result = this(input)) {
        is Result.Ok -> when (val trailerResult = trailer(result.rest)) {
            is Result.Ok -> trailerResult
            is Result.Error -> Result.Error(trailerResult.message, input)
        }
        is Result.Error -> result
    }
}

fun <T> Parser<T>.surroundedBy(prefix: Parser<*>, suffix: Parser<*>): Parser<T> = trace("surroundedBy()") { input ->
    when (val prefixResult = prefix(input)) {
        is Result.Error -> prefixResult
        is Result.Ok -> {
            when (val result = this(prefixResult.rest)) {
                is Result.Error -> Result.Error(result.message, input)
                is Result.Ok -> {
                    when (val suffixResult = suffix(result.rest)) {
                        is Result.Error -> Result.Error(suffixResult.message, input)
                        is Result.Ok -> Result.Ok(result.value, suffixResult.rest)
                    }
                }
            }
        }
    }
}

fun <T> Parser<T>.surroundedBy(parser: Parser<*>): Parser<T> = surroundedBy(parser, parser)

infix fun <T : Any> Parser<T>.or(other: Parser<T>): Parser<T> = takeFirst(this, other)

fun <T : Any> Parser<T>.optional(): Parser<T?> = trace("optional()") { input ->
    when (val result = this(input)) {
        is Result.Ok -> result
        is Result.Error -> Result.Ok(null, input)
    }
}

fun <A, B> parseSeparated(a: Parser<A>, separator: Parser<*>, b: Parser<B>): Parser<Pair<A, B>> =
    a.andThenIgnore(separator).flatMap { aValue -> b.map { bValue -> Pair(aValue, bValue) } }

fun <T> parseSeparatedList(parser: Parser<T>, separator: Parser<*>, min: Int = 0, max: Int = -1): Parser<List<T>> = trace("parseSeparatedList(min=$min, max=$max)") { input ->
    when (max) {
        in 0 until min -> Result.Error("Min ($min) can't be larger than max ($max)!", input)
        0 -> Result.Ok(emptyList(), input)
        else -> {
            when (val first = parser(input)) {
                is Result.Error -> first
                is Result.Ok -> {
                    val output = mutableListOf<T>()
                    var maxRemaining = max
                    var rest = first.rest
                    output.add(first.value)
                    maxRemaining -= 1

                    while (true) {
                        val separatorResult = separator(rest)
                        when (separatorResult) {
                            is Result.Ok -> {
                            }
                            is Result.Error -> {
                                break
                            }
                        }
                        when (val result = parser(separatorResult.rest)) {
                            is Result.Ok -> {
                                rest = result.rest
                                output.add(result.value)
                                if (max >= 0 && output.size == max) {
                                    break
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

fun <T : Any> takeFirst(vararg parsers: Parser<T>): Parser<T> = trace("takeFirst(#parsers=${parsers.size})") { input ->
    parsers.asSequence().map { it(input) }.firstOrNull { it is Result.Ok }
        ?: Result.Error("No parser worked!", input)
}

fun <T> parseRepeatedly(parser: Parser<T>, min: Int = 0, max: Int = -1): Parser<List<T>> = trace("parseRepeatedly(min=$min, max=$max, parser=...)") { input ->
    when (max) {
        in 0 until min -> Result.Error("Min ($min) can't be larger than max ($max)!", input)
        0 -> Result.Ok(emptyList(), input)
        else -> {
            var rest = input
            val output = mutableListOf<T>()

            while (true) {
                when (val result = parser(rest)) {
                    is Result.Ok -> {
                        output.add(result.value)
                        rest = result.rest
                        if (max >= 0 && output.size == max) {
                            break
                        }
                    }
                    is Result.Error -> {
                        break
                    }
                }
            }

            if (output.size < min) Result.Error("only matched ${output.size} times, $min required!", input)
            else Result.Ok(output, rest)
        }
    }
}

fun takeWhile(predicate: (Char) -> Boolean): Parser<StringSlice> = takeWhile(min = 0, max = -1, predicate = predicate)

private fun takeWhilePredicate(min: Int = 0, max: Int = -1, predicate: (Char) -> Boolean): Parser<StringSlice> = { input ->
    var len = 0
    var left = max
    while (left != 0 && len < input.length && predicate(input[len])) {
        len += 1
        left -= 1
    }
    if (len >= min) Result.Ok(input.subSlice(0, len), input.subSlice(len))
    else Result.Error("$len were recognized, $min required!", input)
}

fun takeWhile(min: Int = 0, max: Int = -1, predicate: (Char) -> Boolean): Parser<StringSlice> =
    trace("takeWhile(min=$min, max=$max, predicate=$predicate)", takeWhilePredicate(min, max, predicate))

fun takeWhile(min: Int = 0, max: Int = -1, oneOf: Set<Char>): Parser<StringSlice> =
    trace("takeWhile(min=$min, max=$max, oneOf=${forTrace(oneOf)})", takeWhilePredicate(min, max, oneOf::contains))

fun takeWhile(min: Int = 0, max: Int = -1, c: Char): Parser<StringSlice> =
    trace("takeWhile(min=$min, max=$max, oneOf=${forTrace(c)})", takeWhilePredicate(min, max) { it == c })

fun takeUntil(min: Int = 0, max: Int = -1, predicate: (Char) -> Boolean) =
    trace("takeUntil(min=$min, max=$max)", takeWhilePredicate(min, max) { !predicate(it) })

fun takeUntil(min: Int = 0, max: Int = -1, oneOf: Set<Char>) =
    trace("takeUntil(min=$min, max=$max, oneOf=${forTrace(oneOf)})", takeWhile(min, max) { it !in oneOf })

private fun takeExactlyWhilePredicate(min: Int, max: Int, predicate: (Char) -> Boolean): Parser<StringSlice> = { input ->
    var len = 0
    while (len < input.length && predicate(input[len])) {
        len += 1
    }
    when {
        len < min -> Result.Error("$len were recognized, >= $min required!", input)
        len > max -> Result.Error("$len were recognized, <= $max required!", input)
        else -> Result.Ok(input.subSlice(0, len), input.subSlice(len))
    }
}

fun takeExactlyWhile(min: Int, max: Int, oneOf: Set<Char>) =
    trace("takeExactlyWhile(min=$min, max=$max, oneOf=${forTrace(oneOf)})", takeExactlyWhilePredicate(min, max, oneOf::contains))

private fun takeOneChar(predicate: (Char) -> Boolean): Parser<StringSlice> = { input ->
    if (input.isEmpty()) Result.Error("no input left!", input)
    else {
        val c = input[0]
        if (predicate(c)) Result.Ok(input.subSlice(0, 1), input.subSlice(1))
        else Result.Error("not recognized!", input)
    }
}

fun take(c: Char): Parser<StringSlice> = trace("take('$c')", takeOneChar { it == c })

fun take(chars: Set<Char>): Parser<StringSlice> =
    trace("take(chars=${forTrace(chars)})", takeOneChar(chars::contains))

fun take(predicate: (Char) -> Boolean): Parser<StringSlice> =
    trace("take(predicate=...)", takeOneChar(predicate))

fun takeString(s: CharSequence): Parser<StringSlice> = trace("take(s=\"${forTrace(s, "\"")}\")") { input ->
    if (input.startsWith(s)) Result.Ok(input.subSlice(0, s.length), input.subSlice(s.length))
    else Result.Error("$s was not recognized!", input)
}

fun takeString(strings: Set<CharSequence>): Parser<StringSlice> = trace("take(strings=${forTrace(strings)})") { input ->
    val firstMatching = strings.firstOrNull { input.startsWith(it) }
    if (firstMatching == null) Result.Error("No string matched!", input)
    else Result.Ok(input.subSlice(0, firstMatching.length), input.subSlice(firstMatching.length))
}

fun entireSliceOf(parser: Parser<*>): Parser<StringSlice> = trace("entireSliceOf()")  { input ->
    when (val result = parser(input)) {
        is Result.Ok -> Result.Ok(input.subSlice(0, input.length - result.rest.length), result.rest)
        is Result.Error -> result
    }
}

fun <T> parseEntirely(parser: Parser<T>): Parser<T> = { input ->
    when (val result = parser(input)) {
        is Result.Ok -> {
            if (result.rest.isEmpty()) result
            else Result.Error("Parser did not recognize entire input: ${result.rest}", input)
        }
        is Result.Error -> result
    }
}
