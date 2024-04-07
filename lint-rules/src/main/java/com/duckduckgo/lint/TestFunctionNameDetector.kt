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

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.JAVA_FILE
import com.android.tools.lint.detector.api.Scope.TEST_SOURCES
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
import java.util.EnumSet
import kotlin.io.path.Path

@Suppress("UnstableApiUsage")
class TestFunctionNameDetector : Detector(), SourceCodeScanner {
    override fun isApplicableAnnotationUsage(type: AnnotationUsageType) = true

    override fun applicableAnnotations() = listOf("org.junit.Test")

    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo,
    ) {
        if (context.isAndroidTest()) return

        val method = element.uastParent as? KotlinUMethod ?: return
        // make sure to retain backticks
        val functionName = (method.sourcePsi as? KtNamedDeclaration)?.nameIdentifier?.text ?: return

        val error = functionName.backticksErrorOrNull() ?: functionName.partsErrorOrNull() ?: functionName.capitalizationErrorOrNull() ?: return

        context.report(
            ISSUE,
            context.getNameLocation(method),
            "Test name does not follow convention",
        )
    }

    private fun String.backticksErrorOrNull(): Error? {
        if (this.startsWith('`') && this.endsWith('`')) return null
        return Error.BACKTICKS
    }

    private fun String.partsErrorOrNull(): Error? {
        val splits = this.split(" - ")
        return if (splits.size < 2) Error.PARTS else null
    }

    private fun String.capitalizationErrorOrNull(): Error? {
        val trimmed = trim('`')
        val splits = trimmed.split(" - ")
        return if (splits.all {
                it.firstOrNull()?.isUpperCase() == true
            }) {
            Error.CAPITALIZATION
        } else {
            null
        }
    }

    enum class Error {
        BACKTICKS,
        PARTS,
        CAPITALIZATION
    }

    private fun JavaContext.isAndroidTest() = Path("androidTest") in file.toPath()

    companion object {

        @JvmField val ISSUE: Issue = Issue.create(
            id = "TestFunctionName",
            briefDescription = "Test function name",
            category = Category.TESTING,
            priority = 5,
            severity = Severity.ERROR,
            explanation = "The test function name should be enclosed in backticks. It should have either two or three parts, separated by hyphens. Each part should, where possible, start in lowercase",
            implementation = Implementation(
                TestFunctionNameDetector::class.java,
                EnumSet.of(JAVA_FILE, TEST_SOURCES),
            ),
        )
    }
}
