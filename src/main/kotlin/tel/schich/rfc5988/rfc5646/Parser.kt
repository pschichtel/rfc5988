package tel.schich.rfc5988.rfc5646

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.andThenTake
import tel.schich.rfc5988.parsing.concat
import tel.schich.rfc5988.parsing.entireSliceOf
import tel.schich.rfc5988.parsing.flatMap
import tel.schich.rfc5988.parsing.forTrace
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.parseSeparatedList
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeExactlyWhile
import tel.schich.rfc5988.parsing.takeString
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
).map { LanguageTag.Grandfathered.Regular(it.toString()) }

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
).map { LanguageTag.Grandfathered.Irregular(it.toString()) }

private val parseGrandfathered: Parser<LanguageTag.Grandfathered> = parseIrregular or parseRegular

private val parsePrivateUse =
    entireSliceOf(take('x') then parseRepeatedly(take('-') concat takeExactlyWhile(1, 8, AlphaNumericChars), 1))

private val parseSingleton: Parser<Char> = take(SingletonChars).map { it[0] }

private val parseExtension: Parser<Extension> = parseSingleton.flatMap { prefix ->
    take('-').andThenTake(parseSeparatedList(takeExactlyWhile(2, 8, AlphaNumericChars), take('-'), 1)).map { parts ->
        Extension(prefix, parts.map { it.toString() })
    }
}

private val parseVariant = (takeExactlyWhile(5, 8, AlphaNumericChars) or (take(Digit) concat takeExactlyWhile(3, 3, AlphaNumericChars)))

private val parseRegion = (takeExactlyWhile(2, 2, Alpha) or takeExactlyWhile(3, 3, Digit))

private val parseScript = takeExactlyWhile(4, 4, Alpha)

private val parseExtlang = entireSliceOf(takeExactlyWhile(3, 3, Alpha) then parseRepeatedly(take('-') then takeExactlyWhile(3, 3, Alpha), 0, 2))

private val parseLanguage: Parser<Language> = takeExactlyWhile(2, 3, Alpha).flatMap { primary ->
    take('-').andThenTake(parseExtlang).optional().map { extended ->
        Language(primary.toString(), extended?.toString())
    }
}

private val parseLangtag: Parser<LanguageTag.Simple> = parseLanguage.flatMap { language ->
    take('-').andThenTake(parseScript).optional().flatMap { script ->
        take('-').andThenTake(parseRegion).optional().flatMap { region ->
            parseRepeatedly(take('-').andThenTake(parseVariant)).flatMap { variants ->
                parseRepeatedly(take('-').andThenTake(parseExtension)).flatMap { extensions ->
                    take('-').andThenTake(parsePrivateUse).optional().flatMap { privateUse ->
                        { input ->
                            val extensionMap = extensions.groupBy { it.prefix }
                            if (extensionMap.any { it.value.size > 1 }) {
                                Result.Error("Duplicated extensions exists: $extensionMap", input)
                            } else {
                                val tag = LanguageTag.Simple(
                                    language,
                                    script?.toString(),
                                    region?.toString(),
                                    variants.map { it.toString() },
                                    extensionMap.mapValues { (_, value) -> value.first() },
                                    privateUse?.toString(),
                                )
                                Result.Ok(tag, input)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val parsePrivateUseTag: Parser<LanguageTag.PrivateUse> =
    parsePrivateUse.map { LanguageTag.PrivateUse(it.toString()) }

val parseLanguageTag: Parser<LanguageTag> = parseLangtag or parsePrivateUseTag or parseGrandfathered

data class Language(val primary: String, val extended: String? = null) {
    override fun toString(): String {
        val extendedTag = extended?.let { "-$it" } ?: ""
        return "$primary$extendedTag"
    }
}

data class Extension(val prefix: Char, val parts: List<String>) {
    override fun toString(): String = "$prefix-${parts.joinToString("-")}"
}

sealed interface LanguageTag {
    data class Simple(
        val language: Language,
        val script: String? = null,
        val region: String? = null,
        val variants: List<String> = emptyList(),
        val extensions: Map<Char, Extension> = emptyMap(),
        val privateUse: String? = null,
    ) : LanguageTag {
        override fun toString(): String {

            fun formatTag(value: String) = "-$value"
            fun formatOptionalTag(value: String?) = value?.let(::formatTag) ?: ""

            val languageTag = language.toString()
            val scriptTag = formatOptionalTag(script)
            val regionTag = formatOptionalTag(region)
            val variantsTags = variants.joinToString(separator = "", transform = ::formatTag)
            val extensionsTags = extensions.values
                .map(Extension::toString)
                .joinToString(separator = "", transform = ::formatTag)
            val privateUseTag = formatOptionalTag(privateUse)

            return "$languageTag$scriptTag$regionTag$variantsTags$extensionsTags$privateUseTag"
        }
    }

    data class PrivateUse(val name: String) : LanguageTag {
        override fun toString(): String = name
    }

    sealed interface Grandfathered : LanguageTag {
        data class Irregular(val name: String) : Grandfathered {
            override fun toString(): String = name
        }
        data class Regular(val name: String) : Grandfathered {
            override fun toString(): String = name
        }
    }
}