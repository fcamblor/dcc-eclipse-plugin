/*
 * Copyright (c) 1998, Regents of the University of California
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, Berkeley nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. * 
 */
package com.googlecode.eclipse.plugin.directorycpcontainer;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * This element filter filters files from the Java Package View if they are included in a 
 * DirectoryContainer that is on the parent Java project's classpath.  This will prevent 
 * the user from right-clicking hte file and adding it to the build path as a CPE_LIBRARY 
 * classpath entry and thus prevent duplication on the classpath.
 *  
 * @author Frederic Camblor
 */
public class ContainerDirFilter extends ViewerFilter {

    /**
     * @ return false if the Java element is a file that is contained in a 
     * DirectoryContainer that is in the classpath of the owning Java project   
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ViewerFilter#select(
     * org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof IFile) {
            IFile f = (IFile)element;
            IJavaProject jp = JavaCore.create(f.getProject());
            try {
                // lets see if this file is included in a DirectoryContainer
                IClasspathEntry[] entries = jp.getRawClasspath();
                for(IClasspathEntry entry: entries) {
                    if(entry.getEntryKind()==IClasspathEntry.CPE_CONTAINER) {
                        if(DirectoryContainer.ID.isPrefixOf(entry.getPath())) {
                            // we know this is a DirectoryContainer so lets get the
                            // instance
                            DirectoryContainer con = (DirectoryContainer)JavaCore.
                                              getClasspathContainer(entry.getPath(), jp);
                            if(con.isContained(f.getLocation().toFile())){
                                // this file will is included in the container, so dont 
                                // show it
                                return false;
                            }
                        }
                    }
                }
            } catch(JavaModelException e) {
                Logger.log(Logger.ERROR, e);
            }
        }
        return true;
    }

}
