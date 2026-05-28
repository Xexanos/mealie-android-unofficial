package dev.xexanos.mealie.core.network.di

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("redactSensitiveContent")
class RedactSensitiveContentTest {

    @Nested
    @DisplayName("When message contains a password field")
    inner class PasswordRedaction {

        @Test
        @DisplayName("Then password value is redacted from form-urlencoded body")
        fun redactsPasswordFromFormBody() {
            val input = "username=lars%40winterhalderhome.de&password=SuperSecret123%21&remember_me=true"

            val result = redactSensitiveContent(input)

            assertEquals(
                "username=lars%40winterhalderhome.de&password=<redacted>&remember_me=true",
                result
            )
        }

        @Test
        @DisplayName("Then password at end of body is redacted")
        fun redactsPasswordAtEndOfBody() {
            val input = "username=user%40example.com&password=MyP%40ssw0rd"

            val result = redactSensitiveContent(input)

            assertEquals("username=user%40example.com&password=<redacted>", result)
        }
    }

    @Nested
    @DisplayName("When message contains an access_token field")
    inner class AccessTokenRedaction {

        @Test
        @DisplayName("Then access_token value is redacted from JSON response")
        fun redactsAccessTokenFromJson() {
            val input = """{"access_token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.abc123","token_type":"bearer"}"""

            val result = redactSensitiveContent(input)

            assertEquals("""{"access_token":"<redacted>","token_type":"bearer"}""", result)
            assertFalse(result.contains("eyJ"))
        }
    }

    @Nested
    @DisplayName("When message contains no sensitive content")
    inner class NoRedaction {

        @Test
        @DisplayName("Then message is returned unchanged")
        fun leavesNonSensitiveMessagesUnchanged() {
            val input = "--> POST https://kochbuch.winterhalderhome.de/api/auth/token"

            val result = redactSensitiveContent(input)

            assertEquals(input, result)
        }

        @Test
        @DisplayName("Then headers are returned unchanged")
        fun leavesHeadersUnchanged() {
            val input = "Content-Type: application/x-www-form-urlencoded"

            val result = redactSensitiveContent(input)

            assertEquals(input, result)
        }
    }
}
