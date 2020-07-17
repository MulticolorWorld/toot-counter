plugins {
    kotlin("jvm") version "1.3.72"
}

allprojects {
    group = "net.toot-counter"
    version = "1.0"
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8
}

project(":db") {

    val exposedVersion = "0.24.1"

    dependencies {
        api("org.jetbrains.exposed", "exposed-core", exposedVersion)
        api("org.jetbrains.exposed", "exposed-dao", exposedVersion)
        api("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
        api("org.jetbrains.exposed", "exposed-java-time", exposedVersion)

        api("mysql", "mysql-connector-java", "8.0.20")
        api("com.zaxxer", "HikariCP", "3.4.5")
        api("org.slf4j", "slf4j-simple", "1.7.30")
    }
}

project(":web") {

    val ktorVersion = "1.3.2"
    val mainClass = "net.toot_counter.web.MainKt"

    tasks {
        register("fatJar", Jar::class.java) {
            archiveClassifier.set("all")
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            manifest {
                attributes("Main-Class" to mainClass)
            }
            from(configurations.runtimeClasspath.get()
                    .onEach { println("add from dependencies: ${it.name}") }
                    .map { if (it.isDirectory) it else zipTree(it) })
            val sourcesMain = sourceSets.main.get()
            sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
            from(sourcesMain.output)
        }
    }

    dependencies {
        implementation(project(":db"))
        implementation("io.ktor", "ktor-server-netty", ktorVersion)
        implementation("io.ktor", "ktor-thymeleaf", ktorVersion)
        implementation("com.github.multicolorworld.mastodon4j", "mastodon4j", "master-SNAPSHOT")
    }
}

project(":task") {

    val mainClass = "net.toot_counter.task.MainKt"

    tasks {
        register("fatJar", Jar::class.java) {
            archiveClassifier.set("all")
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            manifest {
                attributes("Main-Class" to mainClass)
            }
            from(configurations.runtimeClasspath.get()
                    .onEach { println("add from dependencies: ${it.name}") }
                    .map { if (it.isDirectory) it else zipTree(it) })
            val sourcesMain = sourceSets.main.get()
            sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
            from(sourcesMain.output)
        }
    }

    dependencies {
        implementation(project(":db"))
        implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.7")
        implementation("com.github.multicolorworld.mastodon4j", "mastodon4j", "master-SNAPSHOT")
    }
}