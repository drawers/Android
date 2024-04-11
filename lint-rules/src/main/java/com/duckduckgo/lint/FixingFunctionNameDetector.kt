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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope.JAVA_FILE
import com.android.tools.lint.detector.api.Scope.TEST_SOURCES
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
import java.io.File
import java.util.EnumSet

@Suppress("UnstableApiUsage")
class FixingFunctionNameDetector : AbstractTestFunctionNameDetector() {

    override fun isApplicable(context: JavaContext): Boolean {
        return false // disable for now, we'll test this later
        // return (Scope.ALL_JAVA_FILES in context.scope)
    }

    override fun performAction(
        context: JavaContext,
        element: UElement,
        method: KotlinUMethod,
        functionName: String,
        location: Location
    ) {

        val sanitizedFileName = getSanitizedFileName(element, location)
        val responseFile = File(context.responseData().path + "/$sanitizedFileName")
        if (!responseFile.exists()) {
            return
        }

        val response = responseFile.readText()
        val firstBackTick = response.indexOfFirst { it == '`' }
        val secondBackTick = response.indexOfLast { it == '`' }
        val proposedFunctionName = "`${response.substring(firstBackTick..secondBackTick)}`"
        context.report(
            TODO(),
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
        return File(buildDir().path + "/ollama/responses")
    }

    companion object {

        @JvmField
        val TEST_FUNCTION_NAME: Issue = Issue.create(
            id = "FixingTestFunctionName",
            briefDescription = "Fixing test function name",
            category = Category.TESTING,
            priority = 5,
            severity = Severity.ERROR,
            explanation = "An issue for fixing a function name given a response from an LM",
            implementation = Implementation(
                FixingFunctionNameDetector::class.java,
                EnumSet.of(JAVA_FILE, TEST_SOURCES),
            ),
        )
    }
}
