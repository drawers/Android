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
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.WARNING
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
import java.io.File
import java.util.EnumSet

@Suppress("UnstableApiUsage")
class FixingFunctionNameDetector : TestFunctionNameDetector() {

    override fun performAction(
        context: JavaContext,
        element: UElement,
        method: KotlinUMethod,
        functionName: String,
        location: Location,
        error: Error
    ) {
        context.client.log(WARNING, null, "Running ${this::class.simpleName}")

        val sanitizedFileName = getSanitizedFileName(element, location)
        val responseFile = File(context.responseData().path + "/$sanitizedFileName")
        if (!responseFile.exists()) {
            return
        }

        val response = responseFile.readText()
        val firstBackTick = response.indexOfFirst { it == '`' }
        val secondBackTick = response.indexOfLast { it == '`' }
        if (firstBackTick == -1 || secondBackTick == -1) return

        val proposedFunctionName = response.substring(firstBackTick..secondBackTick)

        if (proposedFunctionName.isEmpty()) {
            return
        }

        context.report(
            TEST_FUNCTION_NAME,
            location,
            "Test name does not follow convention",
            LintFix.create()
                .name("Use AI-suggested name")
                .replace()
                .all()
                .with(proposedFunctionName)
                .autoFix()
                .build(),
        )
    }

    private fun Context.buildDir(): File {
        return project.buildModule.buildFolder
    }

    private fun Context.responseData(): File {
        return File(buildDir().path + "/${Folders.RESPONSE_DATA}")
    }

    companion object {

        val TEST_FUNCTION_NAME = issue(
            id = "FixingTestFunctionName",
            briefDescription = "Prompt writing test function name",
            "An issue to represent auto-fixing a broken function name based a response from a language model",
            implementation = Implementation(
                FixingFunctionNameDetector::class.java,
                EnumSet.of(Scope.ALL_JAVA_FILES),
            ),
        )
    }
}
