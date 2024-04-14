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
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import java.io.File

class OllintaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply {
            apply("com.android.library")
        }

        val serviceProvider: Provider<OllamaBuildService> =
            project.gradle
                .sharedServices
                .registerIfAbsent("ollama", OllamaBuildService::class.java) { spec ->
                    spec.parameters.modelName.set("orca-mini")
                }

        val dependencies = mutableListOf<TaskProvider<*>>()

        val backupLintXml = project.tasks.register("ollintaBackupLintXml", Copy::class.java) { copy ->
            copy.from("lint.xml")
            copy.into("lint-backup.xml")
        }.also {
            dependencies.add(it)
        }

        val overwriteLintXmlForGeneratePrompts = project.tasks.register("ollintaOverwriteLintXmlForGeneratePrompts", Copy::class.java) { copy ->
            copy.mustRunAfter(backupLintXml)
            copy.from("ollinta-generate-prompts.xml")
            copy.into("lint.xml")
        }.also {
            dependencies.add(it)
        }

        val lintToGeneratePrompts = project.tasks.register("ollintaLintToGeneratePrompts") { task ->
            task.mustRunAfter(overwriteLintXmlForGeneratePrompts)
            task.dependsOn("lint")
        }.also {
            dependencies.add(it)
        }

        val generateResponses = project.tasks.register("ollintaGenerateResponses", Ollinta::class.java) { task ->
            task.mustRunAfter(lintToGeneratePrompts)
            task.ollamaBuildService.set(serviceProvider)
            task.inputDir.set(File(project.buildDir, "ollinta/promptData"))
            task.outputDir.set(File(project.buildDir, "ollinta/responseData"))
        }.also {
            dependencies.add(it)
        }

        val overwriteLintXmlForFix = project.tasks.register("ollintaOverwriteLintXmlForFix", Copy::class.java) { copy ->
            copy.mustRunAfter(generateResponses)
            copy.from("ollinta-apply-fixes.xml")
            copy.into("lint.xml")
        }.also {
            dependencies.add(it)
        }

        val restoreLintXml = project.tasks.register("ollintaRestoreLintXml", Copy::class.java) { copy ->
            copy.from("lint-backup.xml")
            copy.into("lint.xml")
        } // don't add this to dependencies, it's a finalizedBy ;-)

        project.tasks.register("ollintaFix") { task ->
            task.mustRunAfter(overwriteLintXmlForFix)
            task.dependsOn(*dependencies.toTypedArray(), "lintFix")
            task.finalizedBy(restoreLintXml)
        }

        project.tasks.named("lintFix").configure { task ->
            task.mustRunAfter(generateResponses)
        }
    }
}
