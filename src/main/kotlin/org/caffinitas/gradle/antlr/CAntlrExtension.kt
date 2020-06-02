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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.antlr.AntlrTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

open class CAntlrExtension(private val project: Project) {
    fun registerCAntlr(antlrTask: String): TaskProvider<CAntlrTask> {
        return registerCAntlr(antlrTask) {}
    }

    fun registerCAntlr(antlrTask: String, configAction: Action<in CAntlrTask>): TaskProvider<CAntlrTask> {
        val task = project.tasks.named<AntlrTask>(antlrTask)
        return registerCAntlr(task, configAction)
    }

    fun registerCAntlr(antlrTask: TaskProvider<AntlrTask>): TaskProvider<CAntlrTask> {
        return registerCAntlr(antlrTask) {}
    }

    fun registerCAntlr(antlrTask: TaskProvider<AntlrTask>, configAction: Action<in CAntlrTask>): TaskProvider<CAntlrTask> {
        val newName = "${antlrTask.name}Cassandra"
        val cantlrTask = project.tasks.register(newName, CAntlrTask::class.java, antlrTask)
        cantlrTask.configure(configAction)
        return cantlrTask
    }
}
