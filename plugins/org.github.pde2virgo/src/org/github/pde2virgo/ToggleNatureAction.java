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

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Toggles the addition/removal of a bridge nature that allows using a PDE Plug-in project with
 * Virgo tools.
 *
 * The project is to be created as a plug-in project, than this action must be invoked. As a result
 * the project will feature the plugin nature, the OSGi bundle nature (Virgo) and also the
 * {@link Nature}. The latter will come with {@link Builder}
 * @author giamma
 *
 */
public class ToggleNatureAction implements IObjectActionDelegate {

    private static final String VIRGO_TOOLS_NATURE = "org.eclipse.virgo.ide.facet.core.bundlenature"; //$NON-NLS-1$
    private static final String FACET_CORE_NATURE = "org.eclipse.wst.common.project.facet.core.nature"; //$NON-NLS-1$
    private ISelection selection;
    private IWorkbenchPart workbenchPart;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    toggleNature(project);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
     * .IAction, org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.
     * action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.workbenchPart = targetPart;
    }

    /**
     * Toggles sample nature on a project
     *
     * @param project
     *            to have sample nature added or removed
     */
    private void toggleNature(IProject project) {
        try {
            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();

            //  first try to remove
            for (int i = 0; i < natures.length; ++i) {
                if (Constants.NATURE_ID.equals(natures[i])) {
                    // Remove the nature
                    String[] newNatures = new String[natures.length - 1];
                    System.arraycopy(natures, 0, newNatures, 0, i);
                    System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                    description.setNatureIds(newNatures);
                    project.setDescription(description, null);
                    MessageDialog.openInformation(workbenchPart.getSite().getShell(), Messages.ToggleNatureAction_NatureRemovedTitle, Messages.ToggleNatureAction_NatureRemovedMessaged);
                    return;
                }
            }

            // Add the natures
            /*
             * <nature>org.eclipse.wst.common.project.facet.core.nature</nature>
             * <nature>org.eclipse.virgo.ide.facet.core.bundlenature</nature>
             * <nature>com.nterpriseapps.virgo.tools.addon.PDEVirgoBridgeNature</nature>
             */

            boolean skipWST = false, skipVirgo = false;

            for (int i = 0; i < natures.length; ++i) {
                if (FACET_CORE_NATURE.equals(natures[i])) {
                    skipWST = true;
                }
                if (VIRGO_TOOLS_NATURE.equals(natures[i])) {
                    skipVirgo = true;
                }

            }

            int delta = 1;
            delta = skipWST ? delta : delta + 1;
            delta = skipVirgo ? delta : delta + 1;

            String[] newNatures = new String[natures.length + delta];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            int count = 0;
            if (!skipWST) {
                newNatures[natures.length + count++] = FACET_CORE_NATURE;
            }
            if (!skipVirgo) {
                newNatures[natures.length + count++] = VIRGO_TOOLS_NATURE;
            }
            newNatures[natures.length + count] = Constants.NATURE_ID;

            description.setNatureIds(newNatures);
            project.setDescription(description, null);
            MessageDialog.openInformation(workbenchPart.getSite().getShell(), Messages.ToggleNatureAction_NatureAddedTitle, Messages.ToggleNatureAction_NatureAddedMessage);
        } catch (CoreException e) {
            ErrorDialog.openError(workbenchPart.getSite().getShell(), Messages.ToggleNatureAction_ErrorTitle, Messages.ToggleNatureAction_ErrorMessage, e.getStatus());
        }
    }

}
