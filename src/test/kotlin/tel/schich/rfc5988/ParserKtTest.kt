package tel.schich.rfc5988

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tel.schich.parserkombinator.ParserResult
import tel.schich.parserkombinator.invoke
import tel.schich.rfc5988.rfc5646.Language
import tel.schich.rfc5988.rfc5646.LanguageTag
import tel.schich.rfc5988.rfc5987.ExtendedValue
import kotlin.test.assertIs
import kotlin.text.Charsets.UTF_8

internal class ParserKtTest {

    private fun testExample(input: String, expected: List<Link>) {
        val result = parseLink(input.replace("\n", "\r\n"))
        assertIs<ParserResult.Ok<List<Link>>>(result)
        assertEquals(expected, result.value)
        assertTrue(result.rest.isEmpty())
    }

    @Test
    fun firstExampleFromSpec() {
        testExample(
            input = """<http://example.com/TheBook/chapter2>; rel="previous";
         title="previous chapter"""",
            expected = listOf(
                Link(
                    "http://example.com/TheBook/chapter2",
                    listOf(
                        Parameter.Relation(listOf("previous")),
                        Parameter.Title("previous chapter"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun secondExampleFromSpec() {
        testExample(
            input = """</>; rel="http://example.net/foo"""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Relation(listOf("http://example.net/foo")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun thirdExampleFromSpec() {
        val de = LanguageTag.Simple(language = Language(primary = "de"))
        testExample(
            input = """</TheBook/chapter2>;
         rel="previous"; title*=UTF-8'de'letztes%20Kapitel,
         </TheBook/chapter4>;
         rel="next"; title*=UTF-8'de'n%c3%a4chstes%20Kapitel""",
            expected = listOf(
                Link(
                    "/TheBook/chapter2",
                    listOf(
                        Parameter.Relation(listOf("previous")),
                        Parameter.TitleStar(ExtendedValue(UTF_8, de, "letztes Kapitel")),
                    ),
                ),
                Link(
                    "/TheBook/chapter4",
                    listOf(
                        Parameter.Relation(listOf("next")),
                        Parameter.TitleStar(ExtendedValue(UTF_8, de, "nächstes Kapitel")),
                    ),
                )
            ),
        )
    }

    @Test
    fun forthExampleFromSpec() {
        testExample(
            input = """<http://example.org/>;
             rel="start http://example.net/relation/other"""",
            expected = listOf(
                Link(
                    "http://example.org/",
                    listOf(
                        Parameter.Relation(listOf("start", "http://example.net/relation/other")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedRelParameter() {
        testExample(
            input = """</>; rel=a; rel=b""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Relation(listOf("a")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedAnchorParameter() {
        testExample(
            input = """</>; anchor="a"; anchor="b"""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Anchor("a"),
                        Parameter.Anchor("b"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedRevParameter() {
        testExample(
            input = """</>; rev=a; rev=b""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.ReverseRelation(listOf("a")),
                        Parameter.ReverseRelation(listOf("b")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedHreflangParameter() {
        testExample(
            input = """</>; hreflang=de-DE; hreflang=en-US""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.HrefLanguage(LanguageTag.Simple(Language(primary = "de"), region = "DE")),
                        Parameter.HrefLanguage(LanguageTag.Simple(Language(primary = "en"), region = "US")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedMediaParameter() {
        testExample(
            input = """</>; media="screen"; media="print"""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Media("screen"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedTitleParameter() {
        testExample(
            input = """</>; title="a"; title="b"""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Title("a"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedTitleStarParameter() {
        testExample(
            input = """</>; title*=UTF-8'de'a; title*=UTF-8'de'b""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.TitleStar(ExtendedValue(UTF_8, LanguageTag.Simple(Language(primary = "de")), "a")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedTypeParameter() {
        testExample(
            input = """</>; type=text/plain; type=text/html""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Type(MediaType("text", "plain")),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedExtensionParameter() {
        testExample(
            input = """</>; a=a; a=b""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.Extension("a", "a"),
                        Parameter.Extension("a", "b"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun duplicatedExtensionStarParameter() {
        testExample(
            input = """</>; a*=UTF-8'de'a; a*=UTF-8'de'b""",
            expected = listOf(
                Link(
                    "/",
                    listOf(
                        Parameter.ExtensionStar("a", ExtendedValue(UTF_8, LanguageTag.Simple(Language(primary = "de")), "a")),
                        Parameter.ExtensionStar("a", ExtendedValue(UTF_8, LanguageTag.Simple(Language(primary = "de")), "b")),
                    ),
                ),
            ),
        )
    }
}