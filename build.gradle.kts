plugins {
    base
    id("com.gradleup.shadow") version "8.3.10" apply false
}

group = "com.tflives.tflonline"
version = "0.2.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.helpch.at/releases")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}

project(":common") {
    dependencies {
        "implementation"("org.yaml:snakeyaml:2.5")
    }
}

listOf("velocity", "bungee", "paper").forEach { module ->
    project(":$module") {
        apply(plugin = "com.gradleup.shadow")
        dependencies {
            "implementation"(project(":common"))
        }
        tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
            archiveBaseName.set("TFLOnline-" + module.replaceFirstChar { it.uppercase() })
            archiveClassifier.set("")
            relocate("org.yaml.snakeyaml", "com.tflives.tflonline.libs.yaml")
            mergeServiceFiles()
        }
        tasks.named<Jar>("jar") {
            enabled = false
        }
        tasks.named("assemble") {
            dependsOn("shadowJar")
        }
    }
}

project(":velocity") {
    dependencies {
        "compileOnly"("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
        "annotationProcessor"("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
        "compileOnly"("org.slf4j:slf4j-api:2.0.16")
    }
}
project(":bungee") {
    dependencies {
        "compileOnly"("net.md-5:bungeecord-api:1.21-R0.3")
    }
}
project(":paper") {
    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
        "compileOnly"("me.clip:placeholderapi:2.11.6")
    }
}
