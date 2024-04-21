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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Properties

class OllintaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply {
            apply("com.android.library")
        }

        val serviceProvider: Provider<OllamaBuildService> =
            project.gradle
                .sharedServices
                .registerIfAbsent("ollama", OllamaBuildService::class.java) { spec ->
                    spec.parameters.modelName.set("codellama")
                }

        val localProps = Properties()
        localProps.load(project.rootProject.file("local.properties").inputStream())

        val openAiServiceProvider: Provider<OpenAiBuildService> =
            project.gradle
                .sharedServices
                .registerIfAbsent("openai", OpenAiBuildService::class.java) {
                    spec -> spec.parameters.modelName.set("GPT_3_5_TURBO")
                    spec.parameters.apiKey.set(localProps.getProperty("openapi.key"))
                }

        project.tasks.register("ollinta", Ollinta::class.java) { task ->
            task.ollamaBuildService.set(serviceProvider)
            task.inputDir.set(File(project.buildDir, "ollinta/promptData"))
            task.outputDir.set(File(project.buildDir, "ollinta/responseData"))
        }

        project.tasks.register("lintAi", LintAi::class.java) { task ->
            task.openAiBuildService.set(openAiServiceProvider)
            task.inputDir.set(File(project.buildDir, "ollinta/promptData"))
            task.outputDir.set(File(project.buildDir, "ollinta/responseData"))
        }
    }
}
