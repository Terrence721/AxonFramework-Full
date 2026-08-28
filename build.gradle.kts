// Removes every untracked .log file in the repo, found via git ls-files rather than a
// hand-maintained list of directories/filenames to skip - respects .gitignore automatically,
// and stays correct for any future .log file without this task needing changes.
//
// Not wired into anything else here, unlike the equivalent task in the saga-full sibling
// repo (dependsOn on every service's bootRun): this is a library, not an application, so
// there's no real "app startup" command to hook into - `./gradlew cleanLogs` is meant to be
// run directly, the same way `./gradlew build`/`./gradlew test` already are.
tasks.register("cleanLogs") {
    group = "verification"
    description = "Removes every untracked .log file in the repo."

    doLast {
        fun gitLsFiles(ignoredOnly: Boolean): List<String> {
            val args = mutableListOf("git", "ls-files", "--others", "--exclude-standard")
            if (ignoredOnly) args.add("--ignored")
            args.add("*.log")
            val process = ProcessBuilder(args)
                .directory(rootDir)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return output.lines().filter { it.isNotBlank() }
        }

        // --exclude-standard alone drops any .log file that already matches a .gitignore
        // pattern instead of finding it - pairing it with --ignored flips to the opposite,
        // ignored-only set. Neither call alone covers every .log file, so both are unioned.
        val logFiles = (gitLsFiles(false) + gitLsFiles(true)).distinct()

        if (logFiles.isEmpty()) {
            println("No log files found.")
            return@doLast
        }

        logFiles.forEach { relativePath ->
            val file = File(rootDir, relativePath)
            if (file.delete()) {
                println("Removed $relativePath")
            }
        }
    }
}
