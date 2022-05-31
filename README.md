# ShapeKt: A Kotlin Compiler Plugin for Ahead-Of-Time Tensor Shape Verification and Inspection

## What is ShapeKt?
ShapeKt is an extensible Kotlin compiler plugin for ahead-of-time tensor (multi-dimensional) arrays shape verification and inspection. Commonly used in machine learning, tensors are often fed through many different operations; each operation often has different shape requirements and produces a new tensor with a possibly different shape. ShapeKt provides a system to describe and enforce shape requirements and output shapes. 

With the ShapeKt IntelliJ IDE plugin, users can inspect tensor shapes and see tensor shape errors while in active development.

ShapeKt is currently experimental. There is an early integration with DiffKt, a differentiable programming framework in Kotlin.

## Getting Started

### Gradle

Apply the ShapeKt plugin in `build.gradle.kts`:
```
plugins {
    id("shapekt") version <shapekt-version>
    // ... other plugins
}

// ... the rest of the build file
```

Add the annotations and shape-functions projects as dependencies in `build.gradle.kts`:
```
dependencies {
    implementation("shapekt:annotations:<shapekt-version>")
    
    // Enables default shape functions such as matmul and broadcast.
    implementation("shapekt:shape-functions:<shapekt-version>")
} 
```



### Using Custom Extensions
You will need a separate gradle subproject for your extensions that is a dependency of your main project.

For example, given two gradle subprojects, :main and :extensions, you would have the following build files:

main/build.gradle.kts
```
plugins {
    id("shapekt") version "<shapekt-version>"
    // ... other plugins
}

dependencies {
    implementation(project(":extensions"))
    // other dependencies
}
```

extensions/build.gradle.kts
```
plugins {
    kotlin("jvm")
    // tensor typing plugin for writing extensions
    id("shapekt.extensions") version "<shapekt-version>"

}

shapektExtensions {
    name = "MyExtensions"
    vendor = "MyVendor"
    // Other configs
}

```

### IntelliJ Plugin

TODO: Instructions on downloading the IntelliJ

### Building From Source

You may also build the ShapeKt plugin from source.

Clone the repository and go to the `plugin` directory.
```
git clone https://github.com/facebookresearch/shapekt.git

cd shapekt/plugin
```

Publish to mavenLocal.
```
./gradlew publishToMavenLocal
```

Make sure MavenLocal is listed as one of the gradle plugin repositories in `settings.gradle.kts`:
```
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}
// ... the rest of the settings file
```

And add those repositories to `build.gradle.kts`:
```
repositories {
    mavenLocal()
    ...
}
```

Use `0.1.0-SNAPSHOT` as your shapekt-version.

To build the IDE plugin zip from source: 
```
./gradlew buildPlugin
```
The plugin zip should be in `ide-plugin/build/distributions`.

## Tutorials

Here are some tutorials to help you get started.
TODO: Link several tutorials here.

## Contributing

We welcome and greatly value all kinds of contributions to ShapeKt. If you would like to contribute, please see our Contributing Guidelines. // TODO Link to CONTRIBUTING.md or some other documentation on website.

## License

ShapeKt is [MIT licensed](./LICENSE).

