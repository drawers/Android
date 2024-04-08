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

package com.duckduckgo.ollama

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import java.util.Locale

internal class OllamaChatModelTest {

    lateinit var ollama: GenericContainer<*>

    fun baseUrl(): String {
        return String.format(Locale.ROOT, "http://%s:%d", ollama.host, ollama.firstMappedPort)
    }


    @Before
    fun setUp() {
        ollama = GenericContainer("langchain4j/ollama-" + MODEL_NAME + ":latest")
            .withExposedPorts(11434)
        ollama.start()
    }

    @After
    fun tearDown() {
        ollama.stop()
    }

    @Test
    fun simple_example() {
        val model: ChatLanguageModel = OllamaChatModel.builder()
            .baseUrl(baseUrl())
            .modelName(MODEL_NAME)
            .build()

        val answer: String = model.generate("Provide 3 short bullet points explaining why Java is awesome")

        println(answer)
    }

    @Test fun json_output_example() {
        val model: ChatLanguageModel = OllamaChatModel.builder()
            .baseUrl(baseUrl())
            .modelName(MODEL_NAME)
            .format("json")
            .build()

        val json: String = model.generate("Give me a JSON with 2 fields: name and age of a John Doe, 42")

        println(json)
    }

    companion object {
        /**
         * The first time you run this test, it will download a Docker image with Ollama and a model.
         * It might take a few minutes.
         *
         *
         * This test uses modified Ollama Docker images, which already contain models inside them.
         * All images with pre-packaged models are available here: https://hub.docker.com/repositories/langchain4j
         *
         *
         * However, you are not restricted to these images.
         * You can run any model from https://ollama.ai/library by following these steps:
         * 1. Run "docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama"
         * 2. Run "docker exec -it ollama ollama run mistral" <- specify the desired model here
         */
        var MODEL_NAME: String = "orca-mini" // try "mistral", "llama2", "codellama", "phi" or "tinyllama"
    }
}
