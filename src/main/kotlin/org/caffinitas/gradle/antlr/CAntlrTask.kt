/*
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
package org.caffinitas.gradle.antlr

import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.antlr.AntlrTask
import org.gradle.api.plugins.antlr.internal.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.InputChanges
import java.io.File
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * Workaround to deal with antlr include files.
 * See https://github.com/gradle/gradle/issues/13005
 */
open class CAntlrTask @Inject constructor(@Internal val antlrTask: TaskProvider<AntlrTask>) : AntlrTask() {
    @get:Input
    var includeFiles: Set<String> = LinkedHashSet()

    init {
        source = antlrTask.get().source
    }

    @Suppress("UnstableApiUsage")
    @Incubating
    @TaskAction
    override fun execute(inputChanges: InputChanges) {
        val stableSources = stableSources
        // Note: cannot really handle incremental grammar generation (but doesn't actually matter, as there's
        // just one Cql.g file in C* anyway).
        val grammarFiles: Set<File> = HashSet(stableSources.files)
        val processGrammarFiles = grammarFiles.stream()
                .filter { f: File -> !includeFiles.contains(f.name) }
                .collect(Collectors.toSet())
        val manager = AntlrWorkerManager()
        val spec = antlrSpec(processGrammarFiles)
        val projectDir = projectLayout.projectDirectory.asFile
        val result = manager.runWorker(projectDir, workerProcessBuilderFactory, antlrClasspath, spec)
        evaluate(result)
    }

    private fun antlrSpec(processGrammarFiles: Set<File>?): AntlrSpec {
        try {
            return try {
                val sourceDirectorySet = antlrTaskField("sourceDirectorySet") as SourceDirectorySet

                // class AntlrSpecFactory:
                //     public AntlrSpec create(AntlrTask antlrTask, Set<File> grammarFiles, SourceDirectorySet sourceDirectorySet) {
                AntlrSpecFactory::class.java.getDeclaredMethod("create", AntlrTask::class.java, Set::class.java, SourceDirectorySet::class.java)
                        .invoke(AntlrSpecFactory(), this, processGrammarFiles, sourceDirectorySet) as AntlrSpec
            } catch (e: NoSuchFieldException) {
                // Since Gradle 6.6
                val sourceSetDirectories = antlrTaskField("sourceSetDirectories") as FileCollection

                // class AntlrSpecFactory:
                //     public AntlrSpec create(AntlrTask antlrTask, Set<File> grammarFiles, FileCollection sourceSetDirectories) {
                AntlrSpecFactory::class.java.getDeclaredMethod("create", AntlrTask::class.java, Set::class.java, FileCollection::class.java)
                        .invoke(AntlrSpecFactory(), this, processGrammarFiles, sourceSetDirectories) as AntlrSpec
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun antlrTaskField(field: String): Any {
        val f = AntlrTask::class.java.getDeclaredField(field)
        f.isAccessible = true
        return f[antlrTask.get()]
    }

    private fun evaluate(result: AntlrResult) {
        val errorCount = result.errorCount
        when {
            errorCount < 0 -> {
                throw AntlrSourceGenerationException("There were errors during grammar generation", result.exception)
            }
            errorCount == 1 -> {
                throw AntlrSourceGenerationException("There was 1 error during grammar generation", result.exception)
            }
            errorCount > 1 -> {
                throw AntlrSourceGenerationException("There were "
                        + errorCount
                        + " errors during grammar generation", result.exception)
            }
        }
    }
}
