import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.timan1802.typingbongocat"
version = "1.0.4"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "251.*"
        }

        changeNotes = """
            Delete resizeImage and modify the image itself to width 100
    """.trimIndent()
    }
}

