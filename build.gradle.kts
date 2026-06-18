plugins {
    id("java-library")
}

group = "net.minecraft"
version = "rd-161807"

repositories {
    mavenCentral()
}

val natives: Configuration by configurations.creating
natives.isTransitive = true

dependencies {
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl", version = "2.9.3")
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl_util", version = "2.9.3")
    natives(group = "org.lwjgl.lwjgl", name = "lwjgl-platform", version = "2.9.3", classifier = "natives-windows")
    natives(group = "org.lwjgl.lwjgl", name = "lwjgl-platform", version = "2.9.3", classifier = "natives-linux")
    natives(group = "org.lwjgl.lwjgl", name = "lwjgl-platform", version = "2.9.3", classifier = "natives-osx")
}


// Add manifest to the default jar
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.mojang.minecraft.Minecraft"
        attributes["Implementation-Version"] = project.version
    }
}

// Fat jar — bundles all dependencies (LWJGL etc.) into one runnable jar
task("fatJar", Jar::class) {
    group = "build"
    description = "Assembles a runnable jar with all dependencies included"
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.mojang.minecraft.Minecraft"
        attributes["Implementation-Version"] = project.version
    }
    from(sourceSets["main"].output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") && !it.name.contains("natives") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

task("run", JavaExec::class) {
    jvmArgs = listOf("-Dorg.lwjgl.librarypath=${project.projectDir.toPath()}\\run\\natives")
    main = "com.mojang.minecraft.Minecraft"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir("${project.projectDir.toPath()}\\run")
    dependsOn("extractNatives")
}

task("extractNatives", Copy::class) {
    dependsOn(natives)
    from(natives.map { zipTree(it) })
    into("${project.projectDir.toPath()}\\run\\natives")
}
