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
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * @author Claus Priisholm
 */
class ManifestTaskSpec extends Specification {

   /* def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }*/

    /**
     * Test that a new task can be added with the expected class
     */
    def canAddTaskToProject() {
        given:
        Project project = ProjectBuilder.builder().build()

        expect:
        project.task('testManifest', type: ManifestTask) instanceof ManifestTask
    }

    /**
     * Test that task properties can be set/accessed
     */
    def canSetTaskProperties() {
        given:
        Project project = ProjectBuilder.builder().build()
        ManifestTask task = project.task('testManifest', type: ManifestTask)

        when:
        task.with {
            moduleVfsDir = new File('/foo/bar')
        }

        then:
        task.moduleVfsDir.path == '/foo/bar'
    }

}
