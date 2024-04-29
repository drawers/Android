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
                // checkOnly.clear()
                checkOnly.add("PromptWritingTestFunctionName")
                // checkOnly.add("TestFunctionName")
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
            
            `functionUnderTest - state - expected outcome`
            
            Note that to meet the standard, the test names must have the following:
            * They must be in backticks (``)
            * They must have a minimum of two parts separated by a spaced hyphen " - "
            * The "state" is optional - it can be omitted
            * The parts must start with lowercase if possible
            
            I am going to give you the current name of the function and the body of the function with ### (3 hashes) as a separator.   
            You must propose a new name for the function that meets the convention. 
            Your answer MUST only contain the new proposed function name.
            
            Please omit any filler in your answers like "Certainly!"
             
            Here is a sample input and output to help you.             
      
            Example input:
            
            whenTopLevelFeatureDisabledAndDisabledByUserThenCannotAccessAnySubFeatures
            ###
            fun whenTopLevelFeatureDisabledAndDisabledByUserThenCannotAccessAnySubFeatures() = runTest {
                    setupConfig(topLevelFeatureEnabled = false, autofillEnabledByUser = false)
                    assertAllSubFeaturesDisabled()
                }
            ###
            
            Expected output:
            `setUpConfig - top level feature disabled and disabled by user - cannot access sub features`
            
            Why are we choosing this expected output?
            * The name of the function under test is `setUpConfig` so it is the first part
            * "top level feature disabled and disabled by user" describes the state
            * the expected outcome is "all sub features disabled"
            
            Example input:
            
            whenDeleteAllPasswordsConfirmedWithPasswordsSavedThenDoesIssueCommandToShowUndoSnackbar
            ###
            @Test
                fun whenDeleteAllPasswordsConfirmedWithPasswordsSavedThenDoesIssueCommandToShowUndoSnackbar() = runTest {
                    testee.onDeleteAllPasswordsConfirmed()
                    testee.commandsListView.test {
                        awaitItem().verifyHasCommandToAuthenticateMassDeletion()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            ###

            Expected output:
            `onDeleteAllPasswordsConfirmed - issues command to show undo snackbar`
            
            Example input:
            
            whenGetCredentialsWithLocalIdNotFoundThenReturnNull
            ###
            @Test
                fun whenGetCredentialsWithLocalIdNotFoundThenReturnNull() = runTest {
                    givenLocalCredentials(
                        twitterCredentials,
                        spotifyCredentials,
                    )

                    val credential = credentialsSync.getCredentialWithId(1234)

                    assertNull(credential)
                }
            ###
            
            Expected output:
            `getCredentialWithId - local Id not found - null`
            
            I have given you three examples to follow.
            
            From now on, in this conversation I am going to give you a real incorrect function name and the function body separated by a ###. 
            
            Please answer with the expected output. Please DO NOT embellish the answer with extra information. Please DO NOT add three backticks
            to make a code block. Please ONLY answer with the proposed name of the function.
           """.trimIndent()
}
