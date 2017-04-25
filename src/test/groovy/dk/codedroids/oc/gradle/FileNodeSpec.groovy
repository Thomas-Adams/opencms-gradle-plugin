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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Claus Priisholm
 */
class FileNodeSpec extends Specification {


    def 'new FileNode characteristics'() {

        given: 'newly instantiated FileNode'
        FileNode fn = new FileNode()

        expect: 'all default values'
        fn instanceof FileNode
        with(fn) {
            hasChildren() == false
            parent == null
            findByPath('/') == null
            getRoot() == fn
            getAbsolutePath() == ''
        }
    }

    def 'check null parameters'() {
        given:
        FileNode fn = new FileNode(parent:null, name:null)
        File dummy = new File('/tmp')

        when: 'add child with null path'
        fn.addChildByPath(null, dummy)
        then:
        thrown(NullPointerException)

        when: 'find with null path'
        fn.findByPath(null)
        then:
        thrown(NullPointerException)

        when: 'contains with null path'
        fn.containsPath(null)
        then:
        thrown(NullPointerException)

        when: 'add child with path but no file'
        fn.addChildByPath('/foo', null)
        then:
        fn.hasChildren()
        fn.findByPath('/foo') != null
        fn.findByPath('/foo').rfsSource == null
    }

    //@FailsWith(groovy.lang.MissingMethodException)
    @Unroll
    def 'with unnamed root, add children by path : #path'() {
        given:
        FileNode fn = new FileNode(parent:null, name:null)

        expect:
        fn.addChildByPath(path, null).absolutePath == absPath

        where:
        path || absPath
        '' || ''
        '/' || ''
        '/foo' || 'foo'
        '/foo/' || 'foo'
        '/foo/bar' || 'foo/bar'
        '/foo/bar/' || 'foo/bar'
        '/foo/bar/etc' || 'foo/bar/etc'
        '/foo/bar/etc/' || 'foo/bar/etc'
    }

    @Unroll
    def 'with named root, add children by path : #path'() {
        given:
        FileNode fn = new FileNode(parent:null, name:'')

        expect:
        fn.addChildByPath(path, null).absolutePath == absPath

        where:
        path || absPath
        '' || '/'
        '/' || '/'
        '/foo' || '/foo'
        '/foo/' || '/foo'
        '/foo/bar' || '/foo/bar'
        '/foo/bar/' || '/foo/bar'
        '/foo/bar/etc' || '/foo/bar/etc'
        '/foo/bar/etc/' || '/foo/bar/etc'
    }

    @Unroll
    def 'traverse with unnamed root, add children by path : #path'() {
        given:
        FileNode fn = new FileNode(parent:null, name:null)

        expect:
        fn.addChildByPath(path, null).getRoot().traverse(false).size() == levels

        where:
        path || levels
        '' || 1
        '/foo' || 1
        '/foo/bar' || 2
        '/foo/bar/etc' || 3
    }

    @Unroll
    def 'traverse with unnamed root incl. itself, add children by path : #path'() {
        given:
        FileNode fn = new FileNode(parent:null, name:null)

        expect:
        fn.addChildByPath(path, null).getRoot().traverse(true).size() == levels

        where:
        path || levels
        '' || 2
        '/foo' || 2
        '/foo/bar' || 3
        '/foo/bar/etc' || 4
    }
}
