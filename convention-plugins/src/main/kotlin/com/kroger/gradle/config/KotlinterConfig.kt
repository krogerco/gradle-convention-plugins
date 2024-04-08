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
package com.kroger.gradle.config

import com.kroger.gradle.tasks.CreatePreCommitHookTask
import org.gradle.api.Project
import org.jmailen.gradle.kotlinter.KotlinterPlugin
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

internal fun applyAndConfigureKotlinter(project: Project) {
    with(project) {
        pluginManager.apply(KotlinterPlugin::class.java)
        tasks.register("checkKotlinFiles", LintTask::class.java) {
            group = GROUP_FORMATTING
            description = "Runs lint on the specified kotlin source files."
            findOptionalStringProperty("files")?.let { files ->
                source(files.split("\n"))
            }
            reports.set(mapOf("json" to file("build/lint-report.json")))
        }

        tasks.register("formatKotlinFiles", FormatTask::class.java) {
            group = GROUP_FORMATTING
            description = "Formats the specified kotlin source files."
            findOptionalStringProperty("files")?.let { files ->
                source(files.split("\n"))
            }
            report.set(file("build/format-report.txt"))
        }

        tasks.register("createLintKotlinPreCommitHook", CreatePreCommitHookTask::class.java) {
            group = GROUP_FORMATTING
            description = "Registers a pre-commit hook to lint staged Kotlin files."
        }

        tasks.register("createFormatKotlinPreCommitHook", CreatePreCommitHookTask::class.java) {
            group = GROUP_FORMATTING
            description = "Registers a pre-commit hook to format staged Kotlin files."
            autoFormatFiles.set(true)
        }
    }
}

private const val GROUP_FORMATTING: String = "formatting"
