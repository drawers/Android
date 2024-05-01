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

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import java.io.File

@Suppress("UnstableApiUsage")
abstract class ProcessPromptsTask : DefaultTask() {

    @get:ServiceReference("openai")
    abstract val openAiBuildService: Property<OpenAiBuildService>

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:InputDirectory
    @PathSensitive(RELATIVE)
    val inputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    abstract val prompt: Property<String>

    override fun getDescription(): String = "Takes a set of files in promptData and a prompt and runs it through an LLM, writing the results to resultData"

    @TaskAction
    fun performAction() {
        inputDir.get().asFileTree.files.forEach {
            println("Processing prompt file ${it.name}")

            val input = it.readText()
            println(input)

            val model = openAiBuildService.get().model
            val prompt = SystemMessage.from(prompt.get())
            val response = model.generate(prompt, UserMessage.from(input))
            println(response.content().text())

            val outputFile = File(outputDir.asFile.get(), it.name)
            outputFile.writer().use { writer ->
                writer.appendLine(response.content().text())
            }
            println("Finished writing response file ${it.name}")

            Thread.sleep(50) // avoid rate limiting
        }
    }
}
