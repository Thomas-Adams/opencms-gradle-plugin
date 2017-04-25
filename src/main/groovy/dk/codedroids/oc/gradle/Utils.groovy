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
 * @author Claus Priisholm
 */
class Utils {

    /**
     * TODO this list is work in progress
     */
    static String getType(String ext) {
        switch(ext) {
            case 'jsp': return 'jsp'
                break
            case 'png':
            case 'gif':
            case 'jpg': return 'image'
                break
            case 'pdf':
            case 'woff':
            case 'svg':
            case 'eot':
            case 'ttf':
            case 'jar': return 'binary'
                break
            default: return 'plain'
                break
        // xmlpage
        }
    }

    /**
     * Filter on filenames, returns true if file is to be ignored
     */
    static boolean isIgnoredFile(String path) {
        if(path.endsWith('_meta.json')
                || path.endsWith('ignore.txt')
                || path.endsWith('/vfs/manifest.xml')
                || path.endsWith('/vfs/manifest.xml~')
                || path.endsWith('/.directory') ) {
            return true
        } else {
            return false
        }
    }

    /**
     * Path extension
     */
    static String getExt(String path) {
        int ndx = path?.lastIndexOf('.')
        if(ndx > 0 && ndx+1 < path.size())
            return path.substring(ndx+1)
        else
            return null
    }

    /**
     * For files it is the file name + _meta.json, for folders it is the folder and then the file _meta.json, e.g. /foo/_meta.json
     */
    static File getMetaFile(File src) {
        String p = src.canonicalPath + (src.isDirectory() ? '/_meta.json' : '_meta.json')
        return new File(p)
    }

    /**
     * As of OpenCms 10 there are two export versions of relevance, version 7 and the new version 10,
     * this returns one or the other depending on the given OpenCms version string
     * @return
     */
    static int getExportVersion(String opencmsVersion) {
        return (Integer.valueOf(opencmsVersion.substring(0, opencmsVersion.indexOf('.'))) < 10) ? 7 : 10
    }

    /**
     * Given a fully qualified class name this returns the corresponding (relative) path
     * @param fqcn
     * @return
     */
    static String getPathForClass(String fqcn) {
        return "${fqcn.replaceAll(/\./,'/')}.class"
    }

}