plugins {
    id("io.micronaut.build.internal.mutiny-base")
    id("io.micronaut.build.internal.bom")
}
micronautBuild {
    binaryCompatibility.enabledAfter("1.0.0")
}
