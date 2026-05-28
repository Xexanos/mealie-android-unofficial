package dev.xexanos.mealie.e2e

import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before

abstract class E2ETestBase {

    protected val backend: E2EBackend by lazy { E2EBackend.fromInstrumentationArgs() }

    @Before
    fun assumeBackendReachable() {
        if (backend is E2EBackend.Live) {
            val reachable = runBlocking { backend.healthCheck() }
            Assume.assumeTrue("Live backend unreachable - skipping", reachable)
        }
    }

    protected fun assumeWireMockOnly() {
        Assume.assumeTrue(
            "Test requires WireMock backend - skipping in live mode",
            backend is E2EBackend.WireMock,
        )
    }
}
