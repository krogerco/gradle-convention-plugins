/**
 * MIT License
 *
 * Copyright (c) 2024 The Kroger Co. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kroger.gradle

import com.kroger.gradle.util.JDK_VERSION
import com.kroger.gradle.util.KOTLIN_VERSION
import com.kroger.gradle.util.RootTestProjectBuilder
import com.kroger.gradle.util.gradleRunner
import com.kroger.gradle.util.rootProject
import com.kroger.gradle.util.shouldContainAll
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinterPluginTest {
    @TempDir
    private lateinit var testProjectDir: File
    private lateinit var testProjectBuilder: RootTestProjectBuilder

    @BeforeEach
    fun init() {
        testProjectBuilder = rootProject(projectDir = testProjectDir) {
            versionCatalogSpec.versions.apply {
                put("kgpKotlin", "\"$KOTLIN_VERSION\"")
                put("kgpJdk", "\"$JDK_VERSION\"")
            }
            addPlugin("com.kroger.gradle.root")
        }
    }

    @Test
    fun `GIVEN kotlinter enabled WHEN no hook directory exists THEN message logged`() {
        testProjectBuilder.build()
        val output = gradleRunner(testProjectDir, ":createLintKotlinPreCommitHook")
            .build()
            .output

        output.shouldContain("No .husky or .git directory found. Cannot create git hooks.")
    }

    @Test
    fun `GIVEN kotlinter enabled WHEN husky directory exists THEN hook created there`() {
        testProjectBuilder.createProjectDir(".husky")
        testProjectBuilder.build()

        gradleRunner(testProjectDir, ":createLintKotlinPreCommitHook").build()

        val precommitHook = testProjectBuilder.projectPath.resolve(".husky/pre-commit")
        precommitHook.exists().shouldBeTrue()
        precommitHook.canExecute().shouldBeTrue()

        val precommitHookContents = precommitHook.readText()
        precommitHookContents.shouldContainAll(
            "husky.sh",
            "auto_format_files=false",
        )
    }

    @Test
    fun `GIVEN kotlinter enabled WHEN git directory exists and husky does not THEN hook created in git directory`() {
        testProjectBuilder.createProjectDir(".git")
        testProjectBuilder.build()

        gradleRunner(testProjectDir, ":createFormatKotlinPreCommitHook").build()

        val precommitHook = testProjectBuilder.projectPath.resolve(".git/hooks/pre-commit")
        precommitHook.exists().shouldBeTrue()
        precommitHook.canExecute().shouldBeTrue()

        val precommitHookContents = precommitHook.readText()
        precommitHookContents.shouldContainAll(
            "auto_format_files=true",
        )
    }
}
