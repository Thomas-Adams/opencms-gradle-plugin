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
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * @author Claus Priisholm
 */
class OpenCmsPluginTest {
    @Test
    public void hasOpencmsPluginAllTasks() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'dk.codedroids.opencms'

        assertTrue(project.tasks[OpenCmsPlugin.MANIFEST_TASK] instanceof ManifestTask)
        assertTrue(project.tasks[OpenCmsPlugin.MODULE_TASK] instanceof Copy)
        assertTrue(project.tasks[OpenCmsPlugin.MODULE_ZIP_TASK] instanceof Zip)
    }


}
