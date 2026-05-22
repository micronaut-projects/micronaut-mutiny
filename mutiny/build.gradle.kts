import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mutiny-module")
}
micronautBuild {
    binaryCompatibility.enabledAfter("1.0.0")
    testFramework = TestFramework.JUNIT6
}
