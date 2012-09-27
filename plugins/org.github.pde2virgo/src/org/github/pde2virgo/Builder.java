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

import static org.github.pde2virgo.Constants.META_INF;
import static org.github.pde2virgo.Helper.DEBUG;

import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A custom build command that ensures that the content of META-INF is copied to the binary folder of
 * the plug-in project so that Virgo Server can find it via classpath.
 *<p>
 * Additionally the builder parsers the MANIFEST.MF and copies to bin folder any nested JAR (Bundle-ClassPath header)
 * <p>
 * @author giamma
 *
 */
public class Builder extends IncrementalProjectBuilder {

    public Builder() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        Helper.forcePDEEditor(getProject());

        IPath outputLocation = Helper.getOutputLocation(getProject());

        if (DEBUG)  {
            String typeStr = "UNKNOWN"; //$NON-NLS-1$
            switch (kind) {
                case INCREMENTAL_BUILD:
                    typeStr = "INCREMENTAL"; //$NON-NLS-1$
                    break;
                case AUTO_BUILD:
                    typeStr = "AUTO"; //$NON-NLS-1$
                    break;
                case CLEAN_BUILD:
                    typeStr = "CLEAN"; //$NON-NLS-1$
                    break;
                case FULL_BUILD:
                    typeStr = "FULL"; //$NON-NLS-1$
                    break;
            }
            debug("Build type " + typeStr + " output location: " + outputLocation.toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (kind == CLEAN_BUILD) {
            if (DEBUG) {
                debug("Doing nothing"); //$NON-NLS-1$
            }
            return null;
        }

        if (kind == FULL_BUILD) {
            fullBuild(outputLocation, monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                if (DEBUG) {
                    debug("Incremental build was requested but delta is null, performing full build"); //$NON-NLS-1$
                }
                fullBuild(outputLocation, monitor);
                return null;
            }

            if (!Helper.checkMETAINFFolder(getProject())) {
                if (DEBUG) {
                    debug("Incremental build was requested but META-INF folder is missing from output location, performing full build"); //$NON-NLS-1$
                }
                fullBuild(outputLocation, monitor);
                return null;
            }

            if (!Helper.checkLibraries(getProject())) {
                if (DEBUG) {
                    debug("Incremental build was requested but some classpath libraries are missing from output location, performing full build"); //$NON-NLS-1$
                }
                fullBuild(outputLocation, monitor);
                return null;
            }

            if (DEBUG) {
                debug("Incremental build"); //$NON-NLS-1$
            }
            incrementalBuild(outputLocation, delta, monitor);
            return null;
        }

        return null;
    }

    private void incrementalBuild(IPath outputLocation, final IResourceDelta delta, IProgressMonitor monitor)
            throws CoreException {
        monitor.beginTask(Messages.Builder_IncrementalBuildMessage, 2);

        if (delta.findMember(new Path(META_INF)) != null) {
            buildMetaInf(outputLocation, new SubProgressMonitor(monitor, 1));
        } else {
            monitor.worked(1);
        }

        buildLibraries(new Predicate<String>() {
            @Override
            public boolean accept(String t) {
                return delta.findMember(new Path(t))!=null;
            }
        }, outputLocation, new SubProgressMonitor(monitor, 1));
        monitor.done();
    }

    private void fullBuild(IPath outputLocation, IProgressMonitor monitor) throws CoreException {
        if (DEBUG) {
            debug("Full build, output location: " + outputLocation.toOSString()); //$NON-NLS-1$
        }
        monitor.beginTask(Messages.Builder_FullBuildMessage, 2);

        buildMetaInf(outputLocation, new SubProgressMonitor(monitor, 1));

        buildLibraries(null, outputLocation, new SubProgressMonitor(monitor, 1));

        monitor.done();


    }

    private void buildLibraries(Predicate<String> predicate, IPath outputLocation, IProgressMonitor monitor) throws CoreException {
        IFolder binFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);
        binFolder.refreshLocal(IResource.DEPTH_ONE, null);

        java.util.List<String> toCopy = Helper.getLibraryEntries(getProject());

        monitor.beginTask(Messages.Builder_copy_libraries, toCopy.size());
        for (String path : toCopy) {
            if (predicate == null || predicate.accept(path)) {
                Helper.copyLibraryToBin(getProject(), path);
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    private void buildMetaInf(IPath outputLocation, IProgressMonitor monitor) throws CoreException, JavaModelException {
        IProject project = getProject();
        IFolder metaInf = project.getFolder(META_INF);

        if (!metaInf.exists()) {
            error("META-INF folder not found for project: " + getProject().getName()); //$NON-NLS-1$
            return;
        }

        IFolder binFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);
        if (!binFolder.exists()) {
            binFolder.create(true, true, null);
        } else {
            binFolder.refreshLocal(IResource.DEPTH_ONE, null);
        }
        IFolder binaryMetaInf = binFolder.getFolder(META_INF);
        if (!binaryMetaInf.exists()) {
            binaryMetaInf.create(true, true, null);
        } else {
            binaryMetaInf.refreshLocal(IResource.DEPTH_ONE, null);
        }

        SubProgressMonitor sub = new SubProgressMonitor(monitor, 1);

        IResource[] children = metaInf.members();
        sub.beginTask(Messages.Builder_CopyMetaInfContent, children.length);
        for (IResource iResource : children) {
            if (!iResource.isTeamPrivateMember() && !iResource.isDerived()) {
                IPath target = binaryMetaInf.getFullPath().append(iResource.getName());
                IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
                if (res != null && res.exists()) {
                    if (DEBUG) {
                        debug(res.getFullPath().toString() + " exists, deleting"); //$NON-NLS-1$
                    }
                    res.refreshLocal(IResource.DEPTH_INFINITE, null);
                    res.delete(true, null);
                    if (DEBUG) {
                        debug(res.getFullPath().toString() + " deleted"); //$NON-NLS-1$
                    }
                }
                iResource.copy(target, true, null);
                if (DEBUG) {
                    debug("Copied " + iResource.getFullPath().toString() + " to " + target.toString()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            sub.worked(1);
        }
        monitor.done();
    }


    private void debug(String string) {
        Helper.debug(getProject().getName() + " - " + string); //$NON-NLS-1$
    }

    private void error(String string) {
        Helper.error(getProject().getName() + " - " + string); //$NON-NLS-1$
    }


}
