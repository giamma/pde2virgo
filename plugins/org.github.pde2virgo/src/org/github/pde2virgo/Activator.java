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

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 *  Needed only for accessing the platform log facility
 */
public class Activator extends AbstractUIPlugin {

    private static Activator instance;

    public Activator() {
        instance = this;
    }

    public static Activator getDefault() {
        return instance;
    }

}
