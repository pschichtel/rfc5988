package tel.schich.rfc5988

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tel.schich.rfc5988.parsing.Result
import tel.schich.rfc5988.parsing.invoke
import tel.schich.rfc5988.rfc5646.Language
import tel.schich.rfc5988.rfc5646.LanguageTag
import kotlin.test.assertIs

internal class ParserKtTest {

    private fun testExample(input: String, expected: List<Link>) {
        val result = parseLink(input.replace("\n", "\r\n"))
        assertIs<Result.Ok<List<Link>>>(result)
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
                        Parameter.TitleStar(ExtValue(Charsets.UTF_8, de, "letztes Kapitel")),
                    ),
                ),
                Link(
                    "/TheBook/chapter4",
                    listOf(
                        Parameter.Relation(listOf("next")),
                        Parameter.TitleStar(ExtValue(Charsets.UTF_8, de, "nächstes Kapitel")),
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
}