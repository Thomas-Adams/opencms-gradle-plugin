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

import org.gradle.testkit.runner.GradleRunner

import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Claus Priisholm
 */
class BuildLogicFunctionalSpec extends Specification {
    // TemporaryFolder cleans up after run
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile, vfsFolder, manifestFile, buildFolder, libsFolder, jarFile
    String testModuleName = 'com.codedroids.oc.commons'
    String testModuleVersion = '2.3.4.5'
    String testModuleAuthor = 'CodeDroids ApS'
    String testModuleAuthorEmail = 'support@codedroids.dk'
    String testOpencmsVersion = '10.0.1'
    String testModuleJarName = 'codedroids-occommons-2.3.4.5.jar'
    String testManifest = """<?xml version="1.0" encoding="UTF-8"?><export>
  <info>
    <creator>Admin</creator>
    <opencms_version>10.0.0</opencms_version>
    <createdate>Mon, 23 May 2016 10:12:19 GMT</createdate>
    <infoproject>Offline</infoproject>
    <export_version>10</export_version>
  </info>
  <module>
    <name>com.codedroids.oc.commons</name>
    <nicename>CodeDroids OpenCms 10 Commons module</nicename>
    <export-mode name="default"/>
    <group>CodeDroids OpenCms Suite</group>
    <class/>
    <description>Provides common classes and utilities used by other modules in the CodeDroids OpenCms Suite.
&lt;br&gt;
Copyright (c) 2004-2017 CodeDroids ApS</description>
    <version>1.1.1.1</version>
    <authorname>some author</authorname>
    <authoremail>some email</authoremail>
    <datecreated/>
    <userinstalled/>
    <dateinstalled/>
    <dependencies/>
    <exportpoints>
      <exportpoint uri="/system/modules/com.codedroids.oc.commons/lib/" destination="WEB-INF/lib/"/>
      <exportpoint uri="/system/modules/com.codedroids.oc.commons/classes/" destination="WEB-INF/classes/"/>
    </exportpoints>
    <resources>
      <resource uri="/system/modules/com.codedroids.oc.commons/"/>
    </resources>
    <excluderesources/>
    <parameters/>
  </module>
  <files>
    <file>
      <destination>system</destination>
      <type>folder</type>
      <datelastmodified>Mon, 27 Jun 2005 08:00:00 GMT</datelastmodified>
      <userlastmodified>Admin</userlastmodified>
      <datecreated>Sat, 21 Feb 2009 15:14:44 GMT</datecreated>
      <usercreated>Admin</usercreated>
      <flags>0</flags>
      <properties/>
      <relations/>
      <accesscontrol>
        <accessentry>
          <uuidprincipal>ROLE.WORKPLACE_USER</uuidprincipal>
          <flags>514</flags>
          <permissionset>
            <allowed>1</allowed>
            <denied>0</denied>
          </permissionset>
        </accessentry>
      </accesscontrol>
    </file>
  </files>
</export>"""

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        vfsFolder = testProjectDir.newFolder('src', 'vfs')
        buildFolder = testProjectDir.newFolder('build')
        libsFolder = testProjectDir.newFolder('build', 'libs')
        jarFile = new File(libsFolder, 'codedroids-occommons-2.3.4.5.jar')
        manifestFile = new File(vfsFolder, 'manifest.xml')
    }

    def clean() {
        if(manifestFile.exist())
            manifestFile.delete()
    }


    /**
     * Test that we can define a custom task, set properties and run it
     */
    def "make a custom task based on the ManifestTask"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFile = new File(buildFolder, 'manifest.xml')
        buildFile << """
            plugins {
                id 'dk.codedroids.opencms'
            }
            task myCustomTask(type:dk.codedroids.oc.gradle.ManifestTask) {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = '${testModuleAuthor}'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                doLast {
                    println "CUSTOM TASK LAST"
                }
            } 
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m','myCustomTask')
                .withPluginClasspath()
                .build()

        then:
        generatedFile.exists()
        result.output.contains("CUSTOM TASK LAST")
        result.task(":myCustomTask").outcome == SUCCESS
    }

    /**
     * Test that the template manifest is used and updated with values set in opencms extension
     * @return
     */
    def "invoke manifest task with manifest template and settings via extension"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFile = new File(buildFolder, 'manifest.xml')
        // Because of the test kit we can use the "plugins" rather than "buildScript"
        buildFile << """
            plugins {
                id 'dk.codedroids.opencms'
            }
            project.version = '${testModuleVersion}'
            opencms {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = '${testModuleAuthor}'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m',OpenCmsPlugin.MANIFEST_TASK)
                .withPluginClasspath()
                .build()

        then:
        manifestFile.exists() && manifestFile.text.contains('<group>CodeDroids OpenCms Suite</group>')
        generatedFile.exists()
        generatedFile.text.contains('<name>com.codedroids.oc.commons</name>')
        generatedFile.text.contains("<name>${testModuleName}</name>")
        generatedFile.text.contains("<version>${testModuleVersion}</version>")
        generatedFile.text.contains("<authorname>${testModuleAuthor}</authorname>")
        generatedFile.text.contains("<authoremail>${testModuleAuthorEmail}</authoremail>")
        generatedFile.text.contains("<opencms_version>${testOpencmsVersion}</opencms_version>")
        generatedFile.text.contains("<source>system/modules/com.codedroids.oc.commons/lib/${testModuleJarName}</source>")
        result.task(":${OpenCmsPlugin.MANIFEST_TASK}").outcome == SUCCESS
    }

    /**
     * Test that the template manifest is used and updated with values set in opencms extension
     * @return
     */
    def "invoke manifest task with manifest template and settings via task"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFile = new File(buildFolder, 'manifest.xml')
        buildFile << """
            plugins {
                id 'dk.codedroids.opencms'
            }
            ${OpenCmsPlugin.MANIFEST_TASK} {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = '${testModuleAuthor}'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m',OpenCmsPlugin.MANIFEST_TASK)
                .withPluginClasspath()
                .build()

        then:
        manifestFile.exists() && manifestFile.text.contains('<group>CodeDroids OpenCms Suite</group>')
        generatedFile.exists()
        generatedFile.text.contains("<name>${testModuleName}</name>")
        generatedFile.text.contains("<version>${testModuleVersion}</version>")
        generatedFile.text.contains("<authorname>${testModuleAuthor}</authorname>")
        generatedFile.text.contains("<authoremail>${testModuleAuthorEmail}</authoremail>")
        generatedFile.text.contains("<opencms_version>${testOpencmsVersion}</opencms_version>")
        generatedFile.text.contains("<source>system/modules/com.codedroids.oc.commons/lib/${testModuleJarName}</source>")
        result.task(":${OpenCmsPlugin.MANIFEST_TASK}").outcome == SUCCESS
    }

    /**
     * Test that settings set directly on the task overrides settings in the opencms extension
     * @return
     */
    def "settings via task must have precedence over settings via extension"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFile = new File(buildFolder, 'manifest.xml')
        buildFile << """
            plugins {
                id 'dk.codedroids.opencms'
            }
            opencms {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = 'VIA EXTENSION'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
            ${OpenCmsPlugin.MANIFEST_TASK} {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = 'VIA TASK'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m',OpenCmsPlugin.MANIFEST_TASK)
                .withPluginClasspath()
                .build()

        then:
        manifestFile.exists() && manifestFile.text.contains('<group>CodeDroids OpenCms Suite</group>')
        generatedFile.exists()
        generatedFile.text.contains("<name>${testModuleName}</name>")
        generatedFile.text.contains("<version>${testModuleVersion}</version>")
        generatedFile.text.contains("<authorname>VIA TASK</authorname>")
        generatedFile.text.contains("<authoremail>${testModuleAuthorEmail}</authoremail>")
        generatedFile.text.contains("<opencms_version>${testOpencmsVersion}</opencms_version>")
        result.task(":${OpenCmsPlugin.MANIFEST_TASK}").outcome == SUCCESS
    }

    def "build module dir"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFolder = new File(buildFolder, "${testModuleName}_${testModuleVersion}")
        File generatedFile = new File(generatedFolder, 'manifest.xml')
        buildFile << """
            plugins {
                id 'groovy'
                id 'dk.codedroids.opencms'
            }
            opencms {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = '${testModuleAuthor}'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m',OpenCmsPlugin.MODULE_TASK)
                .withPluginClasspath()
                .build()

        then:
        generatedFolder.exists()
        generatedFolder.isDirectory()
        generatedFile.exists()
        generatedFile.text.contains("<name>${testModuleName}</name>")
        result.task(":${OpenCmsPlugin.MODULE_TASK}").outcome == SUCCESS
    }

    def "build module zip"() {
        given:
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','lib')
        testProjectDir.newFolder('src','vfs','system','modules', 'com.codedroids.oc.commons','classes')
        jarFile.text = 'just-something...'
        manifestFile.text = testManifest
        File generatedFolder = new File(buildFolder, "distributions")
        File generatedFile = new File(generatedFolder, "${testModuleName}_${testModuleVersion}.zip")
        buildFile << """
            plugins {
                id 'groovy'
                id 'dk.codedroids.opencms'
            }
            opencms {
                moduleName = '${testModuleName}'
                moduleVersion = '${testModuleVersion}'
                moduleAuthor = '${testModuleAuthor}'
                moduleAuthorEmail = '${testModuleAuthorEmail}'
                opencmsVersion = '${testOpencmsVersion}'
                moduleJarName = '${testModuleJarName}'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('-Dorg.gradle.jvmargs=-Xmx512m',OpenCmsPlugin.MODULE_ZIP_TASK)
                .withPluginClasspath()
                .build()

        then:
        generatedFile.exists()
        generatedFile.path.endsWith("distributions/${testModuleName}_${testModuleVersion}.zip")
        result.task(":${OpenCmsPlugin.MODULE_ZIP_TASK}").outcome == SUCCESS
    }

}
