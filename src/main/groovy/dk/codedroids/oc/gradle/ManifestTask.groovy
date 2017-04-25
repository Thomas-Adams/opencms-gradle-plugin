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

import groovy.json.JsonSlurper
import groovy.transform.ToString
import groovy.xml.XmlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.safehaus.uuid.UUIDGenerator

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * <p>
 *     Builds the OpenCms module manifest.xml from the sources based on a "template" manifest.xml
 * </p>
 * <p>
 *     It is recommended to create a module from OpenCms, export it and place the unzipped module in the
 *     root of src/vfs/ such that the src/vfs/manifest.xml can be used as the template<br/>
 *     This way a lot of info is given and it is only necessary to make meta files for new files provided by the module,
 *     so certain entries in the manifest.xml can only be set by setting them in a "template" manifest.xml inside the vfs folder
 * </p>
 * <p>
 *     Implementents the functionality from the generate-module-manifest.groovy
 * </p>
 *
 * @author Claus Priisholm
 */
@ToString
class ManifestTask  extends DefaultTask implements AccessExtension {

    // Format used by OpenCms in xml files
    static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", new Locale("en_UK"))

    // Note that the non-optional values can be set via the opencms {} (the OpenCmsExtension) since methods looks value up in extension if not already set
    String moduleName
    String moduleNiceName
    String moduleVersion
    String moduleAuthor
    String moduleAuthorEmail
    String moduleGroup
    String moduleActionClass
    String moduleDescription
    String moduleVfsUser
    String opencmsVersion
    String moduleJarName
    List<String> moduleClasses
    def moduleVfsDir // can be anything that project.file() can understand
    def moduleManifestFile
    /** Generated manifest file (.../build/manifest.xml) - this is hard coded */
    @OutputFile File generatedManifestFile = new File(project.buildDir, 'manifest.xml')

    ManifestTask() {

    }


    //
    // Getters added in order to see if extension object has a value for the field in case the value is not set explicitly
    //

    /** The OpenCms module name, eg. com.foo.bar.some.module */
    @Input String getModuleName() {
        return moduleName ?: opencmsExt('moduleName')
    }

    /** Ditto "nice" name (free text) - defaults to project.description */
    @Input @Optional String getModuleNiceName() {
        return moduleNiceName ?: opencmsExt('moduleNiceName', getProject().description)
    }

    /** Module release/version number (stored in manifest) - defaults to project.version (and it probably always should be that value) */
    @Input String getModuleVersion() {
        // Hmm, project.version is not just null before being set, it is 'unspecified' - try to ignore that...
        return moduleVersion ?: opencmsExt('moduleVersion', getProject().version=='unspecified' ? null : getProject().version)
    }

    /** The author of the module  */
    @Input String getModuleAuthor() {
        return moduleAuthor ?: opencmsExt('moduleAuthor')
    }

    /** The email in the manifest info section */
    @Input String getModuleAuthorEmail() {
        return moduleAuthorEmail ?: opencmsExt('moduleAuthorEmail')
    }

    /** Module group, can be omitted*/
    @Input @Optional String getModuleGroup() {
        return moduleGroup ?: opencmsExt('moduleGroup')
    }

    /** Action class, optional */
    @Input @Optional String getModuleActionClass() {
        return moduleActionClass ?: opencmsExt('moduleActionClass')
    }

    /** A longer description shown in OpenCms module UI, optional - no default */
    @Input @Optional String getModuleDescription() {
        return moduleDescription ?: opencmsExt('moduleDescription')
    }

    /** The OpenCms user set as creator of module and files (optional, defaults to Admin) */
    @Input @Optional String getModuleVfsUser() {
        return moduleVfsUser ?: opencmsExt('moduleVfsUser', 'Admin')
    }

    /** The OpenCms version for which the module is targeted */
    @Input String getOpencmsVersion() {
        return opencmsVersion ?: opencmsExt('opencmsVersion')
    }

    /** Module jar which is build and hence not inside the VFS folder (optional if no jar is needed in module) */
    String getModuleJarName() {
        return moduleJarName ?: opencmsExt('moduleJarName')
    }

    /**
     * Given the moduleJarName this returns the file handle for it (note it must exists otherwise the
     * InputFile annotation throws an exception)
     * @return File
     */
    @InputFile @Optional File getModuleJarFile() {
        return getModuleJarName() ? new File(getProject().buildDir, "libs/${getModuleJarName()}") : null
    }

    /** Classes that are not to be included in the jar (optional)
     * - in order to override specific classes in OpenCms core by means of Tomcat class load order)
     * provided as a list of fully qualified class names */
    List<String> getModuleClasses() {
        return moduleClasses ?: opencmsExt('moduleClasses')
    }

    /**
     * Only uses to list inputs for the task
     * @return
     */
    @InputFiles @Optional List<File> getModuleClassesFiles() {
        return getModuleClasses().collect({ cl -> "build/classes/main/${Utils.getPathForClass(cl)}" }) as List<File>
    }

    /** This directory basically mirrors the ditto module folder inside OpenCms VFS (optional, defaults to <project dir>/src/vfs) */
    @InputDirectory @Optional File getModuleVfsDir() {
        return getProject().file(moduleVfsDir ?: (opencmsExt('moduleVfsDir') ?: OpenCmsPlugin.DEFAULT_VFS_PATH))
    }

    /** Module manifest.xml template, should typically not be set explicitly (optional, defaults to src/vfs/manifest.xml if not set)
     * - note marking it an input file causes gradle to throw an expection if the file is missing */
    @InputFile @Optional File getModuleManifestFile() {
        return getProject().file(moduleManifestFile ?: (opencmsExt('moduleManifestFile') ?: OpenCmsPlugin.DEFAULT_VFS_PATH+'/manifest.xml'))
    }


    /**
     *
     */
    @TaskAction
    def generateManifest() {

        // Once initialized we can validate the settings before building (throws if invalid)
        validateTaskProperties()

        File vfsDir = getModuleVfsDir()
        File manifestTemplate = getModuleManifestFile()

        // At this point this must exists and be readable
        if(!manifestTemplate.canRead()) {
            throw new org.gradle.api.resources.MissingResourceException("The module manifest template file must be readable: ${manifestTemplate.path}")
        }
        // And this must be writable
        def tmpModuleManifestFile = File.createTempFile('manifest', '.xml')
        if(!tmpModuleManifestFile.canWrite()) {
            throw new org.gradle.api.resources.MissingResourceException("The temporary module manifest file must be writeable: ${tmpModuleManifestFile.path}")
        }

        // Lets start by loading the manifest "template"
        def export = new XmlParser().parse(manifestTemplate)

        // This is a new setting in export version 10, either reduced or default - in reduced mode some meta data such as modification dates are dropped
        // - mostly relevant to the effect that we no longer can assume that the meta data is present and thus should not try to update it,
        def reducedMode = (export.module.'export-mode'?.@name[0] == 'reduced')

        // Set module and info settings if set here, otherwise leave value at what is in the template
        if (getModuleName()) {
            export.module.name ? export.module.name[0].value = getModuleName() : export.module[0].append(new Node(null, 'name', getModuleName()))
        }
        if (getModuleNiceName()) {
            export.module.nicename ? export.module.nicename[0].value = getModuleNiceName() : export.module[0].append(new Node(null, 'nicename', getModuleNiceName()))
        }
        if (getModuleVersion()) {
            export.module.version ? export.module.version[0].value = getModuleVersion() : export.module[0].append(new Node(null, 'version', getModuleVersion()))
        }
        if (getModuleAuthor()) {
            export.module.authorname ? export.module.authorname[0].value = getModuleAuthor() : export.module[0].append(new Node(null, 'authorname', getModuleAuthor()))
        }
        if (getModuleAuthorEmail()) {
            export.module.authoremail ? export.module.authoremail[0].value = getModuleAuthorEmail() : export.module[0].append(new Node(null, 'authoremail', getModuleAuthorEmail()))
        }
        if (getModuleGroup()) {
            export.module.group ? export.module.group[0].value = getModuleGroup() : export.module[0].append(new Node(null, 'group', getModuleGroup()))
        }
        if (getModuleActionClass()) {
            export.module.'class' ? export.module.'class'[0].value = getModuleActionClass() : export.module[0].append(new Node(null, 'class', getModuleActionClass()))
        }
        if (getModuleDescription()) {
            export.module.description ? export.module.description[0].value = getModuleDescription() : export.module[0].append(new Node(null, 'description', getModuleDescription()))
        }
        if (getModuleVfsUser()) {
            export.info.creator ? export.info.creator[0].value = getModuleVfsUser() : export.info[0].append(new Node(null, 'creator', getModuleVfsUser()))
        }
        if (getOpencmsVersion()) {
            export.info.opencms_version ? export.info.opencms_version[0].value = getOpencmsVersion() : export.info[0].append(new Node(null, 'opencms_version', getOpencmsVersion()))
        }

        // Find all files for module by scanning the vfsDir - anything  there should not be anything outside /system of any interest
        def vfsFiles = new FileNode(parent: null, name: null) // this is an "anonymous" placeholder for the tree of the source files
        vfsDir.eachFileRecurse() { vfsFile ->
            // Git is not polluting with all sorts of files so just check if meta and manifest files before adding
            String path = vfsFile.canonicalPath
            if(!Utils.isIgnoredFile(path)) {
                // key in xml is the destination path and that is a relative path starting with "system/..."
                vfsFiles.addChildByPath(path-vfsDir.path, vfsFile)
            } else {
                project.logger.lifecycle("\tIgnoring file: ${path}") // info is too noisy hence lifecycle
            }
        }

        if(getModuleJarName()) {
            File jarFile = getModuleJarFile()
            // Since moduleJar may include path parts use file name of jarFile as that excludes all path parts
            vfsFiles.addChildByPath("system/modules/${getModuleName()}/lib/${jarFile.name}", jarFile)
            project.logger.lifecycle("\tWill include ${jarFile}")

        }

        if(getModuleClasses()) {
            List<File> paths = getModuleClasses().collect { Utils.getPathForClass(it) } as List
            paths.each { path ->
                File classFile = new File(project.buildDir, path)
                vfsFiles.addChildByPath("system/modules/${getModuleName()}/classes/${path}", classFile) // Folders must exist in the VFS folder
                project.logger.lifecycle("\tWill include ${classFile}")
            }
        }

        // Compare the files section with the file list:
        // 1. Existing ones gets the last mod date set, and any other meta data if a meta data file is present - value set in the meta data file allways override
        // 2. New ones gets created with last mod date and other values, plus what ever is defined in the meta data file if present
        // 3. Deleted real file entries get removed

        export.files.file.findAll({ !vfsFiles.containsPath(it.destination.text()) }).each {
            project.logger.lifecycle("\tWill exclude: ${it.destination.text()}")
            it.parent().remove(it)
        }

        // Various OpenCms managed xml files tend to "leak" file UUIDs (formatter config is an example, would point
        // to a JSP in the module - OpenCms inserts the UUID into the xml and thus the UUID may end up in the formatter
        // config xml in the source tree). This causes a problem as OpenCms in some cases only look at the UUID and
        // fails even if the given file actually exists. Also formatter JSPs are referenced in many places in the
        // content which could lead to problems if a JSP ended up with a different UUID.
        // So it seems better to provide some uuid if it is a new entry. The org.opencms.util.CmsUUID is based on
        // org.safehaus.uuid stuff - more info: http://www.opengroup.org/dce/info/draft-leach-uuids-guids-01.txt.
        // The gist is that even though OC uses 'time and spacial aware' uuids they will not conflict with ditto
        // name-based (and the latter should stay fixed for a given file path - inside the vfs the path unique).
        // Note the conflict with java.util.UUID - we stick to safehaus since that is what OC uses (and they do not
        // generate the same values)
        org.safehaus.uuid.UUID baseUUID = new org.safehaus.uuid.UUID(org.safehaus.uuid.UUID.NAMESPACE_URL)

        def entries = [:]

        vfsFiles.traverse(false).each { FileNode node ->
            String key = node.getAbsolutePath()
            File vfsFile = node.getRfsSource()

            if(!vfsFile) {
                throw new org.gradle.api.resources.MissingResourceException("No source file found for the node path \"${key}\"")
            }

            // This is the last mod time in the RFS, since creation date is not the same as VFS creation dare
            def lastModDatum = dateFormat.format(new Date(vfsFile.lastModified()))
            // Meta data, if meta is null after this there is no meta for this source file
            def metaFile = Utils.getMetaFile(vfsFile)
            def meta = null
            if(metaFile) {
                if(metaFile.exists()) {
                    project.logger.info("\t... reading meta data from ${metaFile.canonicalPath}")
                    meta = new JsonSlurper().parse(metaFile)
                }
                def metaDatum = dateFormat.format(new Date(metaFile.lastModified()))
                lastModDatum = vfsFile.lastModified() > metaFile.lastModified() ? lastModDatum : metaDatum
            }

            def fileEntry = export.files.file.find {  it.destination.text() == key }
            if(fileEntry) {
                if(vfsFile.isDirectory()) {
                    updateExistingDirectoryEntry(key, fileEntry, meta, lastModDatum, reducedMode)
                } else /* is file */ {
                    updateExistingFileEntry(key, fileEntry, meta, lastModDatum,reducedMode)
                }
                entries.put(key, fileEntry)
                fileEntry.parent().remove(fileEntry) // it will be added again - correctly sorted - unless it really is to be deleted
            } else /* new entry */ {
                project.logger.lifecycle("\tAdding file entry: ${key}")
                if(vfsFile.isDirectory()) {
                    entries.put(key, makeNewDirectoryEntry(key, meta, lastModDatum, baseUUID))
                } else {
                    entries.put(key, makeNewFileEntry(key, vfsFile, meta, lastModDatum, baseUUID))
                }
            }
        }

        entries.each { key, entry ->
            export.files[0].append(entry)
        }

        if(logger.isInfoEnabled()) {
            def writer = new StringWriter()
            XmlUtil.serialize(export, writer)
            logger.info(writer.toString())
            writer.close()
        }
        def writer = new FileWriter(tmpModuleManifestFile)
        XmlUtil.serialize(export, writer)
        writer.close()
        if(!project.buildDir.exists())
            project.buildDir.mkdirs()
        // Copy temp to output file
        generatedManifestFile.withDataOutputStream { os->
            tmpModuleManifestFile.withDataInputStream { is->
                os << is
            }
        }
        tmpModuleManifestFile.deleteOnExit()
    }

    /**
     *
     * @param key
     * @param fileEntry
     * @param meta
     * @param reducedMode
     */
    void updateExistingDirectoryEntry(String key, def fileEntry, def meta, def lastModDatum, boolean reducedMode) {
        // For folder the only way we can change it outside the manifest.xml is by using the meta file, in particular - OC does not
        // update folders last mod date when things change inside the folder so the date is directly or indirectly taken from meta file
        if(meta) {
            if (meta.type)
                fileEntry.type[0].value = meta.type
            // Dates and user meta is not present nor should it be in case we are in reduced mode
            // (the template manifest is assumed to be correct so we won't try to fix anything)
            if (!reducedMode) {
                fileEntry.datelastmodified[0].value = meta.datelastmodified ?: lastModDatum
                // If not specified directly use the timestamp from the meta file itself
                if (meta.userlastmodified)
                    fileEntry.userlastmodified[0].value = meta.userlastmodified
                if (meta.datecreated)
                    fileEntry.datecreated[0].value = meta.datecreated
                if (meta.usercreated)
                    fileEntry.usercreated[0].value = meta.usercreated
                if (meta.flags)
                    fileEntry.flags[0].value = meta.flags
            } /*** else {// Don't change any of these fields...
             // Well, datecreated is still included some examples at least, maybe it is supposed to be present
             // whether reduced or not... add it if there is a meta entry for it
             if(meta.datecreated) {if(fileEntry.datecreated) {fileEntry.datecreated[0].value = meta.datecreated}else {def dcBuilder = new NodeBuilder()
             def newDc = dc.datecreated "${meta.datecreated}"
             export.module[0].append(newDc)}}}***/
            // Note if meta file contains properties, it should contain them all as we replace the existing list with the properties
            // found in meta file (think of them as a list but otherwise same MO as simple values - if in meta file the value from the
            // meta file is used. Not imposing any ordering, perhaps OC sorts them alphabetically but it does not matter.
            if (meta.properties || meta.sharedProperties) {
                fileEntry.properties."property".each {
                    String pn = it.name[0].text()
                    if ('shared' == it.@type) {
                        if (!meta.sharedProperties?.containsKey(pn)) {
                            project.logger.lifecycle("\tDropping shared property: '${pn}' from: ${key}")
                            it.parent().remove(it)
                        }
                    } else {
                        if (!meta.properties?.containsKey(pn)) {
                            project.logger.lifecycle("\tDropping property: '${pn}' from: ${key}")
                            it.parent().remove(it)
                        }
                    }
                }
                meta.properties.each { prop ->
                    def propEntry = fileEntry.properties."property".find {
                        it.name.text() == prop.key && 'shared' != it.@type
                    }
                    if (propEntry) {
                        project.logger.lifecycle("\tUpdating property: '${prop.key}' to: ${key}")
                        propEntry.value[0].value = prop.value.toString() // to allow for JSON booleans and numbers
                    } else if (fileEntry.properties) {
                        project.logger.lifecycle("\tAdding property: '${prop.key}' to: ${key}")
                        def propBuilder = new NodeBuilder()
                        def newProp = propBuilder.property {
                            name "${prop.key}"
                            value "${prop.value}"
                        }
                        fileEntry.properties[0].append(newProp)
                    } else {
                        project.logger.lifecycle("\tAdding properties: '${prop.key}' to: ${key}")
                        def propsBuilder = new NodeBuilder()
                        def newProps = propsBuilder.properties {
                            property {
                                name "${prop.key}"
                                value "${prop.value}"
                            }
                        }
                        fileEntry.append(newProps)
                    }
                }
                meta.sharedProperties.each { prop ->
                    def propEntry = fileEntry.properties."property".find {
                        it.name.text() == prop.key && 'shared' == it.@type
                    }
                    if (propEntry) {
                        project.logger.lifecycle("\tUpdating shared property: '${prop.key}' to: ${key}")
                        propEntry.value[0].value = prop.value.toString()
                    } else if (fileEntry.properties) {
                        project.logger.lifecycle("\tAdding shared property: '${prop.key}' to: ${key}")
                        def propBuilder = new NodeBuilder()
                        def newProp = propBuilder.property(type: 'shared') {
                            name "${prop.key}"
                            value "${prop.value}"
                        }
                        fileEntry.properties[0].append(newProp)
                    } else {
                        project.logger.lifecycle("\tAdding properties: '${prop.key}' to: ${key}")
                        def propsBuilder = new NodeBuilder()
                        def newProps = propsBuilder.properties {
                            property(type: 'shared') {
                                name "${prop.key}"
                                value "${prop.value}"
                            }
                        }
                        fileEntry.append(newProps)
                    }
                }
            }
            // TODO relations, accesscontrol
        }
    }

    /**
     *
     * @param key
     * @param fileEntry
     * @param meta
     * @param reducedMode
     */
    void updateExistingFileEntry(String key, def fileEntry, def meta, def lastModDatum, boolean reducedMode) {
        // In case of existing entries we only update them with values in meta files. If there is no meta-file then entry is not updated
        // except for the datelastmodified field - this always reflects the real filesystem last modified attribute and is set to which ever
        // is newer - source file or meta file (unless it is specifically set on the meta file, then that takes precedence)
        // But only if not in reduced mode...
        if(!reducedMode) {
            if (meta?.datelastmodified) {
                fileEntry.datelastmodified[0].value = meta.datelastmodified
            } else {
                fileEntry.datelastmodified[0].value = lastModDatum
            }
        }
        if(meta) {
            if (meta.type)
                fileEntry.type[0].value = meta.type
            if(!reducedMode) {
                if(meta.userlastmodified)
                    fileEntry.userlastmodified[0].value = meta.userlastmodified
                if(meta.datecreated)
                    fileEntry.datecreated[0].value = meta.datecreated
                if(meta.usercreated)
                    fileEntry.usercreated[0].value = meta.usercreated
                if(meta.flags)
                    fileEntry.flags[0].value = meta.flags
            }

            if(meta.properties || meta.sharedProperties) {
                fileEntry.properties."property".each {
                    String pn = it.name[0].text()
                    if('shared' == it.@type) {
                        if(!meta.sharedProperties?.containsKey(pn)) {
                            project.logger.lifecycle("\tDropping shared property: '${pn}' from: ${key}")
                            it.parent().remove(it)
                        }
                    } else {
                        if(!meta.properties?.containsKey(pn)) {
                            project.logger.lifecycle("\tDropping property: '${pn}' from: ${key}")
                            it.parent().remove(it)
                        }
                    }
                }
                meta.properties.each { prop ->
                    def propEntry = fileEntry.properties."property".find {  it.name.text() == prop.key && 'shared' != it.@type}
                    if(propEntry) {
                        propEntry.value[0].value = prop.value.toString()
                    }
                    else {
                        project.logger.lifecycle("\tAdding property: '${prop.key}' to: ${key}")
                        def propBuilder = new NodeBuilder()
                        def newProp = propBuilder.property {
                            name	"${prop.key}"
                            value	"${prop.value}"
                        }
                        fileEntry.properties[0].append(newProp)
                    }
                }
                meta.sharedProperties.each { prop ->
                    def propEntry = fileEntry.properties."property".find {  it.name.text() == prop.key && 'shared' == it.@type}
                    if(propEntry) {
                        propEntry.value[0].value = prop.value.toString()
                    }
                    else {
                        project.logger.lifecycle("\tAdding shared property: '${prop.key}' to: ${key}")
                        def propBuilder = new NodeBuilder()
                        def newProp = propBuilder.property (type:'shared') {
                            name	"${prop.key}"
                            value	"${prop.value}"
                        }
                        fileEntry.properties[0].append(newProp)
                    }
                }
            }
            // TODO relations, accesscontrol
        }
    }

    def makeNewDirectoryEntry(String key, def meta, def lastModDatum, org.safehaus.uuid.UUID baseUUID) {
        def builder = new NodeBuilder()
        // Using safehaus variant - pure java variant would be like:  java.util.UUID.nameUUIDFromBytes(key.getBytes())
        org.safehaus.uuid.UUID newUUID =  UUIDGenerator.getInstance().generateNameBasedUUID(baseUUID, key)
        return builder.file {
            destination key
            type "${meta?.type ?: 'folder'}"
            uuidstructure "${newUUID}"
            datelastmodified "${meta?.datelastmodified ?: lastModDatum}"
            userlastmodified "${meta?.userlastmodified ?: getModuleVfsUser()}"
            datecreated "${meta?.datecreated ?: lastModDatum}"
            usercreated "${meta?.usercreated ?: getModuleVfsUser()}"
            flags "${meta?.flags ?: 0}"
            properties {
                if (meta?.properties) {
                    meta.properties.each { prop ->
                        property {
                            name prop.key
                            value prop.value
                        }
                    }
                }
                if (meta?.sharedProperties) {
                    meta.sharedProperties.each { prop ->
                        property(type: 'shared') {
                            name prop.key
                            value prop.value
                        }
                    }
                }
            }
            relations()
            accesscontrol()
        }
    }

    def makeNewFileEntry(String key, File vfsFile, def meta, def lastModDatum, org.safehaus.uuid.UUID baseUUID) {
        def builder = new NodeBuilder()
        org.safehaus.uuid.UUID newUUID =  UUIDGenerator.getInstance().generateNameBasedUUID(baseUUID, key)
        return builder.file {
            source			key
            destination 	key
            type			"${meta?.type ?: Utils.getType(Utils.getExt(vfsFile.name))}"
            uuidstructure 	"${newUUID}"
            uuidresource	"${newUUID}"
            datelastmodified	"${meta?.datelastmodified ?: lastModDatum}"
            userlastmodified	"${meta?.userlastmodified ?: getModuleVfsUser()}"
            datecreated		"${meta?.datecreated ?: lastModDatum}"
            usercreated		"${meta?.usercreated ?: getModuleVfsUser()}"
            flags			"${meta?.flags ?: 0}"
            properties {
                if(meta?.properties) {
                    meta.properties.each { prop ->
                        property {
                            name prop.key
                            value prop.value
                        }
                    }
                }
                if(meta?.sharedProperties) {
                    meta.sharedProperties.each { prop ->
                        property(type:'shared') {
                            name prop.key
                            value prop.value
                        }
                    }
                }
            }
            relations()
            accesscontrol()
        }
    }

        //
    // Helper methods
    //

    /**
     * Validate that properties are sane
     */
    private void validateTaskProperties() {
        if(!getModuleName())
            throw new org.gradle.api.InvalidUserDataException('The "moduleName" must be set in order to generate manifest')
        if(!getModuleVersion())
            throw new org.gradle.api.InvalidUserDataException('The "moduleVersion" must be set in order to generate manifest')
        if(!getModuleAuthor())
            throw new org.gradle.api.InvalidUserDataException('The "moduleAuthor" must be set in order to generate manifest')
        if(!getModuleAuthorEmail())
            throw new org.gradle.api.InvalidUserDataException('The "moduleAuthorEmail" must be set in order to generate manifest')
        if(!getModuleVfsDir())
            throw new org.gradle.api.InvalidUserDataException('The "moduleVfsDir" must be set in order to generate manifest')
        if(!project.file(getModuleVfsDir()).exists())
            throw new org.gradle.api.InvalidUserDataException("The \"moduleVfsDir\" (${project.file(getModuleVfsDir()).path}) must exist in order to generate manifest")
        if(!getOpencmsVersion())
            throw new org.gradle.api.InvalidUserDataException('The "opencmsVersion" must be set in order to generate manifest')
    }


}
