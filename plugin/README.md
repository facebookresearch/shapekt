# Tensor Typing Compiler Plugin

## Set Up
If on a mac, 

1) Include the following in your `~/.zshrc`:
```
export JAVA_HOME=`/usr/libexec/java_home -v 11`
```
2) Create the following file at `../scripts/github.env`. (The scripts folder should already exist and contains `set_env_vars.sh`.)
  The access token can be created [here](https://github.com/settings/tokens). It should have the `read:packages` scope.
```
GITHUB_ACTOR=<your username>
GITHUB_TOKEN=<your access token with the read:packages permission>
```

## How to Use

### Local Publishing and Use
To publish to MavenLocal: 
```
./gradlew publishToMavenLocal
```

#### Calling Project Configuration
In the calling project,

Apply the tensor typing gradle plugin in `build.gradle.kts`:
```
plugins {
    id("shapeTyping") version "0.1.0-SNAPSHOT"
    // ... other plugins
}

// ... the rest of the build file
```

Add the annotations and shape-functions projects as dependencies in `build.gradle.kts`:
```
dependencies {
    implementation("shapeTyping:annotations:0.1.0-SNAPSHOT")
    
    // Enables default shape functions such as matmul and broadcast.
    implementation("shapeTyping:shape-functions:0.1.0-SNAPSHOT")
} 
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
    mavenCentral()
    mavenLocal()
}
```

### Calling Project with Custom Extensions Configuration
You will need a separate gradle subproject for your extensions that is a dependency of your main project.
For example, given two gradle subprojects, :main and :extensions, you would have the following build files:

main/build.gradle.kts
```
plugins {
    id("shapeTyping") version "0.1.0-SNAPSHOT"
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
    id("shapeTyping.extensions") version "0.1.0-SNAPSHOT"

}

shapeTypingExtensions {
    name = "MyExtensions"
    vendor = "MyVendor"
    // Other configs
}

```
### IDE Plugin

To build IDE plugin zip: 
```
./gradlew buildPlugin
```
The plugin zip should be in `ide-plugin/build/distributions`.

#### Calling Project Configuration
In the calling project,

To install the Kotlin IntelliJ plugin,
Download the plugin zip from [here](https://drive.google.com/drive/folders/1tbj-Y9LF4cOLF0TU7HRYdPaKCICuxBjT).
In IntelliJ, Go to `IntelliJ IDEA > Preferences... > Plugins > (Settings Logo in top bar) > Install Plugin from Disk...`, 
select the plugin zip, and restart IntelliJ to enable the plugin.

To install the ShapeTyping plugin,
In IntelliJ, Go to `IntelliJ IDEA > Preferences... > Plugins > (Settings Logo in top bar) > Install Plugin from Disk...`, 
select the plugin zip in `ide-plugin/build/distributions`, and restart IntelliJ to enable the plugin.

### Github Package Publishing
The Github Action to publish this project to Github Packages is at 
`differentiable/.github/workflows/publish_tensor_typing.yml`.

This Github Action publishes this project on a push to master (which occurs with a new merge).
