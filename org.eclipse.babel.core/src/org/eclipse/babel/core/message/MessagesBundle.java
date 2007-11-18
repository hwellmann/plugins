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
package org.eclipse.babel.core.message;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.eclipse.babel.core.Model;
import org.eclipse.babel.core.message.resource.IMessagesResource;
import org.eclipse.babel.core.message.resource.IMessagesResourceChangeListener;
import org.eclipse.babel.core.util.BabelUtils;


/**
 * For a given scope, all messages for a national language.  
 * @author Pascal Essiembre
 */
public class MessagesBundle extends Model
		implements IMessagesResourceChangeListener {

    private static final long serialVersionUID = -331515196227475652L;

    public static final String PROPERTY_COMMENT = "comment"; //$NON-NLS-1$
    public static final String PROPERTY_ENTRIES = "entries"; //$NON-NLS-1$

    private String comment;
    
    private final Collection orderedKeys = new ArrayList();
    private final Map keyedMessages = new HashMap();

    private final IMessagesResource resource;
    
    private final PropertyChangeListener entryChangeListener =
        new PropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent event) {
                    firePropertyChange(event);
                }
    };

    /**
     * Creates a new <code>MessagesBundle</code>.
     * @param resource the messages bundle resource
     */
    public MessagesBundle(IMessagesResource resource) {
        super();
        this.resource = resource;
        readFromResource();
        // Handle resource changes
        resource.addMessagesResourceChangeListener(
        		new IMessagesResourceChangeListener(){
            public void resourceChanged(IMessagesResource changedResource) {
                readFromResource();
            }
        });
        // Handle bundle changes
        addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
                writetoResource();
            }
        });
    }

    /**
     * Gets the underlying messages resource implementation.
     * @return
     */
    public IMessagesResource getResource() {
        return resource;
    }
    
    /**
     * Gets the locale for the messages bundle (<code>null</code> assumes
     * the default system locale).
     * @return Returns the locale.
     */
    public Locale getLocale() {
        return resource.getLocale();
    }

    /**
     * Gets the overall comment, or description, for this messages bundle..
     * @return Returns the comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for this messages bundle.
     * @param comment The comment to set.
     */
    public void setComment(String comment) {
        Object oldValue = this.comment;
        this.comment = comment;
        firePropertyChange(PROPERTY_COMMENT, oldValue, comment);
    }

    /**
     * @see org.eclipse.babel.core.message.resource
     * 		.IMessagesResourceChangeListener#resourceChanged(
     * 				org.eclipse.babel.core.message.resource.IMessagesResource)
     */
    public void resourceChanged(IMessagesResource changedResource) {
        this.resource.deserialize(this);
    }

    /**
     * Adds a message to this messages bundle.  If the message already exists
     * its properties are updated and no new message is added.
     * @param message the message to add
     */
    public void addMessage(Message message) {
        if (!orderedKeys.contains(message.getKey())) {
            orderedKeys.add(message.getKey());
        }
        if (!keyedMessages.containsKey(message.getKey())) {
            keyedMessages.put(message.getKey(), message);
            message.addPropertyChangeListener(entryChangeListener);
            firePropertyChange(PROPERTY_ENTRIES, null, message);
        } else {
            // Entry already exists, update it.
            Message matchingEntry =
                    (Message) keyedMessages.get(message.getKey());
            matchingEntry.copyFrom(message);
        }
    }
    /**
     * Removes a message from this messages bundle.
     * @param messageKey the key of the message to remove
     */
    public void removeMessage(String messageKey) {
        orderedKeys.remove(messageKey);
        Message message = (Message) keyedMessages.get(messageKey);
        if (message != null) {
            message.removePropertyChangeListener(entryChangeListener);
            keyedMessages.remove(messageKey);
            firePropertyChange(PROPERTY_ENTRIES, message, null);
        }
    }
    /**
     * Removes messages from this messages bundle.
     * @param messageKeys the keys of the messages to remove
     */
    public void removeMessages(String[] messageKeys) {
        for (int i = 0; i < messageKeys.length; i++) {
            removeMessage(messageKeys[i]);
        }
    }

    /**
     * Renames a message key.
     * @param sourceKey the message key to rename
     * @param targetKey the new key for the message
     * @throws MessageException if the target key already exists
     */
    public void renameMessageKey(String sourceKey, String targetKey) {
        if (getMessage(targetKey) != null) {
            throw new MessageException(
            		"Cannot rename: target key already exists."); //$NON-NLS-1$
        }
        Message sourceEntry = getMessage(sourceKey);
        if (sourceEntry != null) {
            Message targetEntry = new Message(targetKey, getLocale());
            targetEntry.copyFrom(sourceEntry);
            removeMessage(sourceKey);
            addMessage(targetEntry);
        }
    }
    /**
     * Duplicates a message.
     * @param sourceKey the message key to duplicate
     * @param targetKey the new message key
     * @throws MessageException if the target key already exists
     */
    public void duplicateMessage(String sourceKey, String targetKey) {
        if (getMessage(sourceKey) != null) {
            throw new MessageException(
            	"Cannot duplicate: target key already exists."); //$NON-NLS-1$
        }
        Message sourceEntry = getMessage(sourceKey);
        if (sourceEntry != null) {
            Message targetEntry = new Message(targetKey, getLocale());
            targetEntry.copyFrom(sourceEntry);
            addMessage(targetEntry);
        }
    }

    /**
     * Gets a message.
     * @param key a message key
     * @return a message
     */
    public Message getMessage(String key) {
        return (Message) keyedMessages.get(key);
    }

    /**
     * Adds an empty message.
     * @param key the new message key
     */
    public void addMessage(String key) {
        addMessage(new Message(key, getLocale()));
    }

    /**
     * Gets all message keys making up this messages bundle.
     * @return message keys
     */
    public String[] getKeys() {
        return (String[]) orderedKeys.toArray(BabelUtils.EMPTY_STRINGS);
    }
    
    /**
     * Iterates through the <code>Message</code> objects in this bundle.
     * @return an iterator
     */
    public Iterator messageIterator() {
        return keyedMessages.values().iterator();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String str = "MessagesBundle=[[locale=" + getLocale() //$NON-NLS-1$
                   + "][comment=" + comment //$NON-NLS-1$
                   + "][entries="; //$NON-NLS-1$
        for (Iterator iter = messageIterator(); iter.hasNext();) {
            str += iter.next().toString();
        }
        str += "]]"; //$NON-NLS-1$
        return str;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((comment == null) ? 0 : comment.hashCode());
        result = PRIME * result + ((entryChangeListener == null)
        		? 0 : entryChangeListener.hashCode());
        result = PRIME * result + ((keyedMessages == null)
        		? 0 : keyedMessages.hashCode());
        result = PRIME * result + ((orderedKeys == null)
        		? 0 : orderedKeys.hashCode());
        result = PRIME * result + ((resource == null)
        		? 0 : resource.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof MessagesBundle)) {
            return false;
        }
        MessagesBundle messagesBundle = (MessagesBundle) obj;
        return equals(comment, messagesBundle.comment)
            && equals(keyedMessages, messagesBundle.keyedMessages);
    }    
    
    private void readFromResource() {
        this.resource.deserialize(this);
    }
    private void writetoResource() {
        this.resource.serialize(this);
    }
}