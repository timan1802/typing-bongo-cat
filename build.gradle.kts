import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.timan1802.typingbongocat"
version = "1.1.6"

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
            // 상한을 두지 않아 이후 IDE 버전에도 설치 가능 (호환성은 JetBrains Plugin Verifier 로 확인)
            untilBuild = provider { null }
        }

        changeNotes = """
            <ul>
              <li>최신 IDE 버전(2026.3 등) 지원: 호환 빌드 상한 제거</li>
              <li>이미지 비율이 찌그러지던 문제 수정</li>
              <li>화면 상단·좌우 끝에서 이미지가 잘리던 문제 수정 (스크롤·줌 상태 포함)</li>
              <li>BAM 효과가 연쇄 발동하거나 위치가 튀던 문제 수정</li>
              <li>성능 개선: 타이핑·스크롤 시 에디터 전체 다시 그리기 제거, 플러그인 용량 약 80% 감소</li>
            </ul>
        """.trimIndent()
    }
}

