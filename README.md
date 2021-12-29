# rfc5988

This is a simple library that parses `Link` headers in accordance to [RFC 5988](https://datatracker.ietf.org/doc/html/rfc5988). The parser should also be compatible with the newer [RFC 8288](https://datatracker.ietf.org/doc/html/rfc8288) (see [Appendix C](https://datatracker.ietf.org/doc/html/rfc8288#appendix-C)).

This RFC references several other standards as per [Section 1](https://datatracker.ietf.org/doc/html/rfc5988#section-1):

* [RFC 2616](https://datatracker.ietf.org/doc/html/rfc2616): Several ABNF definitions have been implemented
* [RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986): `URI` and `URI-Reference` defintions, neither of which have been implemented in this library
* [RFC 4288](https://datatracker.ietf.org/doc/html/rfc4288): type-name and subtype-name have been implemented
* [W3C HTML 4.01](https://www.w3.org/TR/html401/): `MediaDesc` is referenced, but not implemented in this libary
* [RFC 5646](https://datatracker.ietf.org/doc/html/rfc5646): `Language-Tag` is referenced and fully implemented here including a test suite based on the RFC examples
* [RFC 5987](https://datatracker.ietf.org/doc/html/rfc5987): `ext-value` and `parmname` are referenced and fully implemented here
