/*******************************************************************************
 *  Copyright (c) 2012 GianMaria Romanato
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     GianMaria Romanato - initial API and implementation
 *******************************************************************************/
package org.github.pde2virgo;

import static org.github.pde2virgo.Constants.DEBUG_KEY;
import static org.github.pde2virgo.Constants.MANIFEST_MF;
import static org.github.pde2virgo.Constants.META_INF;
import static org.github.pde2virgo.Constants.PLUGIN_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;

public class Helper {

    /* package */ static boolean DEBUG = "true".equals(Platform.getDebugOption(DEBUG_KEY)); //$NON-NLS-1$
    private static ILog log = Activator.getDefault().getLog();

    /* key for the preferred editor persistent property, see org.eclipse.ide.ui.IDE */
    private static final QualifiedName EDITOR_PROPERTY = new QualifiedName(
            "org.eclipse.ui.internal.registry.ResourceEditorRegistry", "EditorProperty"); //$NON-NLS-1$ //$NON-NLS-2$

    /* ID of the PDE plug-in manifest editor, see org.eclipse.pde.internal.ui.IPDEUIConstants */
    private static final String EDITOR_VALUE = "org.eclipse.pde.ui.manifestEditor"; //$NON-NLS-1$

    /**
     * emit debug log
     */
    /* package */ static void debug(String message) {
        log.log(new Status(IStatus.OK, PLUGIN_ID, message));
    }

    /**
     * emit error log
     */
    /* package */ static void error(String message) {
        log.log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * tells whether a META-INF folder exists in the output location that contains a MANIFEST.MF file
     * @param outputLocation
     * @return
     * @throws CoreException
     */
    static boolean checkMETAINFFolder(IProject project) throws CoreException {
        IPath outputLocation = getOutputLocation(project);
        IFolder binFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);
        if (!binFolder.exists()) {
            if (DEBUG) {
                debug("Creating bin folder"); //$NON-NLS-1$
            }
            binFolder.create(true, true, null);
            return false;
        } else {
            if (DEBUG) {
                debug("Refreshing bin folder"); //$NON-NLS-1$
            }
            binFolder.refreshLocal(IResource.DEPTH_ONE, null);
        }
        IFolder binaryMetaInf = binFolder.getFolder(META_INF);
        if (!binaryMetaInf.exists()) {
            if (DEBUG) {
                debug("Creating bin/META-INF"); //$NON-NLS-1$
            }
            binaryMetaInf.create(true, true, null);
            return false;
        } else {
            if (DEBUG) {
                debug("bin/META-INF already exists, refreshing"); //$NON-NLS-1$
            }
            binaryMetaInf.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
        IFile binaryManifest = binaryMetaInf.getFile(MANIFEST_MF);
        if (!binaryManifest.exists()) {
            if (DEBUG) {
                debug("bin/META-INF/MANIFEST.MF does not exist"); //$NON-NLS-1$
            }
            return false;
        }
        return true;
    }

    /* package */ static java.util.List<String> getLibraryEntries(IProject project) throws CoreException {
        IFolder metaInf = project.getFolder(META_INF);
        if (!metaInf.exists()) {
            return Collections.emptyList();
        }

        IFile manifest = metaInf.getFile(MANIFEST_MF);
        if (!manifest.exists()) {
            return Collections.emptyList();
        }

        HashMap<String, String> manifestEntries = new HashMap<String, String>();
        try {
            ManifestElement.parseBundleManifest(manifest.getContents(), manifestEntries);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                    Messages.Helper_ManifestParsingError, e));
        }

        String rawValue = manifestEntries.get("Bundle-ClassPath"); //$NON-NLS-1$
        java.util.List<String> toCopy = new ArrayList<String>();

        if (rawValue != null) {
            String[] classpathEntries = ManifestElement.getArrayFromList(rawValue);
            for (String classpathEntry : classpathEntries) {
                if (".".equals(classpathEntry)) { //$NON-NLS-1$
                    continue;
                } else {
                    toCopy.add(classpathEntry);
                }
            }
        }
        if (DEBUG) {
            debug("JARS declared in manifest: " + Arrays.toString(toCopy.toArray())); //$NON-NLS-1$
        }
        return toCopy;
    }

    /**
     * Gets the output location of the project. Either returns the global output location
     * or the first output location found for a source folder.
     * @return
     * @throws CoreException
     */
    /* package */ static IPath getOutputLocation(IProject project) throws CoreException {
        IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
        IPath outputLocation = jp.getOutputLocation();

        if (outputLocation == null) {
            IClasspathEntry[] entries = jp.getRawClasspath();

            for (IClasspathEntry iClasspathEntry : entries) {
                if (iClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    outputLocation = iClasspathEntry.getOutputLocation();
                    if (outputLocation != null) {
                        break;
                    }
                }
            }
        }

        if (outputLocation == null) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                    Messages.Helper_BinFolderError));
        }
        return outputLocation;
    }


    /**
     * Copies a library pointed by the given MANIFEST.MF classpath under the outputlocation
     * @param project
     * @param path
     * @param outputLocation
     * @throws CoreException
     */
    /* package */ static void copyLibraryToBin(IProject project, String path) throws CoreException {
        IPath outputLocation = getOutputLocation(project);
        IFolder binFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);

        IResource target = binFolder.findMember(path);

        if (target!=null && target.exists()) {
            if (DEBUG) {
                debug(path + " already exists, deleting"); //$NON-NLS-1$
            }
            target.delete(true, null);
            if (DEBUG) {
                debug(path + " deleted"); //$NON-NLS-1$
            }
        }
        IResource libraryResource = project.findMember(new Path(path));
        if (libraryResource!=null) {
            IPath destinationPath = binFolder.getFullPath().append(path);
            IPath binRelativePath = destinationPath.removeFirstSegments(outputLocation.segmentCount());
            binRelativePath = binRelativePath.removeLastSegments(1);
            int segmentCount = binRelativePath.segmentCount();
            if (segmentCount>0) {
                for (int i = 0; i < segmentCount; i++) {
                    IFolder aFolder = binFolder.getFolder(binRelativePath.removeLastSegments(segmentCount-1-i));
                    if (!aFolder.exists()) {
                        if (DEBUG) {
                            debug("parent folder does not exist: " + aFolder.getFullPath().toString()); //$NON-NLS-1$
                        }
                        aFolder.create(true, true, null);
                        if (DEBUG) {
                            debug("parent folder created"); //$NON-NLS-1$
                        }
                    }
                }
            }

            switch (libraryResource.getType()) {
                case IResource.FILE:
                    IFile libraryFile = project.getFile(new Path(path));
                    libraryFile.copy(destinationPath, true, null);
                    break;
                case IResource.FOLDER:
                    IFolder libraryFolder = project.getFolder(new Path(path));
                    libraryFolder.copy(destinationPath, true, null);
                    break;
            }
            if (DEBUG) {
                debug("copied " +libraryResource.getFullPath().toString() + " to " + destinationPath.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else {
            if (DEBUG) {
                debug("Manifest contains unexisting library entry: " + path); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

    }

    /* package */ static boolean checkLibraries(IProject project) throws CoreException {

        List<String> entries = getLibraryEntries(project);
        if (entries.isEmpty()) {
            return true;
        }
        IPath outputLocation = getOutputLocation(project);
        for (String string : entries) {
            if (project.getFile(string).exists()) { // iff the library really exists
                IPath binFile = outputLocation.append(string);
                IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(binFile);
                if (res == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /* package */ static void forcePDEEditor(IProject project) {
        IFolder metaInf = project.getFolder(META_INF);
        if (metaInf.exists()) {
            IFile manifest = metaInf.getFile(MANIFEST_MF);
            if (manifest.exists()) {
                // quick hack to force Eclipse to use the PDE editor
                try {
                    manifest.setPersistentProperty(EDITOR_PROPERTY, EDITOR_VALUE);
                } catch (CoreException e) {
                    if (DEBUG) {
                        Activator.getDefault().getLog().log(e.getStatus());
                    }
                }
            }
        }
    }
}
