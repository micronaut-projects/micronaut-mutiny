import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mutiny-module")
}
dependencies {
    api(mn.micronaut.core.reactive)
    api(libs.managed.mutiny)
    implementation(libs.managed.mutiny.zero.flow.adapters)
}
micronautBuild {
    binaryCompatibility.enabledAfter("1.0.0")
    testFramework = TestFramework.JUNIT6
}
