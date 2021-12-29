package tel.schich.rfc5988.parsing

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class StringSliceTest {

    @Test
    fun identity() {
        val string = "abc"
        val slice = StringSlice.of(string)
        assertSame(string, slice.toString())
    }

    @Test
    fun subSequence() {
        val string = "abc"
        val slice = StringSlice.of(string)

        assertSame(slice, slice.subSequence(0, 3))
        assertEquals("a", slice.subSequence(0, 1).toString())
    }

    @Test
    fun subSlice() {
        val string = "abc"
        val slice = StringSlice.of(string)

        assertSame(slice, slice.subSlice(0))
        assertSame(slice, slice.subSlice(0, 3))
        assertEquals("a", slice.subSlice(0, 1).toString())
        assertEquals("b", slice.subSlice(1, 1).toString())
        assertEquals("c", slice.subSlice(2).toString())
    }
}