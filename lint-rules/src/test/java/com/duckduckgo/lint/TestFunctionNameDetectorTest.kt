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

import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import java.io.File

class TestFunctionNameDetectorTest {

    @Test
    fun `name has no backticks - reports error`() {
        lint()
            .sdkHome(File("/Users/davidrawson/Library/Android/sdk"))
            .issues(TestFunctionNameDetector.ISSUE)
            .files(
                JUNIT_STUB,
                kt(
                    """
                package com.example        

                import org.junit.Test
    
                @Test
                fun foo() {
                    println("hello")
                }
            """,
                ),
            )

            .run()
            .expectErrorCount(1)
            .expectContains("Test name does not follow convention")
    }

    @Test
    fun `name no parts - reports error`() {
        lint()
            .sdkHome(File("/Users/davidrawson/Library/Android/sdk"))
            .issues(TestFunctionNameDetector.ISSUE)
            .files(
                JUNIT_STUB,
                kt(
                    """
                package com.example        

                import org.junit.Test
    
                @Test
                fun `foo bar`() {
                    println("hello")
                }
            """,
                ),
            )

            .run()
            .expectErrorCount(1)
            .expectContains("Test name does not follow convention")
    }

    @Test
    fun `name not enough parts - reports error`() {
        lint()
            .sdkHome(File("/Users/davidrawson/Library/Android/sdk"))
            .issues(TestFunctionNameDetector.ISSUE)
            .files(
                JUNIT_STUB,
                kt(
                    """
                package com.example        

                import org.junit.Test
    
                @Test
                fun `foo`() {
                    println("hello")
                }
            """,
                ),
            )

            .run()
            .expectErrorCount(1)
            .expectContains("Test name does not follow convention")
    }

    @Test
    fun `name capitalization - reports error`() {
        lint()
            .sdkHome(File("/Users/davidrawson/Library/Android/sdk"))
            .issues(TestFunctionNameDetector.ISSUE)
            .files(
                JUNIT_STUB,
                kt(
                    """
                package com.example        

                import org.junit.Test
    
                @Test
                fun `Foo - Bar`() {
                    println("hello")
                }
            """,
                ),
            )

            .run()
            .expectErrorCount(1)
            .expectContains("Test name does not follow convention")
    }

    @Test
    fun `name parts - clean`() {
        lint()
            .sdkHome(File("/Users/davidrawson/Library/Android/sdk"))
            .issues(TestFunctionNameDetector.ISSUE)
            .files(
                JUNIT_STUB,
                kt(
                    """
                package com.example        

                import org.junit.Test
    
                @Test
                fun `println - prints hello`() {
                    println("hello")
                }
            """,
                ),
            )

            .run()
            .expectClean()
    }

    companion object {

        val JUNIT_STUB = kt(
            """
                package org.junit

                annotation class Test
            """,
        )
    }
}
