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
 * Helper class to build the file structure based on the manifest file
 *
 * @author Claus Priisholm
 */
class FileNode {
    FileNode parent
    String name
    Map<String,FileNode> children = new TreeMap<>(String.CASE_INSENSITIVE_ORDER) // mimics OpenCms' sorting
    File rfsSource

    /**
     * Convenience
     * @return
     */
    boolean hasChildren() {
        return children.size() > 0
    }

    /**
     * Convenience that adds the path and automatically inserts missing folder nodes in the hierarchy
     * - path is taken to be relative to the node itself if there is no leading slash, if there is a slash
     * it is added to the root.
     * @return The child node
     */
    FileNode addChildByPath(String path, File rfsSource) {
        if(path.startsWith('/')) {
            return getRoot().addChildByPath(path.substring(1), rfsSource)
        } else {
            String[] parts = path.split('/')
            for(String part : parts) {
                FileNode partChild = children.get(part)
                if (!partChild) {
                    partChild = new FileNode(parent: this, name: part)
                    children.put(part, partChild)
                }
                if (parts.size() > 1) {
                    return partChild.addChildByPath(parts[1..-1].join('/'), rfsSource)
                } else {
                    // A leaf so add rfsSource (if any) to child node before returning
                    if(rfsSource)
                        partChild.rfsSource = rfsSource
                    return partChild
                }
            }
        }
    }

    /**
     *
     * @param path
     * @return The matching node or null if not found
     */
    FileNode findByPath(String path) {
        if(path.startsWith('/')) {
            return getRoot().findByPath(path.substring(1))
        } else {
            String[] parts = path.split('/')
            for(String part : parts) {
                FileNode partChild = children.get(part)
                if (partChild) {
                    if (parts.size() > 1) {
                        return partChild.findByPath(parts[1..-1].join('/'))
                    } else {
                        // A leaf so it is a match
                        return partChild
                    }
                }
            }
        }
        return null
    }

    boolean containsPath(String path) {
        return findByPath(path) != null
    }

    /**
     * Return the root node of the hierarchy
     * @return
     */
    FileNode getRoot() {
        return (parent != null) ? parent.getRoot() : this
    }

    /**
     * Gets absolute path of this node (ignoring parent if the parent name is null)
     * @return
     */
    String getAbsolutePath() {
        String path = ''
        if(parent && parent.name != null)
            path += parent.getAbsolutePath() + '/'
        path += name ?: ''
        return path
    }

    /**
     * Mimics the traversal done by oc and return a flattened list of the hierarchy
     * (excluding itself if is includeThis is false)
     * @return
     */
    List<FileNode> traverse(boolean includeThis) {
        List<FileNode> retval = new ArrayList<>()
        if(includeThis)
            retval.add(this)
        if(this.hasChildren()) {
            // OpenCms returns file children listed before the folder ditto
            for (FileNode child : children.values()) {
                if (!child.hasChildren())
                    retval.add(child)
            }
            for (FileNode child : children.values()) {
                if (child.hasChildren())
                    retval.addAll(child.traverse(true))
            }
        }
        return retval
    }

    /**
     * Shows path and number of children
     * @return
     */
    String toString() {
        return "Path=${getAbsolutePath()} - ${hasChildren() ? 'has children : '+children.size()+'' : ''}"
    }
}
