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

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Scope.JAVA_FILE
import com.android.tools.lint.detector.api.Scope.TEST_SOURCES
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
import java.util.EnumSet

@Suppress("UnstableApiUsage")
class IdeFunctionNameDetector : TestFunctionNameDetector() {

    override fun isApplicable(context: JavaContext): Boolean {
        return (Scope.ALL_JAVA_FILES !in context.scope)
    }

    override fun performAction(
        context: JavaContext,
        element: UElement,
        method: KotlinUMethod,
        functionName: String,
        location: Location,
        error: Error
    ) {
        context.report(
            TEST_FUNCTION_NAME,
            context.getNameLocation(method),
            error.message,
        )
    }

    companion object {

        val TEST_FUNCTION_NAME = issue(
            id = "TestFunctionName",
            briefDescription = "Test function name",
            "The test function name should be enclosed in backticks. It should have either two or three parts, separated by hyphens. Each part should, where possible, start in lowercase",
            implementation = Implementation(
                IdeFunctionNameDetector::class.java,
                EnumSet.of(JAVA_FILE, TEST_SOURCES),
                EnumSet.of(TEST_SOURCES),
                EnumSet.of(JAVA_FILE),
            ),
        )
    }
}
