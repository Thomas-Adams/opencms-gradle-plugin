# opencms-gradle-plugin
Gradle plugin for building  [OpenCms](http://www.opencms.org) modules

*This is free software, see the included LICENSE text*

Based on `src/vfs` folder, and a template `manifest.xml` this plugin can build a OpenCms module zip which can be imported into OpenCms.

The content of the `src/vfs` folder mimics the layout of Virtual File System (VFS) folder for
the module inside OpenCms. Actually it is highly recommended to create the module via the OpenCms administration,
export the module and unzip such that the content of the module zip ends up in the `src/vfs` folder.

The 'root' should look like this with whatever relevant folders is needed inside the **system** folder:

```
.../src/vfs
.../src/vfs/manifest.xml
.../src/vfs/system/
```

## Usage

Per default `./gradlew uploadArchives` will publish to a local archive (as defined in the included `build.gradle` file 
as environment variable `MAVEN_REPO`).

After that step, you can use the plugin in your own projects.

Your **build.gradle** should include something like the following:

```
buildscript {
    repositories {
        maven { url uri( "file://${System.properties['MAVEN_REPO']}/" ) }
        mavenCentral()
    }
    dependencies {
        classpath group: 'dk.codedroids.oc.gradle', name: 'opencms-plugin', version: '0.7'
    }
}

//apply plugin: 'java'
apply plugin: 'groovy'
apply plugin: 'dk.codedroids.opencms'
```

The the path to the local repository should of course be adjusted if you install it in a different place.

Alternatively you can use Gradle's composite build feature to include the plugin project directly. 
To do that add the following to your **settings.gradle** where the folder is where the plugin project resides:

```
def folder = file('/some/dir/opencms-gradle-plugin')
if(folder.exists()) {
    includeBuild folder
}
```

## Tasks

Once enabled the plugin provides following tasks. The tasks requires some configuration, see the Configuration section below.

### moduleManifest

Given a template **manifest.xml** this builds a new `manifest.xml` based on the template, the **opencms** build settings and the content of the VFS folder.

### module

Builds the module with the resources from the VFS folder and the generated `manifest.xml`

### moduleZip

Builds the module zip file from the generated module folder.

### releaseModule

Builds the module zip file and copies to a predefined folder. 

*Note while the zip includes the jar (if any) this is not the same as **uploadArchives** or other repository tasks.* 

### syncToCms

Pushes (via a **Sync** task) the module to a directory mounted on the OpenCms VFS (via the SMB integration).

*Note it is only pushing to the VFS, it does not read changes made in the VFS*

## Configuration

While some tasks can be configured directly it is recommended to make all the build configuration via the **opencms** extension.

To do that include something like this in your project's **build.gradle**:

```
opencms {
    moduleName = 'org.opencms.locale.da'
    moduleVersion = project.version
    moduleNiceName = 'OpenCms 10 Danish localization for the workplace'
    moduleJarName = ''
    moduleAuthor = 'CodeDroids ApS'
    moduleAuthorEmail = 'support@codedroids.dk'
    moduleClasses = []
    workplaceToolName = ''
    opencmsVersion = '10.0.1'
}
```

### moduleName
The OpenCms module name, eg. **com.foo.bar.some.module**

### moduleVersion
Module release/version number (stored in manifest) - defaults to `project.version`

### moduleNiceName
A descriptive name (free text) - defaults to `project.description`

### moduleAuthor
The author of the module

### moduleAuthorEmail
The email in the manifest info section

### moduleGroup
Module group, optional

### moduleActionClass
Action class, optional

### moduleDescription
A longer description shown in OpenCms module UI, optional (undefined per default)

### moduleVfsUser
The OpenCms user set as creator of module and files (defaults to **Admin**)

### opencmsVersion
The OpenCms version for which the module is intended

### moduleJarName
Module jar which is build and hence not inside the **src/vfs** folder (set to empty if no jar is needed in module)

### moduleClasses
Classes that are not to be included in the jar (in order to override specific classes in OpenCms core by 
means of servlet containers class load order - provided as a list of fully qualified class names

### moduleVfsDIr
This directory basically mirrors the ditto module folder inside OpenCms VFS, defaults to `src/vfs`
    
### moduleManifestFile
Module manifest.xml template, path should typically not be set directly - 
defaults to `src/vfs/manifest.xml`

### workplaceToolName
For OpenCms legacy workplace only, set to empty if not needed

### moduleReleaseDir
Place to store the actual module zip file (used by `releaseModule` - not to be confused with `uploadArchives` and the like)

### opencmsMountDir
The `syncToCms` copies files for modules to this folder, it is assumed to be a mount to OpenCmsMount VFS (via SMB).
*Note that OpenCms' 

### jointCompilation
Legacy setting - enables compilation of both Java and Groovy code (required for the original Ant version of the build script)

Gradle's **groovy**-plugin accepts both Groovy and Java in the `src/main/groovy` folder so in this case it is not needed.

## Virtual Filesystem properties

OpenCms' VFS allows for setting properties on the files and folders. In order to mimic this you can provide **meta** files which 
the `moduleManifest` task then read and inserts into the resulting `manifest.xml`, OpenCms' will then set the properties of
folders when the module is imported.

To set properties for a folder you place the **meta** inside folder like this:

```
.../vfs/system/com.some.module/resources/css/
.../vfs/system/com.some.module/resources/css/_meta.json
```

and contents could be something like this (setting the **export** property on the **css** folder):

```
{
    "properties": { "export": true }
}
```

In case of files add **_meta.json** as extension to the original filename:

```
.../vfs/system/com.some.module/formatters/employee.jsp
.../vfs/system/com.some.module/formatters/employee.jsp_meta.json
```

and contents like this (setting **export** as a shared property, and **cache** property to the **container-element**):

```
{
    "sharedProperties": { "export": false },
    "properties": {
        "cache": "container-element"
    }
}
```

It is also possible to define the file type (as OpenCms cannot always infer it from the file extension):

```
.../vfs/system/com.some.module/formatters/employee.xml
.../vfs/system/com.some.module/formatters/employee.xml_meta.json
```

where the contents is:

```
{
    "type": "formatter_config"
}
```