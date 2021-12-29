package tel.schich.rfc5988

import tel.schich.rfc5988.rfc5646.LanguageTag
import java.nio.charset.Charset

data class ExtValue(val charset: Charset, val language: LanguageTag?, val value: String)

data class MediaType(val typeName: String, val subTypeName: String)

sealed interface Parameter {
    data class Relation(val names: List<String>) : Parameter
    data class Anchor(val reference: String) : Parameter
    data class ReverseRelation(val names: List<String>) : Parameter
    data class HrefLanguage(val languageTag: LanguageTag) : Parameter
    data class Media(val mediaDesc: String) : Parameter
    data class Title(val value: String) : Parameter
    data class TitleStar(val value: ExtValue) : Parameter
    data class Type(val mediaType: String) : Parameter
    data class Extension(val name: String, val value: String?) : Parameter
    data class ExtensionStar(val name: String, val value: ExtValue) : Parameter
}

data class Link(val target: String, val parameters: List<Parameter>)