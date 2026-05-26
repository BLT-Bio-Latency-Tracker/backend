package com.medilux.blt.global.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HashUtilsTest {
    @Test
    fun `sha256Hex returns known lowercase hex digest`() {
        val hash = HashUtils.sha256Hex("abc")

        assertThat(hash)
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test
    fun `sha256Hex always returns 64 hex characters`() {
        val hash = HashUtils.sha256Hex("apple-sub-with-negative-digest-bytes")

        assertThat(hash).hasSize(64)
        assertThat(hash).matches("^[0-9a-f]{64}$")
    }
}
