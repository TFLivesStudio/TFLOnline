plugins {
    base
}

group = "com.tflives.tflonline"
version = "0.1.0-SNAPSHOT"

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
        withSourcesJar()
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

project(":velocity") {
    dependencies {
        "implementation"(project(":common"))
        "compileOnly"("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
        "annotationProcessor"("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
        "compileOnly"("org.slf4j:slf4j-api:2.0.16")
    }
    tasks.named<Jar>("jar") {
        archiveBaseName.set("TFLOnline-Velocity")
        from(project(":common").extensions.getByType<SourceSetContainer>()["main"].output)
    }
}
project(":bungee") {
    dependencies {
        "implementation"(project(":common"))
        "compileOnly"("net.md-5:bungeecord-api:1.21-R0.3")
    }
    tasks.named<Jar>("jar") {
        archiveBaseName.set("TFLOnline-Bungee")
        from(project(":common").extensions.getByType<SourceSetContainer>()["main"].output)
    }
}
project(":paper") {
    dependencies {
        "implementation"(project(":common"))
        "compileOnly"("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
        "compileOnly"("me.clip:placeholderapi:2.11.6")
    }
    tasks.named<Jar>("jar") {
        archiveBaseName.set("TFLOnline-Paper")
        from(project(":common").extensions.getByType<SourceSetContainer>()["main"].output)
    }
}
