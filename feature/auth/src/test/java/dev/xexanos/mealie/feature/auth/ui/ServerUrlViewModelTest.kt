package dev.xexanos.mealie.feature.auth.ui

import app.cash.turbine.test
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.ui.R
import dev.xexanos.mealie.feature.auth.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class ServerUrlViewModelTest {

    // --- normalizeUrl tests ---

    @Test
    fun `normalizeUrl strips trailing slash`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("https://mealie.example.com/"))
    }

    @Test
    fun `normalizeUrl strips multiple trailing slashes`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("https://mealie.example.com///"))
    }

    @Test
    fun `normalizeUrl prepends https to bare IP with port`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://192.168.1.100:9925", vm.normalizeUrl("192.168.1.100:9925"))
    }

    @Test
    fun `normalizeUrl prepends https to bare hostname`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.local", vm.normalizeUrl("mealie.local"))
    }

    @Test
    fun `normalizeUrl returns null for blank input`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertNull(vm.normalizeUrl("   "))
    }

    @Test
    fun `normalizeUrl returns null for empty string`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertNull(vm.normalizeUrl(""))
    }

    @Test
    fun `normalizeUrl handles uppercase HTTP scheme without double-prefixing`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("http://mealie.example.com", vm.normalizeUrl("HTTP://mealie.example.com"))
    }

    @Test
    fun `normalizeUrl handles mixed-case HTTPS scheme`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("Https://mealie.example.com"))
    }

    @Test
    fun `normalizeUrl strips known Mealie UI path`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("https://mealie.example.com/dashboard"))
    }

    @Test
    fun `normalizeUrl strips login path`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("https://mealie.example.com/login"))
    }

    @Test
    fun `normalizeUrl strips api path`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://mealie.example.com", vm.normalizeUrl("https://mealie.example.com/api/app/about"))
    }

    @Test
    fun `normalizeUrl preserves non-Mealie subpath for reverse proxy`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://example.com/mealie", vm.normalizeUrl("https://example.com/mealie"))
    }

    @Test
    fun `normalizeUrl preserves custom subpath with trailing slash stripped`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://example.com/apps/mealie", vm.normalizeUrl("https://example.com/apps/mealie/"))
    }

    @Test
    fun `normalizeUrl handles IPv6 address with port`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertEquals("https://[::1]:9925", vm.normalizeUrl("[::1]:9925"))
    }

    @Test
    fun `normalizeUrl returns null for port zero`() {
        val vm = ServerUrlViewModel(FakeAuthRepository())
        assertNull(vm.normalizeUrl("mealie.local:0"))
    }

    // --- onConnect: malformed input ---

    @Test
    fun `onConnect with malformed input sets InputError and does not call probe`() = runTest {
        val fake = FakeAuthRepository()
        val vm = ServerUrlViewModel(fake)
        vm.onConnect("")
        val state = vm.uiState.value as ServerUrlUiState.InputError
        assertEquals(R.string.setup_url_error_invalid, state.messageResId)
        assertEquals(0, fake.probeCallCount)
    }

    // --- onConnect: probe outcomes ---

    @Test
    fun `onConnect with valid url on Success emits NavigateToNext`() = runTest {
        val fake = FakeAuthRepository(probeResult = UrlProbeResult.Success)
        val vm = ServerUrlViewModel(fake)
        vm.events.test {
            // consume the init check event (no stored URL, so no event emitted)
            vm.onConnect("https://mealie.example.com")
            assertEquals(ServerUrlUiEvent.NavigateToNext, awaitItem())
        }
    }

    @Test
    fun `onConnect on NetworkError sets InputError with correct message`() = runTest {
        val fake = FakeAuthRepository(probeResult = UrlProbeResult.NetworkError)
        val vm = ServerUrlViewModel(fake)
        vm.onConnect("https://mealie.example.com")
        val state = vm.uiState.value as ServerUrlUiState.InputError
        assertEquals(R.string.setup_url_error_unreachable, state.messageResId)
    }

    @Test
    fun `onConnect on NotMealieServer sets InputError with correct message`() = runTest {
        val fake = FakeAuthRepository(probeResult = UrlProbeResult.NotMealieServer)
        val vm = ServerUrlViewModel(fake)
        vm.onConnect("https://mealie.example.com")
        val state = vm.uiState.value as ServerUrlUiState.InputError
        assertEquals(R.string.setup_url_error_not_mealie, state.messageResId)
    }

    // --- init with stored URL ---

    @Test
    fun `init with stored URL emits NavigateToNext without calling onConnect`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "https://mealie.example.com")
        val vm = ServerUrlViewModel(fake)
        vm.events.test {
            assertEquals(ServerUrlUiEvent.NavigateToNext, awaitItem())
        }
        assertEquals(0, fake.probeCallCount)
    }
}
