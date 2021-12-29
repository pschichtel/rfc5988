package tel.schich.rfc5988.parsing

sealed interface Result<out T> {

    val rest: StringSlice

    data class Ok<T>(val value: T, override val rest: StringSlice) : Result<T>
    data class Error(val message: String, override val rest: StringSlice) : Result<Nothing>
}