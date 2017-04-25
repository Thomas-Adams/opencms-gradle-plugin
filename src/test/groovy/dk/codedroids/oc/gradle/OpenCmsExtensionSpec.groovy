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
import spock.lang.Specification

/**
 * @author Claus Priisholm
 */
class OpenCmsExtensionSpec extends Specification {
    OpenCmsExtension ext = new OpenCmsExtension()
    Project project = Stub()
    TaskStub task = new TaskStub(project: project) // using traits hence the class rather than just a stub

    def setup() {
        project.hasProperty(OpenCmsPlugin.EXT_NAME) >> true
        project.hasProperty(_) >> false
    }

    def 'set and get values using the AccessExtension trait'() {
        given:
        project.getProperties() >> [(OpenCmsPlugin.EXT_NAME): ext]

        when:
        task.getProject().getProperties().get(OpenCmsPlugin.EXT_NAME).moduleName = 'foo'

        then:
        task.getProject().getProperties().get(OpenCmsPlugin.EXT_NAME).moduleName == 'foo'
        task.opencmsExt('moduleName') == 'foo'
        task.opencmsExt('moduleAuthor') == null
        task.opencmsExt('moduleName', 'bar') == 'foo'
        task.opencmsExt('moduleAuthor', 'bar') == 'bar'
    }

    def 'no extension case using the AccessExtension trait'() {
        expect:
        task.opencmsExt('moduleAuthor') == null
        task.opencmsExt('moduleAuthor', 'bar') == 'bar'    }

    /**
     * Just for testing the setup of the test works
     * @return
     */
    def 'verification of mocking'() {
        given:
        project.getProperties() >> [(OpenCmsPlugin.EXT_NAME): ext]

        expect:
        task instanceof AccessExtension
        task.getProject() instanceof Project
        task.getProject().hasProperty(OpenCmsPlugin.EXT_NAME) == true
        task.getProject().hasProperty('foo') == false
        task.getProject().getProperties().get('foo') == null
        task.getProject().getProperties().get(OpenCmsPlugin.EXT_NAME) instanceof OpenCmsExtension
    }
}
