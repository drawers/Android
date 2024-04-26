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
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getIoFile
import org.jetbrains.uast.kotlin.KotlinUMethod
import kotlin.io.path.Path

@Suppress("UnstableApiUsage")
abstract class TestFunctionNameDetector : Detector(), SourceCodeScanner {

    final override fun isApplicableAnnotationUsage(type: AnnotationUsageType) = true

    final override fun applicableAnnotations() = listOf("org.junit.Test")

    final override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo,
    ) {
        if (context.isAndroidTest()) return

        val method = element.uastParent as? KotlinUMethod ?: return

        // make sure to retain backticks
        val functionName = (method.sourcePsi as? KtNamedDeclaration)?.nameIdentifier?.text ?: return

        // look for errors
        val error = functionName.backticksErrorOrNull() ?: functionName.partsErrorOrNull() ?: functionName.capitalizationErrorOrNull() ?: return

        val location = context.getNameLocation(method)

        performAction(
            context,
            element,
            method,
            functionName,
            location,
            error,
        )
    }

    abstract fun performAction(
        context: JavaContext,
        element: UElement,
        method: KotlinUMethod,
        functionName: String,
        location: Location,
        error: Error,
    )

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

    enum class Error(val message: String) {
        BACKTICKS("Test name should be in backticks."),
        PARTS("Test name should have two or three parts separated by a spaced hyphen in the form `functionUnderTest - state - expected outcome`"),
        CAPITALIZATION("Test name parts should not be capitalized")
    }

    private fun JavaContext.isAndroidTest() = Path("androidTest") in file.toPath()

    protected fun getSanitizedFileName(
        element: UElement,
        location: Location
    ): String {
        return element.containingFileName.replace(".", "_").plus('_').plus(location.start!!.line)
    }

    protected val UElement.containingFileName
        get() = getContainingUFile()?.getIoFile()?.name!!

    companion object {
        fun issue(
            id: String,
            briefDescription: String,
            explanation: String,
            implementation: Implementation,
        ): Issue = Issue.create(
            id = id,
            briefDescription = briefDescription,
            category = Category.TESTING,
            explanation = explanation,
            severity = Severity.WARNING,
            implementation = implementation,
        )
    }

    object Folders {
        const val OLLINTA = "ollinta"
        const val PROMPT_DATA = "$OLLINTA/promptData"
        const val RESPONSE_DATA = "$OLLINTA/responseData"
    }
}
