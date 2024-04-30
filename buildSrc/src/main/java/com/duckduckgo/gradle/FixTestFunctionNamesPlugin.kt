/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import dev.langchain4j.model.openai.OpenAiChatModelName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Properties

/**
 * Configures a [ProcessPrompts] task for a project and wires it up so it runs after lint but before lintFix
 * together with the prompt for performing a migration.
 *
 */
@Suppress("UnstableApiUsage")
class FixTestFunctionNamesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val localProps = Properties()
        localProps.load(project.rootProject.file("local.properties").inputStream())

        val openAiServiceProvider: Provider<OpenAiBuildService> =
            project.gradle
                .sharedServices
                .registerIfAbsent("openai", OpenAiBuildService::class.java) { spec ->
                    spec.parameters.modelName.set(OpenAiChatModelName.GPT_3_5_TURBO.name)
                    spec.parameters.apiKey.set(localProps.getProperty("openapi.key"))
                }

        val createLintFixDirectory = project.tasks.register("createLintFixDirectory") { task ->
            task.doLast {
                val lintFixDir = File(project.buildDir, "lintFix")
                lintFixDir.mkdir()
                val lintFixPromptDataDir = File(lintFixDir, "promptData")
                lintFixPromptDataDir.mkdir()
            }
        }

        val prepareForLintFix = project.tasks.register("prepareForLintFix", ProcessPrompts::class.java) { task ->
            task.openAiBuildService.set(openAiServiceProvider)
            task.inputDir.set(File(project.buildDir, "lintFix/promptData"))
            task.outputDir.set(File(project.buildDir, "lintFix/responseData"))
            task.prompt.set(prompt)
            task.mustRunAfter(createLintFixDirectory)
            task.dependsOn(createLintFixDirectory)
        }

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.finalizeDsl { commonExtension ->
            commonExtension.lint {
                checkOnly.addAll(listOf("FixingTestFunctionName", "PromptWritingTestFunctionName"))
            }
        }

        androidComponents.onVariants { _: Variant ->
            prepareForLintFix.configure {
                it.mustRunAfter("lint")
            }
            project.tasks.named("lintFix").configure {
                it.dependsOn(prepareForLintFix)
            }
        }
    }

    private val prompt: String =
        """
            There is a Kotlin project with unit tests. The unit test functions currently have various non-standard names.
            
            We are performing a migration from non-standard names to a new standard.
            
            The new standard for the names is:
            
            `methodUnderTest - state - expected outcome`
            
            Here:
            * "methodUnderTest" means the method that the test intends to exercise being exercised. If we're thinking of "arrange/act/assert"
            then the method under test is normally exercised in the "act" part of the test body i.e., the middle.
            * "state" means the setup or situation for the test. Thinking of "arrange/act/assert" the state would normally be the first part.
            Note that not all tests have state. For instance, tests with no set up or tests of pure functions.
            * "expected outcome" means what we are hoping to measure in the test. Thinking of "arrange/act/assert" this would be the "assert" part 
            (the last part of the test)
            
            Note that to meet the standard, the test names must have the following:
            * They must be in backticks (``)
            * They must have a minimum of two parts separated by a spaced hyphen " - "
            * The "state" (second part) is optional - it can be omitted to allow for tests with only two parts
            * The parts must start with lowercase if possible
            
            I am going to give you a Kotlin function to consider. You must propose a new name for the function that meets the convention. 
            Your answer MUST only contain the new proposed function name.
            
            Please omit any filler in your answers like "Certainly!"
             
            Here is a sample input and output to help you.             
      
            Example input:
            
            @Test
            fun whenUserEnablesAutofillThenViewStateUpdatedToReflectChange() = runTest {
                testee.onEnableAutofill()
                testee.viewState.test {
                    assertTrue(this.awaitItem().autofillEnabled)
                    cancelAndIgnoreRemainingEvents()
                }
            }
            
            Expected output:
            `onEnableAutofill - viewState updated`
            
            Example input:
            
            @Test
            fun whenNotSignedIntoEmailProtectionThenReturnTypeIsNotSignedIn() = runTest {
                configureEmailProtectionNotSignedIn()
                val status = testee.getActivationStatus("foo@example.com")
                assertTrue(status is NotSignedIn)
            }
            
            Expected output:            
            `getActivationStatus - email protection not signed in - not signed in`

            Example input:
            
            @Test
            fun whenSaveCredentialsUnsuccessfulThenDoesNotDisableDeclineCountMonitoringFlag() = runTest {
                val bundle = bundle("example.com", someLoginCredentials())
                whenever(autofillStore.saveCredentials(any(), any())).thenReturn(null)
                testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
                verify(declineCounter, never()).disableDeclineCounter()
            }
            
            Expected output:
            `processResult - save credentials unsuccessful - does not disable decline count monitoring flag`

            I have given you three examples to follow.
            
            From now on, in this conversation I am going to give you the function body so you can devise a new name for it following the convention and examples. 
            
            Please answer with the expected output. Please DO NOT embellish the answer with extra information. Please DO NOT add three backticks
            to make a code block. Please ONLY answer with the proposed name of the function.
           """.trimIndent()
}
