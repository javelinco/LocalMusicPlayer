plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

val isOneDriveWorkspace = rootDir.toPath()
    .any { pathPart -> pathPart.toString().equals("OneDrive", ignoreCase = true) }

if (isOneDriveWorkspace) {
    val externalBuildRoot = file(
        "${System.getProperty("java.io.tmpdir")}/LocalMusicPlayer-build/${rootDir.name}",
    )
    layout.buildDirectory.set(externalBuildRoot.resolve("root"))
    subprojects {
        layout.buildDirectory.set(externalBuildRoot.resolve(name))
    }
}
