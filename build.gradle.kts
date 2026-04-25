plugins {
    base
}

val androidHomeProvider = providers.environmentVariable("ANDROID_HOME")
    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))

val androidHome = androidHomeProvider.map { file(it) }
val compileSdk = "android-36"
val buildTools = androidHome.map { sdk ->
    sdk.resolve("build-tools").listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?: error("No Android build-tools found in ${sdk.resolve("build-tools")}")
}
val androidJar = androidHome.map { it.resolve("platforms/$compileSdk/android.jar") }
val aapt2 = buildTools.map { it.resolve("aapt2").absolutePath }
val d8 = buildTools.map { it.resolve("d8").absolutePath }
val zipalign = buildTools.map { it.resolve("zipalign").absolutePath }

val appDir = layout.projectDirectory.dir("app")
val generatedDir = layout.buildDirectory.dir("generated/aapt/debug")
val compiledResources = layout.buildDirectory.file("intermediates/resources/debug/resources.zip")
val linkedApk = layout.buildDirectory.file("intermediates/apk/debug/linked.apk")
val classesDir = layout.buildDirectory.dir("intermediates/javac/debug/classes")
val dexDir = layout.buildDirectory.dir("intermediates/dex/debug")
val apkDir = layout.buildDirectory.dir("outputs/apk/debug")
val unsignedApk = layout.buildDirectory.file("outputs/apk/debug/app-debug-unsigned.apk")
val debugApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")

tasks.register<Exec>("compileDebugResources") {
    group = "build"
    description = "Compiles Android resources for the debug APK."
    inputs.dir(appDir.dir("src/main/res"))
    outputs.file(compiledResources)

    doFirst {
        compiledResources.get().asFile.parentFile.mkdirs()
    }

    commandLine(
        aapt2.get(),
        "compile",
        "--dir",
        appDir.dir("src/main/res").asFile.absolutePath,
        "-o",
        compiledResources.get().asFile.absolutePath,
    )
}

tasks.register<Exec>("linkDebugResources") {
    group = "build"
    description = "Links Android resources and generates R.java."
    dependsOn("compileDebugResources")
    inputs.file(appDir.file("src/main/AndroidManifest.xml"))
    inputs.file(compiledResources)
    outputs.file(linkedApk)
    outputs.dir(generatedDir)

    doFirst {
        linkedApk.get().asFile.parentFile.mkdirs()
        generatedDir.get().asFile.mkdirs()
    }

    commandLine(
        aapt2.get(),
        "link",
        "-I",
        androidJar.get().absolutePath,
        "--manifest",
        appDir.file("src/main/AndroidManifest.xml").asFile.absolutePath,
        "-o",
        linkedApk.get().asFile.absolutePath,
        "--java",
        generatedDir.get().asFile.absolutePath,
        "--auto-add-overlay",
        compiledResources.get().asFile.absolutePath,
    )
}

tasks.register<JavaCompile>("compileDebugJava") {
    group = "build"
    description = "Compiles debug Java sources."
    dependsOn("linkDebugResources")
    source(
        fileTree(appDir.dir("src/main/java")) { include("**/*.java") },
        generatedDir,
    )
    classpath = files(androidJar, generatedDir)
    destinationDirectory.set(classesDir)
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
    options.release.set(17)
}

tasks.register<Exec>("dexDebug") {
    group = "build"
    description = "Converts debug classes to DEX bytecode."
    dependsOn("compileDebugJava")
    inputs.dir(classesDir)
    outputs.dir(dexDir)

    doFirst {
        dexDir.get().asFile.mkdirs()
        commandLine(
            listOf(
                d8.get(),
                "--lib",
                androidJar.get().absolutePath,
                "--output",
                dexDir.get().asFile.absolutePath,
            ) + fileTree(classesDir).matching { include("**/*.class") }.files.map { it.absolutePath },
        )
    }
}

tasks.register<Zip>("packageDebug") {
    group = "build"
    description = "Packages the unsigned debug APK."
    dependsOn("linkDebugResources", "dexDebug")
    archiveFileName.set("app-debug-unsigned.apk")
    destinationDirectory.set(apkDir)
    from(zipTree(linkedApk))
    from(dexDir) {
        include("classes.dex")
    }
}

tasks.register<Exec>("assembleDebug") {
    group = "build"
    description = "Builds a debug APK."
    dependsOn("packageDebug")
    inputs.file(unsignedApk)
    outputs.file(debugApk)

    doFirst {
        debugApk.get().asFile.parentFile.mkdirs()
    }

    commandLine(
        zipalign.get(),
        "-f",
        "4",
        unsignedApk.get().asFile.absolutePath,
        debugApk.get().asFile.absolutePath,
    )
}

tasks.named("assemble") {
    dependsOn("assembleDebug")
}
