/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.feature.toggles.api

import com.duckduckgo.appbuildconfig.api.BuildFlavor
import com.duckduckgo.feature.toggles.api.Toggle.State
import com.duckduckgo.feature.toggles.api.Toggle.State.Target
import java.lang.IllegalStateException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeatureTogglesTest {

    private lateinit var feature: TestFeature
    private lateinit var provider: FakeProvider
    private lateinit var toggleStore: FakeToggleStore

    @Before
    fun setup() {
        provider = FakeProvider()
        toggleStore = FakeToggleStore()
        feature = FeatureToggles.Builder()
            .store(toggleStore)
            .appVariantProvider { provider.variantKey }
            .appVersionProvider { provider.version }
            .flavorNameProvider { provider.flavorName }
            .forceDefaultVariantProvider { provider.variantKey = "" }
            .featureName("test")
            .build()
            .create(TestFeature::class.java)
    }

    @Test
    fun `disableByDefault - return disabled`() {
        assertFalse(feature.disableByDefault().isEnabled())
    }

    @Test
    fun `disableByDefault - set enabled - return enabled`() {
        feature.disableByDefault().setEnabled(Toggle.State(enable = true))
        assertTrue(feature.disableByDefault().isEnabled())
    }

    @Test
    fun `enabledByDefault - return enabled`() {
        assertTrue(feature.enabledByDefault().isEnabled())
    }

    @Test
    fun `enabledByDefault - set disabled - return disabled`() {
        feature.enabledByDefault().setEnabled(Toggle.State(enable = false))
        assertFalse(feature.enabledByDefault().isEnabled())
    }

    @Test(expected = IllegalStateException::class)
    fun `noDefaultValue - throw`() {
        feature.noDefaultValue().isEnabled()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `wrongReturnValue - throws - illegal argument exception`() {
        feature.wrongReturnValue()
    }

    @Test
    fun `enabledByDefault - not allowed min version - return disabled`() {
        provider.version = 10
        feature.enabledByDefault().setEnabled(Toggle.State(enable = true, minSupportedVersion = 11))
        assertFalse(feature.enabledByDefault().isEnabled())
    }

    @Test
    fun `enabledByDefault - allowed min version - return disabled`() {
        provider.version = 10
        feature.enabledByDefault().setEnabled(Toggle.State(enable = true, minSupportedVersion = 9))
        assertTrue(feature.enabledByDefault().isEnabled())
    }

    @Test
    fun `testInternalAlwaysEnabledAnnotation - always enabled`() {
        assertFalse(feature.internal().isEnabled())

        provider.flavorName = BuildFlavor.PLAY.name
        assertFalse(feature.internal().isEnabled())

        provider.flavorName = BuildFlavor.FDROID.name
        assertFalse(feature.internal().isEnabled())

        provider.flavorName = BuildFlavor.INTERNAL.name
        assertTrue(feature.internal().isEnabled())

        // enable the feature
        val enabledState = Toggle.State(
            remoteEnableState = true,
            rollout = null,
        )
        feature.internal().setEnabled(enabledState)
        provider.flavorName = BuildFlavor.PLAY.name
        assertTrue(feature.internal().isEnabled())
        provider.flavorName = BuildFlavor.FDROID.name
        assertTrue(feature.internal().isEnabled())
        provider.flavorName = BuildFlavor.INTERNAL.name
        assertTrue(feature.internal().isEnabled())

        feature.internal().setEnabled(enabledState.copy(remoteEnableState = false))
        provider.flavorName = BuildFlavor.PLAY.name
        assertFalse(feature.internal().isEnabled())
        provider.flavorName = BuildFlavor.FDROID.name
        assertFalse(feature.internal().isEnabled())
        provider.flavorName = BuildFlavor.INTERNAL.name
        assertTrue(feature.internal().isEnabled())
    }

    @Test
    fun `setVariantIfNullOnExperiment - forces default variant if null`() {
        toggleStore.set(
            "test_forcesDefaultVariant",
            State(
                targets = listOf(Target("na")),
            ),
        )
        assertNull(provider.variantKey)
        assertFalse(feature.experimentDisabledByDefault().isEnabled())
        assertEquals("", provider.variantKey)
    }

    @Test
    fun `testForcesDefaultVariantOnExperiment - variant key empty - default experiment disabled`() {
        assertNull(provider.variantKey)
        assertFalse(feature.experimentDisabledByDefault().isEnabled())
        assertEquals("", provider.variantKey)
    }

    @Test
    fun `doesNotForceDefaultVariant - feature flag disabled - null variant key`() {
        assertNull(provider.variantKey)
        assertFalse(feature.disableByDefault().isEnabled())
        assertNull(provider.variantKey)
    }

    @Test
    fun `testSkipForcesDefaultVariantWhenNotNull - variant key set`() {
        provider.variantKey = "ma"
        assertFalse(feature.experimentDisabledByDefault().isEnabled())
        assertEquals("ma", provider.variantKey)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun `methodWithArguments - throws exception`() {
        feature.methodWithArguments("")
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun `suspendFun - suspend function then throw`() = runTest {
        feature.suspendFun()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `whenValidFeatureAndMissingFeatureNameBuilderParameterThenThrow - build - throws`() {
        FeatureToggles.Builder()
            .store(FakeToggleStore())
            .appVersionProvider { provider.version }
            .build()
            .create(TestFeature::class.java)
            .self()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `whenValidFeatureAndMissingStoreBuilderParameterThenThrow - build - throws`() {
        FeatureToggles.Builder()
            .featureName("test")
            .appVersionProvider { provider.version }
            .build()
            .create(TestFeature::class.java)
            .self()
    }

    @Test
    fun `setEnabled - enabled and rollout threshold - is enable returns true`() {
        val state = Toggle.State(
            enable = true,
            rollout = null,
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())

        feature.self().setEnabled(state.copy(rollout = emptyList()))
        assertTrue(feature.self().isEnabled())

        feature.self().setEnabled(state.copy(rolloutThreshold = 20.0))
        assertTrue(feature.self().isEnabled())

        feature.self().setEnabled(state.copy(rollout = listOf(1.0, 2.0)))
        assertEquals(feature.self().rolloutThreshold() < 2.0, feature.self().isEnabled())

        feature.self().setEnabled(state.copy(rollout = listOf(0.5, 2.0), rolloutThreshold = 0.0))
        assertTrue(feature.self().isEnabled())

        feature.self().setEnabled(state.copy(rollout = listOf(0.5, 100.0), rolloutThreshold = 10.0))
        assertTrue(feature.self().isEnabled())
    }

    @Test
    fun `setEnabled - valid rollout - return keep rollout step`() {
        val state = Toggle.State(
            enable = true,
            rollout = listOf(100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())
        val expected = state.copy(remoteEnableState = state.enable, rolloutThreshold = feature.self().getRawStoredState()?.rolloutThreshold)
        assertEquals(expected, toggleStore.get("test"))
    }

    @Test
    fun `whenDisabledAndValidRolloutThenDetermineRolloutValue - state updated`() {
        val state = Toggle.State(
            enable = false,
            rollout = listOf(100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())

        val updatedState = toggleStore.get("test")
        assertTrue(updatedState!!.enable)
    }

    @Test
    fun `whenDisabledAndValidRolloutWithMultipleStepsThenDetermineRolloutValue - state - rollout value determined`() {
        val state = Toggle.State(
            enable = false,
            rollout = listOf(1.0, 10.0, 20.0, 40.0, 100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())

        val updatedState = toggleStore.get("test")
        assertTrue(updatedState!!.enable)
    }

    @Test
    fun `setRolloutThreshold - only once`() {
        var state = Toggle.State(
            enable = false,
            rollout = listOf(0.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        val threshold = feature.self().rolloutThreshold()
        var loop = 1.0
        do {
            loop *= 2
            assertTrue(feature.self().rolloutThreshold() > state.rollout())
            assertEquals(threshold, feature.self().rolloutThreshold(), 0.00001)
            state = state.copy(
                rolloutThreshold = feature.self().rolloutThreshold(),
                rollout = state.rollout!!.toMutableList().apply { add(loop.coerceAtMost(100.0)) },
            )
            feature.self().setEnabled(state)
        } while (feature.self().rolloutThreshold() > state.rollout())

        val updatedState = toggleStore.get("test")
        assertTrue(updatedState!!.enable)
    }

    @Test
    fun `selfEnabled - previous steps and valid rollout with multiple steps - enabled`() {
        val state = Toggle.State(
            enable = false,
            rollout = listOf(1.0, 10.0, 20.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0),
            rolloutThreshold = 2.0,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())

        val updatedState = toggleStore.get("test")
        assertTrue(updatedState!!.enable)
    }

    @Test
    fun `whenDisabledWithValidRolloutStepsAndNotSupportedVersionThenReturnDisabled - state - disabled`() {
        provider.version = 10
        val state = Toggle.State(
            enable = false,
            rollout = listOf(1.0, 10.0, 20.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0),
            rolloutThreshold = 2.0,
            minSupportedVersion = 11,
        )
        feature.self().setEnabled(state)

        // the feature flag is internally enabled but isEnabled() returns disabled because it doesn't meet  minSupportedVersion
        assertFalse(feature.self().isEnabled())
        val updatedState = toggleStore.get("test")!!
        assertEquals(true, updatedState.enable)
        assertNotEquals(2, updatedState.rolloutThreshold)
        assertEquals(state.minSupportedVersion, updatedState.minSupportedVersion)
        assertEquals(state.rollout, updatedState.rollout)
    }

    @Test
    fun `self - remote enable state null - honour local and update`() {
        val state = Toggle.State(enable = false)
        feature.self().setEnabled(state)

        assertFalse(feature.self().isEnabled())
        val updatedState = toggleStore.get("test")!!
        assertEquals(false, updatedState.enable)
        assertEquals(false, updatedState.remoteEnableState)
        assertNotNull(updatedState.rolloutThreshold)
        assertNull(updatedState.rollout)
    }

    @Test
    fun `whenRemoteStateDisabledThenIgnoreLocalState - ignore local state`() {
        val state = Toggle.State(
            remoteEnableState = false,
            enable = true,
        )
        feature.self().setEnabled(state)
        assertFalse(feature.self().isEnabled())
        assertEquals(state, toggleStore.get("test"))
    }

    @Test
    fun `whenRemoteStateDisabledAndValidRollout - ignore rollout`() {
        val state = Toggle.State(
            remoteEnableState = false,
            enable = true,
            rollout = listOf(100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertFalse(feature.self().isEnabled())
        assertEquals(state, toggleStore.get("test"))
    }

    @Test
    fun `whenRemoteStateEnabledAndLocalStateEnabledWithValidRollout - ignore rollout`() {
        val state = Toggle.State(
            remoteEnableState = true,
            enable = true,
            rollout = listOf(100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())
        val expected = state.copy(rolloutThreshold = feature.self().getRawStoredState()?.rolloutThreshold)
        assertEquals(expected, toggleStore.get("test"))
    }

    @Test
    fun `whenRemoteStateEnabledAndLocalStateDisabledWithValidRollout - do rollout`() {
        val state = Toggle.State(
            remoteEnableState = true,
            enable = false,
            rollout = listOf(100.0),
            rolloutThreshold = null,
        )
        feature.self().setEnabled(state)
        assertTrue(feature.self().isEnabled())
        val expected = state.copy(enable = true, rollout = listOf(100.0), rolloutThreshold = toggleStore.get("test")?.rolloutThreshold)
        assertEquals(expected, toggleStore.get("test"))
    }

    @Test
    fun `whenAppUpdateThenEvaluatePreviousState - evaluate previous state`() {
        // when remoteEnableState is null it means app update
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_disableByDefault", state)
        assertTrue(feature.disableByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_enabledByDefault", state.copy(enable = false))
        assertFalse(feature.enabledByDefault().isEnabled())
    }

    @Test
    fun `whenNoMatchingVariantThenFeatureIsDisabled -  feature disabled`() {
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
            targets = listOf(Toggle.State.Target("ma")),
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentDisabledByDefault", state)
        assertFalse(feature.experimentDisabledByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentEnabledByDefault", state.copy(enable = false))
        assertFalse(feature.experimentEnabledByDefault().isEnabled())
    }

    @Test
    fun `whenMatchingVariantThenReturnFeatureState - return feature state`() {
        provider.variantKey = "ma"
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
            targets = listOf(Toggle.State.Target(provider.variantKey!!)),
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentDisabledByDefault", state)
        assertTrue(feature.experimentDisabledByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentEnabledByDefault", state.copy(enable = false))
        assertFalse(feature.experimentEnabledByDefault().isEnabled())
    }

    @Test
    fun `setFeatureFlags - variants ignored - disabled by default`() {
        provider.variantKey = "ma"
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
            targets = listOf(Toggle.State.Target("zz")),
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_disableByDefault", state)
        assertTrue(feature.disableByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentDisabledByDefault", state)
        assertFalse(feature.experimentDisabledByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_enabledByDefault", state.copy(enable = false))
        assertFalse(feature.enabledByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentEnabledByDefault", state.copy(enable = false))
        assertFalse(feature.experimentEnabledByDefault().isEnabled())
    }

    @Test
    fun `whenMultipleNotMatchingVariantThenReturnFeatureState - return feature state`() {
        provider.variantKey = "zz"
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
            targets = listOf(
                Toggle.State.Target("ma"),
                Toggle.State.Target("mb"),
            ),
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentDisabledByDefault", state)
        assertFalse(feature.experimentDisabledByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_experimentEnabledByDefault", state.copy(enable = false))
        assertFalse(feature.experimentEnabledByDefault().isEnabled())
    }

    @Test
    fun `whenAnyMatchingVariantThenReturnFeatureState - return feature state`() {
        provider.variantKey = "zz"
        val state = Toggle.State(
            remoteEnableState = null,
            enable = true,
            targets = listOf(
                Toggle.State.Target("ma"),
                Toggle.State.Target("zz"),
            ),
        )

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_disableByDefault", state)
        assertTrue(feature.disableByDefault().isEnabled())

        // Use directly the store because setEnabled() populates the local state when the remote state is null
        toggleStore.set("test_enabledByDefault", state.copy(enable = false))
        assertFalse(feature.enabledByDefault().isEnabled())
    }
}

interface TestFeature {
    @Toggle.DefaultValue(true)
    fun self(): Toggle

    @Toggle.DefaultValue(false)
    fun disableByDefault(): Toggle

    @Toggle.DefaultValue(true)
    fun enabledByDefault(): Toggle
    fun noDefaultValue(): Toggle

    @Toggle.DefaultValue(true)
    fun wrongReturnValue(): Boolean

    @Toggle.DefaultValue(true)
    fun methodWithArguments(arg: String)

    @Toggle.DefaultValue(true)
    suspend fun suspendFun(): Toggle

    @Toggle.DefaultValue(false)
    @Toggle.InternalAlwaysEnabled
    fun internal(): Toggle

    @Toggle.DefaultValue(false)
    @Toggle.Experiment
    fun experimentDisabledByDefault(): Toggle

    @Toggle.DefaultValue(true)
    @Toggle.Experiment
    fun experimentEnabledByDefault(): Toggle
}

private fun Toggle.rolloutThreshold(): Double {
    return getRawStoredState()?.rolloutThreshold!!
}

private fun Toggle.State.rollout(): Double {
    return this.rollout?.last()!!
}

private class FakeProvider {
    var version = Int.MAX_VALUE
    var flavorName = ""
    var variantKey: String? = null
}
