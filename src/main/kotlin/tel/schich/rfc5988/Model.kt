package tel.schich.rfc5988

import tel.schich.rfc5988.rfc5646.LanguageTag
import tel.schich.rfc5988.rfc5987.ExtendedValue

data class MediaType(val typeName: String, val subTypeName: String)

sealed class Parameter(val name: String) {
    data class Relation(val names: List<String>) : Parameter(NAME) {
        companion object {
            const val NAME = "rel"
        }
    }
    data class Anchor(val reference: String) : Parameter(NAME) {
        companion object {
            const val NAME = "anchor"
        }
    }
    data class ReverseRelation(val names: List<String>) : Parameter(NAME) {
        companion object {
            const val NAME = "rev"
        }
    }
    data class HrefLanguage(val languageTag: LanguageTag) : Parameter(NAME) {
        companion object {
            const val NAME = "hreflang"
        }
    }
    data class Media(val mediaDesc: String) : Parameter(NAME) {
        companion object {
            const val NAME = "media"
        }
    }
    data class Title(val value: String) : Parameter(NAME) {
        companion object {
            const val NAME = "title"
        }
    }
    data class TitleStar(val value: ExtendedValue) : Parameter(NAME) {
        companion object {
            const val NAME = "title*"
        }
    }
    data class Type(val mediaType: MediaType) : Parameter(NAME) {
        companion object {
            const val NAME = "type"
        }
    }
    data class Extension(val fullName: String, val value: String?) : Parameter(fullName)
    data class ExtensionStar(val fullName: String, val value: ExtendedValue) : Parameter("$fullName*")
}

data class Link(val target: String, val parameters: List<Parameter>)