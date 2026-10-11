plugins {
    alias(libs.plugins.kotlinMultiplatform)

}

kotlin {
    mingwX64 {
        compilations.all {
            cinterops {
                create("tlhelp32") {
                    definitionFile = file("src/nativeInterop/cinterop/tlhelp32.def")
                }
            }
        }
    }
}