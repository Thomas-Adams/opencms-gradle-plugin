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


/**
 * Placeholder for properties shared by various task in the plugin, also provides methods implementing defaults given a project
 *
 * @author Claus Priisholm
 */
class OpenCmsExtension {
    /** The OpenCms module name, eg. com.foo.bar.some.module */
    String moduleName
    /** Module release/version number (stored in manifest) - defaults to project.version and it probably always should be that */
    String moduleVersion
    /** Ditto "nice" name (free text) - defaults to project.description */
    String moduleNiceName
    /** The author of the module  */
    String moduleAuthor
    /** The email in the manifest info section */
    String moduleAuthorEmail
    /** Module group, can be omitted*/
    String moduleGroup
    /** Action class, optional */
    String moduleActionClass
    /** A longer description shown in OpenCms module UI, optional - no default */
    String moduleDescription
    /** The OpenCms user set as creator of module and files (defaults to Admin) */
    String moduleVfsUser
    /** The OpenCms version for which the module is targeted */
    String opencmsVersion
    /** Module jar which is build and hence not inside the VFS folder (optional if no jar is needed in module) */
    String moduleJarName
    /** Classes that are not to be included in the jar (in order to override specific classes in OpenCms core by means of Tomcat class load order)
     * - provided as a list of fully qualified class names */
    List<String> moduleClasses
    /** This directory basically mirrors the ditto module folder inside OpenCms VFS */
    def moduleVfsDir
    /** Module manifest.xml template, should typically not be set explicitly - defaults to src/vfs/manifest.xml if not set */
    def moduleManifestFile

    /**
     * TODO is this relevant? If the module is a workplace tool then the name property
     * must be set (typically same as module name, i.e. ${module.vfsname}) - leave empty if not needed
     */
    String workplaceToolName
    /** Module release is the actual zip file, not the jar */
    def moduleReleaseDir // e.g. '/home/riddler/git/ocbuild/releases'
    /** Mount to use for pushing files to cms via SMB, should of course be mounted otherwise content is just copied to this folder. */
    def opencmsMountDir
    /**
     * Flags that we have some groovy code mixed in with the java, usually the groovy plugin assumes mixed code to
     * be inside main/src/groovy, but so far (using ant) this has been stored in main/src/java and this setting the
     * flag to true adds the otherwise "ignored" groovy to the groovy source set
     */
    boolean jointCompilation = false
}
