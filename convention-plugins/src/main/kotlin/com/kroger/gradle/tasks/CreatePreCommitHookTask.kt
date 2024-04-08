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
package com.kroger.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Create a pre-commit hook to lint staged Kotlin files.
 * Optionally try to auto-format the files as well.
 */
public abstract class CreatePreCommitHookTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:Input
    internal val autoFormatFiles: Property<Boolean> = objectFactory.property(Boolean::class.java)
        .convention(false)

    @get:InputDirectory
    internal val rootDir: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(project.rootDir)
    }

    @TaskAction
    public fun installHook() {
        val hookDir = findHookDir()
        if (hookDir == null) {
            logger.warn("No .husky or .git directory found. Cannot create git hooks.")
            return
        }

        logger.info("Found hook directory: ${hookDir.path}")
        val preCommitHookFile = hookDir.resolve(PRE_COMMIT_HOOK_FILENAME)
        createPreCommitHookFile(preCommitHookFile)
        writeHook(preCommitHookFile)
    }

    private fun writeHook(hookFile: File) {
        val fileContents = hookFile.readText()
        val hookStartIndex = fileContents.indexOf(KOTLINTER_PRE_COMMIT_HEADER)
        if (hookStartIndex == -1) {
            hookFile.appendText(
                text =
                """
                |
                |$KOTLINTER_PRE_COMMIT_HEADER
                |${createGitHook(autoFormatFiles.get())}
                |$KOTLINTER_PRE_COMMIT_FOOTER
                |
                """.trimMargin(),
            )
        } else {
            val hookEndIndex = fileContents.indexOf(KOTLINTER_PRE_COMMIT_FOOTER)
            val newFileContents = fileContents.replaceRange(
                hookStartIndex,
                hookEndIndex,
                replacement =
                """
                |$KOTLINTER_PRE_COMMIT_HEADER
                |${createGitHook(autoFormatFiles.get())}
                |
                """.trimMargin(),
            )
            hookFile.writeText(newFileContents)
        }
    }

    private fun createGitHook(autoformatFiles: Boolean): String =
        """
        staged_files=$GIT_COMMAND_WITH_FILTER
        if [ -z "${'$'}staged_files" ]; then
            echo "No Kotlin files are staged."
            exit 0
        fi;

        echo "Running ktlint on the following staged files:"
        echo "${'$'}staged_files"
        
        auto_format_files=$autoformatFiles
        if [ "${'$'}auto_format_files" = true ] ; then
            echo "auto-formatting staged files..."
            ./gradlew formatKotlinFiles -Pfiles="${'$'}staged_files"
        fi

        if ! ./gradlew checkKotlinFiles -Pfiles="${'$'}staged_files"; then
            echo "pre-commit hook: Some files are either not properly formatted or could not be auto-formatted. Aborting commit."
            exit 1
        else
            # Re-index any files that may have been corrected before committing
            echo "${'$'}staged_files" | xargs git add
            exit_code=0
        fi
        """.trimIndent()

    private fun createPreCommitHookFile(hookFile: File) {
        if (hookFile.exists()) {
            return
        }

        logger.info("Creating pre-commit hook file at: ${hookFile.path}")
        try {
            if (hookFile.createNewFile() && hookFile.setExecutable(true)) {
                var fileContents = BASH_SHEBANG
                if (hookFile.path.contains(HUSKY_DIR)) {
                    fileContents += "\n$HUSKY_SCRIPT"
                }
                hookFile.writeText(fileContents)
            } else {
                logger.warn("Could not create hook file at: ${hookFile.path}")
            }
        } catch (e: Exception) {
            logger.warn("Could not create hook file at: ${hookFile.path}", e)
        }
    }

    private fun findHookDir(): File? =
        findHuskyHookDir() ?: findGitHookDir()

    private fun findHuskyHookDir(): File? {
        val huskyDir = File(rootDir.asFile.get(), HUSKY_DIR)
        return if (huskyDir.exists()) {
            logger.info("Found $HUSKY_DIR directory: ${huskyDir.path}")
            huskyDir
        } else {
            null
        }
    }

    private fun findGitHookDir(): File? {
        val gitDir = findGitDir(rootDir.asFile.get())

        return if (gitDir == null) {
            null
        } else {
            logger.info("Found $GIT_DIR directory: ${gitDir.path}")
            val hooksDir = gitDir.resolve(HOOKS_DIR)
            if (!hooksDir.exists()) {
                hooksDir.mkdir()
            }
            hooksDir
        }
    }

    private tailrec fun findGitDir(directory: File?): File? {
        val gitDir = directory?.resolve(GIT_DIR)
        return when {
            directory == null -> null
            gitDir?.exists() == true -> gitDir
            else -> findGitDir(directory.parentFile)
        }
    }
}

private const val BASH_SHEBANG = "#!/bin/bash"
private const val GIT_DIR = ".git"
private const val HOOKS_DIR = "hooks"
private const val HUSKY_DIR = ".husky"
private const val HUSKY_SCRIPT = """. "$(dirname "$0")/_/husky.sh""""
private const val KOTLINTER_PRE_COMMIT_HEADER = "### AUTOGENERATED KGP KOTLINTER HOOK START - DO NOT MODIFY ###"
private const val KOTLINTER_PRE_COMMIT_FOOTER = "### AUTOGENERATED KGP KOTLINTER HOOK END ###"
private const val PRE_COMMIT_HOOK_FILENAME = "pre-commit"

// --no-pager: do not pipe Git output into a pager.
// diff: show changes between commit and working tree
// --name-status: only print filename and status of change (A = added, D = deleted, M = modified, etc)
// --no-color: do not colorize printed output
// --staged: include staged changes only
// Example of what command output looks like:
// A       app/Application.kt
// M       app/build.gradle.kts
// D       app-feature/AppFeatureClass.kt
private const val GIT_COMMAND = "git --no-pager diff --name-status --no-color --staged"

// $1 != "D": $1 is the change type so exclude files where the status change type is D (Deleted)
// $NF ~ /\.kts|\.kt/: $NF is the last field on each line representing the filename. This excludes files that are not Kotlin related (do not end in .kts or .kt).
// $NF is used because the file is different depending on the change type. Created and deleted files are in the second field on each line but moved and renamed files are in the third field.
// { print $NF }: print the results with the filter applied to just list file paths/names
private const val AWK_FILTER_COMMAND = """awk '$1 != "D" && ${'$'}NF ~ /\.kts|\.kt/ { print ${'$'}NF }'"""

private const val GIT_COMMAND_WITH_FILTER = "\"$($GIT_COMMAND | $AWK_FILTER_COMMAND)\""
