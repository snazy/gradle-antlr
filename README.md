Plugin to work around these issues in the built-in `AntlrTask` in Gradle.
The code works with Gradle 6.4.x and newer, it might work with earlier Gradle versions.

See these Gradle issues:
* [antlr plugin should respect include files](https://github.com/gradle/gradle/issues/13005)
* [antlr-plugin should optionally be restricted to an explicit set of `SourceSet`s](https://github.com/gradle/gradle/issues/13015)
* [AntlrTask doesn't respect non-default source-directories](https://github.com/gradle/gradle/issues/13016)

Usage:
```(kotlin)
plugins {
    id("org.caffinitas.gradle.antlr") version "0.1"
}
```

The plugin disables all existing `AntlrTask`s. You need to register new tasks of the type `CAntlrTask` like this:
```(kotlin)
import org.caffinitas.gradle.antlr.CAntlrTask

val generateGrammarSourceCassandra by tasks.registering(CAntlrTask::class) {
    // find the "original" task and copy the configuration from that one
    val antlrTask = tasks.getByName("generateGrammarSource", AntlrTask::class)
    configureFrom(antlrTask)

    // add custom configurations (same as for the original AntlrTask)

    // specify the include files
    includeFiles = setOf("Lexer.g", "Parser.g")
}
```

Note: you cannot reuse a task name created by Gradle's built-in antlr-plugin.
