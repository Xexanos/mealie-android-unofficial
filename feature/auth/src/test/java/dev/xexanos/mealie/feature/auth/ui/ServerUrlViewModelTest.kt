package dev.xexanos.mealie.feature.auth.ui

import app.cash.turbine.test
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
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

    // --- onConnect: malformed input ---

    @Test
    fun `onConnect with malformed input sets InputError and does not call probe`() = runTest {
        val fake = FakeAuthRepository()
        val vm = ServerUrlViewModel(fake)
        vm.onConnect("")
        val state = vm.uiState.value
        assert(state is ServerUrlUiState.InputError)
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
        assertEquals("Could not reach server", state.message)
    }

    @Test
    fun `onConnect on NotMealieServer sets InputError with correct message`() = runTest {
        val fake = FakeAuthRepository(probeResult = UrlProbeResult.NotMealieServer)
        val vm = ServerUrlViewModel(fake)
        vm.onConnect("https://mealie.example.com")
        val state = vm.uiState.value as ServerUrlUiState.InputError
        assertEquals("Not a Mealie server", state.message)
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
