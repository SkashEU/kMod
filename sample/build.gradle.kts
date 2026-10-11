plugins {
    alias(libs.plugins.kotlinMultiplatform)

}

kotlin {
    mingwX64 {
        binaries {
            executable {
                baseName = "sample"
                entryPoint = "com.kmod.sample.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
        }
    }
}