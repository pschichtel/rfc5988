package tel.schich.rfc5988

import tel.schich.parserkombinator.Parser
import tel.schich.parserkombinator.andThenIgnore
import tel.schich.parserkombinator.andThenTake
import tel.schich.parserkombinator.concat
import tel.schich.parserkombinator.flatMap
import tel.schich.parserkombinator.map
import tel.schich.parserkombinator.optional
import tel.schich.parserkombinator.or
import tel.schich.parserkombinator.parseRepeatedly
import tel.schich.parserkombinator.parseSeparated
import tel.schich.parserkombinator.parseSeparatedList
import tel.schich.parserkombinator.surroundedBy
import tel.schich.parserkombinator.take
import tel.schich.parserkombinator.takeString
import tel.schich.parserkombinator.takeFirst
import tel.schich.parserkombinator.takeUntil
import tel.schich.parserkombinator.takeWhile
import tel.schich.parserkombinator.then
import tel.schich.parserkombinator.trace
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
import tel.schich.rfc5988.rfc5987.parseExtendedValue
import tel.schich.rfc5988.rfc5987.parseParamName

private val paramTokenChars = Alpha + Digit + setOf(
    '!', '#', '$', '%', '&', '\'', '(',
    ')', '*', '+', '-', '.', '/',
    ':', '<', '=', '>', '?', '@',
    '[', ']', '^', '_', '`', '{', '|',
    '}', '~',
)

private val parameterFollowSet = setOf(';', ',')

private fun <T> quoted(parser: Parser<T>): Parser<T> = parser.surroundedBy(take('"'))

private val parseParamToken = takeWhile(min = 1, oneOf = paramTokenChars).map { it.toString() }

private val parseMediaDesc = takeUntil(min = 1, oneOf = parameterFollowSet + '"').map { it.toString() }

private val parseMediaType = parseSeparated(parseRegName, take('/'), parseRegName)
    .map { (type, subtype) -> MediaType(type.toString(), subtype.toString()) }

private val parseQuotedMediaType = quoted(parseMediaType)

private val parseLowerAlpha = take(LowercaseAlpha)
private val parseRegRelType = trace("reg-rel-type",
    parseLowerAlpha.concat(takeWhile(oneOf = Digit + LowercaseAlpha + setOf('.', '-')))
        .map { it.toString() }
)

private val parseExtRelType: Parser<String> = trace("ext-rel-type", parseUri(parameterFollowSet + '"'))

private val parseRelationType = trace("relation-type", parseExtRelType or parseRegRelType)

private val parseRelationTypes: Parser<List<String>> = trace("relation-types",
    takeFirst(
        quoted(parseSeparatedList(parseRelationType, takeWhile(min = 1, c = SP), min = 1)),
        parseRelationType.map { listOf(it) },
    )
)

private val parseExtNameStar = (parseParamName then take('*')).map { it.toString() }

private fun <T : Any> parseParameter(name: String, value: Parser<T>): Parser<T> =
    takeString(name).andThenIgnore(takeWithLws('=')).andThenTake(value)

private val parseRelParam: Parser<Parameter.Relation> =
    parseParameter(Parameter.Relation.NAME, parseRelationTypes.map(Parameter::Relation))

private val parseAnchorParam: Parser<Parameter.Anchor> =
    parseParameter(Parameter.Anchor.NAME, quoted(parseUriReference(setOf('"'))).map(Parameter::Anchor))

private val parseRevParam: Parser<Parameter.ReverseRelation> =
    parseParameter(Parameter.ReverseRelation.NAME, parseRelationTypes.map(Parameter::ReverseRelation))

private val parseHrefLangParam: Parser<Parameter.HrefLanguage> =
    parseParameter(Parameter.HrefLanguage.NAME, parseLanguageTag.map(Parameter::HrefLanguage))

private val parseMediaParam: Parser<Parameter.Media> =
    parseParameter(Parameter.Media.NAME, (quoted(parseMediaDesc) or parseMediaDesc).map(Parameter::Media))

private val parseTitleParam: Parser<Parameter.Title> =
    parseParameter(Parameter.Title.NAME, parseQuotedString.map(Parameter::Title))

private val parseTitleStarParam: Parser<Parameter.TitleStar> =
    parseParameter(Parameter.TitleStar.NAME, parseExtendedValue.map(Parameter::TitleStar))

private val parseTypeParam: Parser<Parameter.Type> =
    parseParameter(Parameter.Type.NAME, (parseQuotedMediaType or parseMediaType).map { Parameter.Type(it) })

private val parseLinkExtensionStar: Parser<Parameter.ExtensionStar> =
    (parseExtNameStar then takeWithLws('=')).flatMap { name ->
        parseExtendedValue.map { ext -> Parameter.ExtensionStar(name, ext) }
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
    parseLinkExtensionStar,
    parseLinkExtension,
)

private val singletonParameters = setOf(
    Parameter.Relation.NAME,
    Parameter.Title.NAME,
    Parameter.TitleStar.NAME,
    Parameter.Media.NAME, // RFC suggests that duplicated media should be an error, not sure though
    Parameter.Type.NAME,
)

private fun removeParameterDuplicates(parameters: List<Parameter>): List<Parameter> {
    return parameters
        .groupBy { it.name }.mapValues { (name, params) ->
            if (name in singletonParameters) listOf(params.first())
            else params
        }
        .values
        .flatten()
}

private val parseLinkValue = parseUriReference(setOf('>')).surroundedBy(takeWithLws('<'), takeWithLws('>'))
    .flatMap { uriReference ->
        val parseSeparator = takeWithLws(';')
        parseRepeatedly(parseSeparator.andThenTake(parseLinkParam)).map { parameters ->
            Link(uriReference, removeParameterDuplicates(parameters))
        }
    }

val parseLink = parseCommaSeparatedList(parser = parseLinkValue)
