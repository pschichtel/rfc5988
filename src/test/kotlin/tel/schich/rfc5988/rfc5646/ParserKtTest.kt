package tel.schich.rfc5988.rfc5646

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tel.schich.parserkombinator.ParserResult
import tel.schich.parserkombinator.invoke
import tel.schich.parserkombinator.parseEntirely
import kotlin.test.assertIs

internal class ParserKtTest {

    private fun test(input: String, expected: LanguageTag) {
        val result = parseLanguageTag(input)
        assertIs<ParserResult.Ok<LanguageTag>>(result)
        assertEquals("", result.rest.toString())
        assertEquals(expected, result.value)
    }

    @Test
    fun simpleLanguageSubtag() {
        // Simple language subtag
        test("de", LanguageTag.Simple(language = Language(primary = "de")))
        test("fr", LanguageTag.Simple(language = Language(primary = "fr")))
        test("ja", LanguageTag.Simple(language = Language(primary = "ja")))
        test("i-enochian", LanguageTag.Grandfathered.Irregular("i-enochian"))
    }

    @Test
    fun languageSubtagPlusScriptSubtag() {
        //Language subtag plus Script subtag
        test("zh-Hant", LanguageTag.Simple(language = Language(primary = "zh"), script = "Hant"))
        test("zh-Hans", LanguageTag.Simple(language = Language(primary = "zh"), script = "Hans"))
        test("sr-Cyrl", LanguageTag.Simple(language = Language(primary = "sr"), script = "Cyrl"))
        test("sr-Latn", LanguageTag.Simple(language = Language(primary = "sr"), script = "Latn"))
    }

    @Test
    fun extendedLanguageSubtagsAndTheirPrimaryLanguageSubtagCounterparts() {
        // Extended language subtags and their primary language subtag counterparts
        test("zh-cmn-Hans-CN", LanguageTag.Simple(language = Language(primary = "zh", extended = "cmn"), script = "Hans", region = "CN"))
        test("cmn-Hans-CN", LanguageTag.Simple(language = Language(primary = "cmn"), script = "Hans", region = "CN"))
        test("zh-yue-HK", LanguageTag.Simple(language = Language(primary = "zh", extended = "yue"), region = "HK"))
        test("yue-HK", LanguageTag.Simple(language = Language(primary = "yue"), region = "HK"))
    }

    @Test
    fun languageScriptRegion() {
        // Language-Script-Region
        test("zh-Hans-CN", LanguageTag.Simple(language = Language(primary = "zh"), script = "Hans", region = "CN"))
        test("sr-Latn-RS", LanguageTag.Simple(language = Language(primary = "sr"), script = "Latn", region = "RS"))
    }

    @Test
    fun languageVariant() {
        // Language-Variant
        test("sl-rozaj", LanguageTag.Simple(language = Language(primary = "sl"), variants = listOf("rozaj")))
        test("sl-rozaj-biske", LanguageTag.Simple(language = Language(primary = "sl"), variants = listOf("rozaj", "biske")))
        test("sl-nedis", LanguageTag.Simple(language = Language(primary = "sl"), variants = listOf("nedis")))
    }

    @Test
    fun languageRegionVariant() {
        // Language-Region-Variant
        test("de-CH-1901", LanguageTag.Simple(language = Language(primary = "de"), region = "CH", variants = listOf("1901")))
        test("sl-IT-nedis", LanguageTag.Simple(language = Language(primary = "sl"), region = "IT", variants = listOf("nedis")))
    }

    @Test
    fun languageScriptRegionVariant() {
        // Language-Script-Region-Variant
        test("hy-Latn-IT-arevela", LanguageTag.Simple(language = Language(primary = "hy"), script = "Latn", region = "IT", variants = listOf("arevela")))
    }

    @Test
    fun languageRegion() {
        // Language-Region
        test("de-DE", LanguageTag.Simple(language = Language(primary = "de"), region = "DE"))
        test("en-US", LanguageTag.Simple(language = Language(primary = "en"), region = "US"))
        test("es-419", LanguageTag.Simple(language = Language(primary = "es"), region = "419"))
    }

    @Test
    fun privateUseSubtags() {
        // Private use subtags
        test("de-CH-x-phonebk", LanguageTag.Simple(language = Language(primary = "de"), region = "CH", privateUse = "x-phonebk"))
        test("az-Arab-x-AZE-derbend", LanguageTag.Simple(language = Language(primary = "az"), script = "Arab", privateUse = "x-AZE-derbend"))
    }

    @Test
    fun privateUseRegistryValues() {
        // Private use registry values
        test("x-whatever", LanguageTag.PrivateUse("x-whatever"))
        test("qaa-Qaaa-QM-x-southern", LanguageTag.Simple(language = Language(primary = "qaa"), script = "Qaaa", region = "QM", privateUse = "x-southern"))
        test("de-Qaaa", LanguageTag.Simple(language = Language(primary = "de"), script = "Qaaa"))
        test("sr-Latn-QM", LanguageTag.Simple(language = Language(primary = "sr"), script = "Latn", region = "QM"))
        test("sr-Qaaa-RS", LanguageTag.Simple(language = Language(primary = "sr"), script = "Qaaa", region = "RS"))
    }

    @Test
    fun tagsThatUseExtensions() {
        // Tags that use extensions (examples ONLY -- extensions MUST be defined by revision or update to this document, or by RFC)
        test("en-US-u-islamcal", LanguageTag.Simple(language = Language(primary = "en"), region = "US", extensions = mapOf('u' to Extension('u', listOf("islamcal")))))
        test("zh-CN-a-myext-x-private", LanguageTag.Simple(language = Language(primary = "zh"), region = "CN", extensions = mapOf('a' to Extension('a', listOf("myext"))), privateUse = "x-private"))
        test("en-a-myext-b-another", LanguageTag.Simple(language = Language(primary = "en"), extensions = mapOf('a' to Extension('a', listOf("myext")), 'b' to Extension('b', listOf("another")))))
    }

    private fun testFailure(input: String) {
        val result = parseEntirely(parseLanguageTag)(input)
        assertIs<ParserResult.Error>(result)
        assertNotEquals("", result.rest.toString())
    }

    @Test
    fun someInvalidTags() {
        // Some Invalid Tags

        // two region tags
        testFailure("de-419-DE")

        // use of a single-character subtag in primary position; note
        // that there are a few grandfathered tags that start with "i-" that
        // are valid
        testFailure("a-DE")

        // two extensions with same single-letter prefix
        testFailure("ar-a-aaa-b-bbb-a-ccc")
    }

    private fun loopbackTest(input: String) {
        val result = parseLanguageTag(input)
        assertIs<ParserResult.Ok<LanguageTag>>(result)
        assertEquals("", result.rest.toString())
        assertEquals(input, result.value.toString())
    }

    @Test
    fun serialization() {
        loopbackTest("de-DE")
        loopbackTest("zh-CN-a-myext-x-private")
        loopbackTest("az-Arab-x-AZE-derbend")
        loopbackTest("hy-Latn-IT-arevela")
        loopbackTest("ar-b-bbb-a-ccc")
    }
}