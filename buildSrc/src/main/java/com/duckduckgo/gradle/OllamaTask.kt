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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

@Suppress("UnstableApiUsage")
abstract class OllamaTask : DefaultTask() {

    @get:ServiceReference("ollama")
    abstract val ollamaBuildService: Property<OllamaBuildService>

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:InputDirectory
    @PathSensitive(RELATIVE)
    val inputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun performAction() {
        val model = ollamaBuildService.get().model
        println("inputDir: " + inputDir.files())
        inputDir.files().forEach {
            val input = it.readText()
            val response = model.generate(input)
            println(response)
            val outputFile = File(outputDir.asFile.get(), it.name)
            outputFile.writer().use { writer ->
                writer.appendLine(response)
            }
        }
    }
}
