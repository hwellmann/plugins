/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.editor.tree;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.IKeyTreeModel;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.MessagesEditorMarkers;
import org.eclipse.babel.editor.util.OverlayImageIcon;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for key tree viewer.
 * @author Pascal Essiembre
 */
public class KeyTreeLabelProvider 
        extends LabelProvider implements IFontProvider, IColorProvider {	

    private static final int KEY_DEFAULT = 1 << 1;
    private static final int KEY_COMMENTED = 1 << 2;
    private static final int KEY_VIRTUAL = 1 << 3;
    private static final int BADGE_WARNING = 1 << 4;
    private static final int BADGE_WARNING_GREY = 1 << 5;
    
    /** Registry instead of UIUtils one for image not keyed by file name. */
    private static ImageRegistry imageRegistry = new ImageRegistry();

    
    private MessagesEditor editor;
    private MessagesBundleGroup messagesBundleGroup;

	/**
     * 
     */
    public KeyTreeLabelProvider(
            MessagesEditor editor,
            IKeyTreeModel treeModel) {
        super();
        this.editor = editor;
        this.messagesBundleGroup = editor.getBundleGroup();
    }

    /**
	 * @see ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element) {
        KeyTreeNode node = (KeyTreeNode) element;
        String key = node.getMessageKey();

        int iconFlags = 0;

        // Figure out background icon
        if (messagesBundleGroup.isMessageKey(key)) {
            //TODO create check (or else)
//            if (!noInactiveKeyCheck.checkKey(messagesBundleGroup, node.getPath())) {
//                iconFlags += KEY_COMMENTED;
//            } else {
                iconFlags += KEY_DEFAULT;
                
//            }
        } else {
            iconFlags += KEY_VIRTUAL;
        }
        
        // Maybe add warning icon
        //TODO implement preferences here
//        if (RBEPreferences.getReportMissingValues()) {
        if (editor.getMarkers().isMarked(key)) {
            iconFlags += BADGE_WARNING;
        } else if (isOneChildrenMarked(node)) {
            iconFlags += BADGE_WARNING_GREY;
        }
//        }
        return generateImage(iconFlags);
	}

	/**
	 * @see ILabelProvider#getText(Object)
	 */
	public String getText(Object element) {
        return ((KeyTreeNode) element).getName(); 
	}

    /**
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
	public void dispose() {
//TODO        imageRegistry.dispose();   could do if version 3.1        
	}

    /**
     * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
     */
    public Font getFont(Object element) {
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
     */
    public Color getForeground(Object element) {
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
     */
    public Color getBackground(Object element) {
        return null;
    }
    
    /**
     * Generates an image based on icon flags. 
     * @param iconFlags
     * @return generated image
     */
    private Image generateImage(int iconFlags) {
        Image image = imageRegistry.get("" + iconFlags); //$NON-NLS-1$
        if (image == null) {
            // Figure background image
            if ((iconFlags & KEY_COMMENTED) != 0) {
                image = getRegistryImage("keyCommented.png"); //$NON-NLS-1$
            } else if ((iconFlags & KEY_VIRTUAL) != 0) {
                image = getRegistryImage("keyVirtual.png"); //$NON-NLS-1$
            } else {
                image = getRegistryImage("keyDefault.png"); //$NON-NLS-1$
            }
            
            // Add warning icon
            if ((iconFlags & BADGE_WARNING) != 0) {
                image = overlayImage(image, "warning.gif", //$NON-NLS-1$
                        OverlayImageIcon.BOTTOM_RIGHT, iconFlags);
            } else if ((iconFlags & BADGE_WARNING_GREY) != 0) {
                image = overlayImage(image, "warningGrey.gif", //$NON-NLS-1$
                        OverlayImageIcon.BOTTOM_RIGHT, iconFlags);
            }
        }
        return image;
    }
    
    private Image overlayImage(
            Image baseImage, String imageName, int location, int iconFlags) {
        /* To obtain a unique key, we assume here that the baseImage and 
         * location are always the same for each imageName and keyFlags 
         * combination.
         */
        String imageKey = imageName + iconFlags;
        Image image = imageRegistry.get(imageKey);
        if (image == null) {
            image = new OverlayImageIcon(baseImage, getRegistryImage(
                    imageName), location).createImage();
            imageRegistry.put(imageKey, image);
        }
        return image;
    }

    private Image getRegistryImage(String imageName) {
        Image image = imageRegistry.get(imageName);
        if (image == null) {
            image = UIUtils.getImageDescriptor(imageName).createImage();
            imageRegistry.put(imageName, image);
        }
        return image;
    }

    private boolean isOneChildrenMarked(KeyTreeNode parentNode) {
        MessagesEditorMarkers markers = editor.getMarkers();
        KeyTreeNode[] childNodes = 
                editor.getKeyTreeModel().getChildren(parentNode);
        for (int i = 0; i < childNodes.length; i++) {
            KeyTreeNode node = childNodes[i];
            if (markers.isMarked(node.getMessageKey())) {
                return true;
            }
            if (isOneChildrenMarked(node)) {
                return true;
            }
        }
        return false;
    }

}