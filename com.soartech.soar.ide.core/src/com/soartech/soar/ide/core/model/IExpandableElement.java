package com.soartech.soar.ide.core.model;

/**
 * Represents an expandable element such a soar or tcl command 
 * which can be expanded in the Soar Source Viewer. 
 * 
 * @author aron
 *
 */
public interface IExpandableElement 
{
    /**
     * @return The source for this production after Tcl expansion has been
     *  applied. If Tcl processing failed, this will return <code>null</code>.
     * @throws SoarModelException 
     */
    String getExpandedSource() throws SoarModelException;
}
