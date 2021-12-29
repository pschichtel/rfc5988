package tel.schich.rfc5988

import tel.schich.rfc5988.parsing.Parser
import tel.schich.rfc5988.parsing.andThenIgnore
import tel.schich.rfc5988.parsing.andThenTake
import tel.schich.rfc5988.parsing.concat
import tel.schich.rfc5988.parsing.flatMap
import tel.schich.rfc5988.parsing.map
import tel.schich.rfc5988.parsing.optional
import tel.schich.rfc5988.parsing.or
import tel.schich.rfc5988.parsing.parseRepeatedly
import tel.schich.rfc5988.parsing.parseSeparated
import tel.schich.rfc5988.parsing.parseSeparatedList
import tel.schich.rfc5988.parsing.surroundedBy
import tel.schich.rfc5988.parsing.take
import tel.schich.rfc5988.parsing.takeString
import tel.schich.rfc5988.parsing.takeFirst
import tel.schich.rfc5988.parsing.takeUntil
import tel.schich.rfc5988.parsing.takeWhile
import tel.schich.rfc5988.parsing.then
import tel.schich.rfc5988.parsing.trace
import tel.schich.rfc5988.rfc2616.Alpha
import tel.schich.rfc5988.rfc2616.Digit
import tel.schich.rfc5988.rfc2616.LowercaseAlpha
import tel.schich.rfc5988.rfc2616.SP
import tel.schich.rfc5988.rfc2616.parseCommaSeparatedList
import tel.schich.rfc5988.rfc2616.parseQuotedString
import tel.schich.rfc5988.rfc2616.takeWithLws
import tel.schich.rfc5988.rfc3986.parseUri
import tel.schich.rfc5988.rfc3986.parseUriReference
import tel.schich.rfc5988.rfc4288.parseRegName
import tel.schich.rfc5988.rfc5646.parseLanguageTag
import tel.schich.rfc5988.rfc5987.parseExtValue
import tel.schich.rfc5988.rfc5987.parseParamName

private val paramTokenChars = Alpha + Digit + setOf(
    '!', '#', '$', '%', '&', '\'', '(',
    ')', '*', '+', '-', '.', '/',
    ':', '<', '=', '>', '?', '@',
    '[', ']', '^', '_', '`', '{', '|',
    '}', '~',
)

private fun <T> quoted(parser: Parser<T>): Parser<T> = parser.surroundedBy(take('"'))

private val parseParamToken = takeWhile(min = 1, oneOf = paramTokenChars).map { it.toString() }

private val parseMediaType = parseSeparated(parseRegName, take('/'), parseRegName)
    .map { (type, subtype) -> MediaType(type.toString(), subtype.toString()) }

private val parseQuotedMediaType = quoted(parseMediaType)

private val parseLowerAlpha = take(LowercaseAlpha)
private val parseRegRelType = trace("reg-rel-type",
    parseLowerAlpha.concat(takeWhile(oneOf = Digit + LowercaseAlpha + setOf('.', '-')))
        .map { it.toString() }
)

private fun isRelationTypeFollow(c: Char) = c == '"' || c == ';' || c == ','

private val parseExtRelType: Parser<String> = trace("ext-rel-type", parseUri(::isRelationTypeFollow))

private val parseRelationType = trace("relation-type", parseExtRelType or parseRegRelType)

private val parseRelationTypes: Parser<List<String>> = trace("relation-types",
    takeFirst(
        quoted(parseSeparatedList(parseRelationType, takeWhile(min = 1, c = SP), min = 1)),
        parseRelationType.map { listOf(it) },
    )
)

private val parseExtNameStar = parseParamName then take('*')

private fun <T : Any> parseParameter(name: String, value: Parser<T>): Parser<T> =
    takeString(name).andThenIgnore(takeWithLws('=')).andThenTake(value)

private val parseRelParam: Parser<Parameter.Relation> =
    trace("rel-param", parseParameter("rel", parseRelationTypes.map(Parameter::Relation)))

private fun isQuote(c: Char) = c == '"'

private val parseAnchorParam: Parser<Parameter.Anchor> =
    trace("anchor-param", parseParameter("anchor", quoted(parseUriReference(::isQuote)).map(Parameter::Anchor)))

private val parseRevParam: Parser<Parameter.ReverseRelation> =
    parseParameter("rev", parseRelationTypes.map(Parameter::ReverseRelation))

private val parseHrefLangParam: Parser<Parameter.HrefLanguage> =
    parseParameter("hreflang", parseLanguageTag.map(Parameter::HrefLanguage))

private val parseMediaParam: Parser<Parameter.Media> =
    parseParameter("media", takeUntil(min = 1) { it == ';' }.map { Parameter.Media(it.toString()) })

private val parseTitleParam: Parser<Parameter.Title> =
    parseParameter("title", parseQuotedString.map(Parameter::Title))

private val parseTitleStarParam: Parser<Parameter.TitleStar> =
    parseParameter("title*", parseExtValue.map(Parameter::TitleStar))

private val parseTypeParam: Parser<Parameter.Type> =
    parseParameter("type", (parseMediaParam or parseQuotedMediaType).map { Parameter.Type(it.toString()) })

private val parseLinkExtensionStar: Parser<Parameter.ExtensionStar> =
    (parseExtNameStar then takeWithLws('=')).flatMap { name ->
        parseExtValue.map { ext -> Parameter.ExtensionStar(name.toString(), ext) }
    }

private val parseLinkExtension: Parser<Parameter.Extension> = parseParamName.flatMap { name ->
    takeWithLws('=').andThenTake(parseParamToken or parseQuotedString).optional()
        .map { ext -> Parameter.Extension(name.toString(), ext) }
}

private val parseLinkParam: Parser<Parameter> = takeFirst(
    parseRelParam,
    parseAnchorParam,
    parseRevParam,
    parseHrefLangParam,
    parseMediaParam,
    parseTitleParam,
    parseTitleStarParam,
    parseTypeParam,
    parseLinkExtension,
    parseLinkExtensionStar,
)

private fun removeDuplicateTitleStar(parameters: List<Parameter>): List<Parameter> {
    val output = mutableListOf<Parameter>()

    var titleStarSeen = false
    for (parameter in parameters) {
        if (parameter is Parameter.TitleStar) {
            if (titleStarSeen) {
                continue
            }
            titleStarSeen = true
        }
        output.add(parameter)
    }

    return output.toList()
}

private fun isClosingAngle(c: Char) = c == '>'

private val parseLinkValue = parseUriReference(::isClosingAngle).surroundedBy(takeWithLws('<'), takeWithLws('>'))
    .flatMap { uriReference ->
        val parseSeparator = takeWithLws(';')
        parseRepeatedly(parseSeparator.andThenTake(parseLinkParam)).map { parameters ->
            Link(uriReference, removeDuplicateTitleStar(parameters))
        }
    }

val parseLink = parseCommaSeparatedList(parser = parseLinkValue)
