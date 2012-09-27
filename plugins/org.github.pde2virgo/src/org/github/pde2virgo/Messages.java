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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.github.pde2virgo.messages"; //$NON-NLS-1$
    public static String Builder_IncrementalBuildMessage;
    public static String Builder_FullBuildMessage;
    public static String Builder_copy_libraries;
    public static String Builder_CopyMetaInfContent;
    public static String Helper_BinFolderError;
    public static String Helper_ManifestParsingError;
    public static String ToggleNatureAction_ErrorMessage;
    public static String ToggleNatureAction_ErrorTitle;
    public static String ToggleNatureAction_NatureAddedMessage;
    public static String ToggleNatureAction_NatureAddedTitle;
    public static String ToggleNatureAction_NatureRemovedMessaged;
    public static String ToggleNatureAction_NatureRemovedTitle;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
