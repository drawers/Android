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

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.testcontainers.containers.GenericContainer
import org.testcontainers.ollama.OllamaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Locale
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED

abstract class OllamaBuildService : BuildService<OllamaBuildService.Params>, AutoCloseable {

    abstract class Params : BuildServiceParameters {
        /**
         * The model name to use (e.g., "orca-mini", "mistral", "mistral", "llama2", "codellama", "phi", "tinyllama"
         */
        abstract val modelName: Property<String>
    }

    private val modelName: String
        get() = parameters.modelName.get()

    private fun baseUrl(ollama: GenericContainer<*>): String {
        return String.format(Locale.ROOT, "http://%s:%d", ollama.host, ollama.firstMappedPort)
    }

    private val ollama: OllamaContainer by lazy(NONE) {
        OllamaContainer(
            DockerImageName.parse("langchain4j/ollama-$modelName:latest")
                .asCompatibleSubstituteFor("ollama/ollama"),
        ).apply {
            start()
        }
    }

    val model: ChatLanguageModel by lazy(SYNCHRONIZED) {
        OllamaChatModel.builder().baseUrl(baseUrl(ollama)).modelName(modelName).seed(0).temperature(0.0).build()
    }


    override fun close() {
        ollama.close()
    }
}
