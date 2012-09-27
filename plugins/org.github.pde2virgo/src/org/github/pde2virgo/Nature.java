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

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * A marker nature that is used to contribute a custom build command, see {@link Builder}
 * @author giamma
 *
 */
public class Nature implements IProjectNature {

	private IProject project;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	@Override
    public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(Constants.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(Constants.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
    public void deconfigure() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(Constants.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	@Override
    public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	@Override
    public void setProject(IProject project) {
		this.project = project;
	}

}
