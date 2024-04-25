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

package com.duckduckgo.lint

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
import java.io.File
import java.util.EnumSet

@Suppress("UnstableApiUsage")
class PromptWritingFunctionNameDetector : TestFunctionNameDetector() {

    override fun isApplicable(context: JavaContext): Boolean = (Scope.ALL_JAVA_FILES in context.scope)

    override fun performAction(
        context: JavaContext,
        element: UElement,
        method: KotlinUMethod,
        functionName: String,
        location: Location,
        error: Error
    ) {
        try {
            val outputDir = context.outputDir()
            outputDir.createDirectory()

            val sanitizedFileName = getSanitizedFileName(element, location)
            val writeFile = File(outputDir.path + "/$sanitizedFileName")

            writeFile.writer().use {
                it.appendLine(element.containingFileName)
                it.appendLine(location.start?.line!!.toString())
                it.appendLine(functionName)
                it.appendLine("###")
                it.appendLine(method.sourcePsi?.text!!)
                it.appendLine("###")
            }
        } catch (t: Throwable) {
            context.log(t, "Could not write prompt for $functionName")
        }
    }

    private fun Context.buildDir(): File {
        return project.buildModule.buildFolder
    }

    private fun Context.outputDir(): File {
        return File(buildDir().path + "/${Folders.PROMPT_DATA}")
    }

    companion object {

        val TEST_FUNCTION_NAME = issue(
            id = "PromptWritingTestFunctionName",
            briefDescription = "Prompt writing test function name",
            "An issue to represent writing prompts with information suitable for an AI agent to suggest a rename",
            implementation = Implementation(
                PromptWritingFunctionNameDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
            ),
        )
    }
}
