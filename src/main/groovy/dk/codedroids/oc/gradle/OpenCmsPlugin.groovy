/*
 * Copyright (c) 2016-2017 CodeDroids ApS (http://www.codedroids.dk)
 *
 * This file is part of OpenCms plugin for Gradle.
 *
 * OpenCms plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * OpenCms plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenCms plugin . If not, see <http://www.gnu.org/licenses/>.
 */
package dk.codedroids.oc.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task

/**
 * @author Claus Priisholm
 */
class OpenCmsPlugin implements Plugin<Project> {

    // Collect all task under this name rather than say 'Build'
    static final String GROUP_NAME = 'OpenCms'

    // Task names defined by this plugin
    static final String SYNC_VIA_SMB_TASK = 'syncToCms'
    static final String MANIFEST_TASK = 'moduleManifest'
    static final String MODULE_TASK = 'module'
    static final String MODULE_ZIP_TASK = 'moduleZip'
    static final String MODULE_RELEASE_TASK = 'releaseModule'

    // Register the extension under this name
    static final String EXT_NAME = 'opencms'

    // Defaults
    static final String DEFAULT_VFS_PATH = 'src/vfs'

    @Override
    void apply(Project target) {
        target.apply(plugin: 'base')
        target.extensions.create(EXT_NAME, OpenCmsExtension)

        // Generates the OpenCms module manifest.xml file based on the VFS source dir and the a template manifest.xml
        //
        target.task(
                MANIFEST_TASK,
                type: ManifestTask,
                group: GROUP_NAME,
                description: 'Generates OpenCms module manifest.xml file.'
        )

        // Builds a directory corresponding to the format of the OpenCms module zip
        //
        target.task(
                MODULE_TASK,
                type: org.gradle.api.tasks.Copy,
                group: GROUP_NAME,
                description: 'Generates folders and files for the module zip.'
        ) {
            project.afterEvaluate { p ->
                def moduleZipDir = p.file("build/${p.opencms.moduleName}_${p.opencms.moduleVersion}")
                def moduleVfsDir = p.file(p.opencms.moduleVfsDir ?: OpenCmsPlugin.DEFAULT_VFS_PATH)
                p.with {
                    dependsOn = (opencms.moduleJarName ? ['jar', MANIFEST_TASK]: [MANIFEST_TASK])
                    inputs.dir moduleVfsDir
                    if(opencms.moduleClasses) {
                        inputs.files opencms.moduleClasses.collect({incl -> "build/classes/main/${incl}"})
                    }
                    if(opencms.moduleJarName) {
                        inputs.files "build/libs/${opencms.moduleJarName}"
                    }
                    outputs.dir moduleZipDir

                    // Setup copy task
                    into (moduleZipDir)
                    from (moduleVfsDir) {
                        exclude '**/*_meta.json','**/ignore.txt','**/.git/*','**/.svn/*','**/CVS/*','**/.cvsignore','**/.nbattrs','**/.project','**/.classpath'
                    }
                    from (buildDir) {
                        include "manifest.xml"
                    }
                    // Needed to get specific class copied into the classes VFS dir rather than storing it in the jar
                    opencms.moduleClasses?.each { incl ->
                        def i = incl.lastIndexOf('/')
                        def pkg = (i > 0 ? incl - incl[i+1..-1] : '')
                        into ("system/modules/${opencms.moduleName}/classes/${pkg}") { from "build/classes/main/${incl}" }
                    }
                    if(opencms.moduleJarName) {
                        into ("system/modules/${opencms.moduleName}/lib") { from "build/libs/${opencms.moduleJarName}" }
                    }
                    includeEmptyDirs = true // default anyway...
                }
            }
        }

        // Builds the OpenCms module zip and stores it under build/distributions
        //
        target.task(
                MODULE_ZIP_TASK,
                type: org.gradle.api.tasks.bundling.Zip,
                group: GROUP_NAME,
                description: 'Generate OpenCms module zip file.'
        ) {
            project.afterEvaluate { p ->
                def moduleZipDir = p.file("build/${p.opencms.moduleName}_${p.opencms.moduleVersion}")
                p.with {
                    archiveName = "${opencms.moduleName}_${opencms.moduleVersion}.zip"
                    // default value via java plugin: destinationDir = project.distsDir aka build/distributions

                    // Only input is the zip dir generated by module task
                    dependsOn = [MODULE_TASK]
                    inputs.dir moduleZipDir
                    outputs.file "${destinationDir}/${archiveName}"

                    from (moduleZipDir)
                    includeEmptyDirs = true // default anyway...
                }
            }
        }

        // Simply copy the module to a release dir (by dependency also uploads jar to repos)
        //
        target.task(
                MODULE_RELEASE_TASK,
                type: org.gradle.api.tasks.Copy,
                group: GROUP_NAME,
                description: 'Copy Module zip file to "release" folder.'
        ) {
            project.afterEvaluate { Project p ->
                p.with {
                    def archiveName = "${opencms.moduleName}_${opencms.moduleVersion}.zip"
                    // not really dependent on releaseJar but I often forget to do it manually so lets tack it on the module
                    dependsOn = ['uploadArchives',MODULE_ZIP_TASK]
                    inputs.files "${project.distsDir}/${archiveName}"
                    outputs.file "${opencms.moduleReleaseDir}/${archiveName}"

                    // Setup copy task
                    // default value: destinationDir = project.distsDir aka build/distributions
                    into (opencms.moduleReleaseDir)
                    from "${project.distsDir}/${archiveName}"
                }
            }
        }

        // Syncs directly to OpenCms vfs via SMB mount
        //
        target.task(
                SYNC_VIA_SMB_TASK,
                type:  org.gradle.api.tasks.Sync,
                group: GROUP_NAME,
                description: 'Syncs to OpenCms VFS via SMB mount.'
        ) {
            project.afterEvaluate { Project p ->
                p.with {
                    def moduleVfsDir = file(opencms.moduleVfsDir ?: OpenCmsPlugin.DEFAULT_VFS_PATH)
                    def mountDir = file(opencms.opencmsMountDir ?: '/mnt/OpenCms')
                    def tgtDir = new File(mountDir, "/system/modules/${opencms.moduleName}")

                    dependsOn = (opencms.moduleJarName ? ['jar'] : ['classes'])

                    inputs.dir moduleVfsDir
                    // if the manifest.xml is changed manually this triggers an rebuild (since it is inside the dir)
                    if (opencms.moduleClasses) {
                        inputs.files opencms.moduleClasses.collect({ incl -> "build/classes/main/${incl}" })
                    }
                    if (opencms.moduleJarName) {
                        inputs.files "build/libs/${opencms.moduleJarName}"
                    }
                    outputs.dir tgtDir

                    // Note it differs from ant sync in it has no way to protect files in target option hence we sync the
                    // just module folder rather than the entire vfs-folder.
                    // It's possible to make a include closure and inspect the FileTreeElement say lastModified but those
                    // not included then gets deleted which is not what is wanted...
                    // When using a smb mount it seems to work more or less like copy in that it overwrites unchanged files
                    // so really all it does is to remove superfluous files and add new ones. Not intelligent like rsync...
                    // Maybe better to build local "sync dir" and then rsync that to the share
                    // TODO And in the end the share option still fails to work properly with properties - they can be set via
                    // /__properties/file.txt.properties for the file /file.txt but no way of reading them (no shared properties?)
                    from("${moduleVfsDir}/system/modules/${opencms.moduleName}") {
                        exclude '**/*_meta.json', '**/ignore.txt', '**/.git/*', '**/.svn/*', '**/CVS/*', '**/.cvsignore', '**/.nbattrs', '**/.project', '**/.classpath', '**/.directory', 'manifest.xml'
                    }
                    into(tgtDir)

                    // TODO sync to workplace is problematic as this could be a few icons in the filetypes folder and sync will remove all the others from the folder
                    // Maybe if the source set is individual files it will work???

                    // Handle the jars and classes which is not in the vfs-folder
                    // - seems somewhat arbitrary that the into's need to be relative to the first into...
                    opencms.moduleClasses?.each { incl ->
                        def i = incl.lastIndexOf('/')
                        def pkg = (i > 0 ? incl - incl[i + 1..-1] : '')
                        into("classes/${pkg}") { from "build/classes/main/${incl}" }
                    }
                    if (opencms.moduleJarName) {
                        into("lib") { from "build/libs/${opencms.moduleJarName}" }
                    }
                }
            }
        }

        // Adjust behaviour of related tasks (such as setting name of jar file to match legacy naming)
        //
        target.afterEvaluate { project ->
            project.getAllTasks(false).each { Project key, Set<Task> value ->
                value.each { Task t ->
                    if(t.name=='jar') {
                        project.with {
                            // Defaults to[baseName]-[appendix]-[version]-[classifier].[extension] but we use the explicitly defined moduleJarName
                            t.archiveName = project.opencms.moduleJarName
                            // Exclude classes that the module should store directly rather than in jar
                            t.exclude { src -> project.opencms.moduleClasses.find {excl -> project.file("build/classes/main/${excl}") == src.file } }
                            // From ant, but Gradle jar task already ignores this stuff:
                            // **/.git/*,**/.svn/*,**/CVS/*,**/.cvsignore,**/.nbattrs,**/.project,**/.classpath,**/*_meta.json,*.gwt.xml
                        }
                    }
                }
            }
        }
    }
}

