package tel.schich.rfc5988.rfc2234

import tel.schich.rfc5988.rfc2616.Digit

val HiHexDigitChars = Digit + ('A'..'F').toSet()
val LoHexDigitChars = Digit + ('a'..'f').toSet()
val HexDigitChars = HiHexDigitChars + LoHexDigitChars