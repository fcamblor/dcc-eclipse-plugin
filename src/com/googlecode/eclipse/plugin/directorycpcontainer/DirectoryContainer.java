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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/** 
 * This classpath container add archive files from a configured project directory to the
 * classpath as CPE_LIBRARY entries, and it attaches -src/-source/-sources archives as source attachments
 * plus -javadoc javadoc attachments
 * 
 * @author Frederic Camblor
 */
public class DirectoryContainer implements IClasspathContainer {
    public final static Path ID = new Path("com.googlecode.eclipse.plugin.directorycpcontainer.DIR_CONTAINER");
    
    /**
     * Suffixes list to search for sources archive files
     */
    private final static String[] SRC_POSSIBLE_SUFFIXES = new String[]{ "-src", "-source", "-sources" };
    
    /**
     * Suffixes list to search for javadoc archive files
     */
    private final static String[] JAVADOC_POSSIBLE_SUFFIXES = new String[]{ "-javadoc" };
    
    // use this string to represent the root project directory
    public final static String ROOT_DIR = "-";
    
    // user-fiendly name for the container that shows on the UI
    private String _desc;
    // path string that uniquiely identifies this container instance
    private IPath _path;
    // directory that will hold files for inclusion in this container
    private File _dir;
    // Filename extensions to include in container
    private HashSet<String> _exts;
  
    /**
     * This filename filter will be used to determine which files
     * will be included in the container 
     */
    private FilenameFilter _dirFilter = new FilenameFilter() {

        /** 
         * This File filter is used to filter files that are not in the configured 
         * extension set.
         * Also, filters out files that have the correct extension but end with 
         * source/javadoc suffixes, since filenames with this pattern will be attached as 
         * source/javadoc to the corresponding archive.
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            // lets avoid including filenames that end with SRC_POSSIBLE_SUFFIXES since 
            // we will use this as the convention for attaching source
            String[] nameSegs = splitFileNameWithExtension(name);
            
            if(nameSegs.length != 2) {
                return false;
            }
            // If given archive is a src or javadoc filename : filter it !
            if(isSrcFilename(nameSegs[0]) || isJavadocFilename(nameSegs[0])) {
                return false;
            }            
            // Accept file if and only if extension matches AND it isn't src/javadoc
            if(_exts.contains(nameSegs[1].toLowerCase())) {
                return true;
            }           
            return false;
        }
    };
    
    /**
     * Will return true if given filename is special because it is source archive
     * @param filenameWithoutExtension The filename to check, with extension truncated
     * @return True if given filename is a special file corresponding to a source archive
     */
    private static final boolean isSrcFilename(String filenameWithoutExtension){
    	return isSuffixedFilename(filenameWithoutExtension, SRC_POSSIBLE_SUFFIXES);
    }
    
    /**
     * Will return true if given filename is special because it is javadoc archive
     * @param filenameWithoutExtension The filename to check, with extension truncated
     * @return True if given filename is a special file corresponding to a javadoc archive
     */
    private static final boolean isJavadocFilename(String filenameWithoutExtension){
    	return isSuffixedFilename(filenameWithoutExtension, JAVADOC_POSSIBLE_SUFFIXES);
    }
    
    /**
     * Will return true if given filename ends with at least one of the given suffixes
     * @param filenameWithoutExtension Filename to test, with extension truncated
     * @param possibleSuffixes A suffix array
     * @return true if at least one of the suffixes match with filenameWithoutExtension
     */
    private static final boolean isSuffixedFilename(String filenameWithoutExtension, String[] possibleSuffixes){
    	int i=0;
    	while(i<possibleSuffixes.length && !filenameWithoutExtension.endsWith(possibleSuffixes[i])){
    		i++;
    	}
    	
    	return i != possibleSuffixes.length;
    }
    
    /**
     * This constructor uses the provided IPath and IJavaProject arguments to assign the 
     * instance variables that are used for determining the classpath entries included 
     * in this container.  The provided IPath comes from the classpath entry element in 
     * project's .classpath file.  It is a three segment path with the following 
     * segments:   
     *   [0] - Unique container ID
     *   [1] - project relative directory that this container will collect files from
     *   [2] - comma separated list of extensions to include in this container 
     *         (extensions do not include the preceding ".")    
     * @param path unique path for this container instance, including directory  
     *             and extensions a segments
     * @param project the Java project that is referencing this container
     */
    public DirectoryContainer(IPath path, IJavaProject project) {
        _path = path;
        
        // extract the extension types for this container from the path
        String extString = path.lastSegment();
        _exts = new HashSet<String>();
        String[] extArray = extString.split(",");
        for(String ext: extArray) {
            _exts.add(ext.toLowerCase());
        }
        // extract the directory string from the PATH and create the directory relative 
        // to the project
        path = path.removeLastSegments(1).removeFirstSegments(1);   
        File rootProj = project.getProject().getLocation().makeAbsolute().toFile(); 
        if(path.segmentCount()==1 && path.segment(0).equals(ROOT_DIR)) {
            _dir = rootProj;
            path = path.removeFirstSegments(1);
        } else {
            _dir = new File(rootProj, path.toString());
        }        
        
        // Create UI String for this container that reflects the directory being used
        _desc = "/" + path + " Libraries";
    }
    
    /**
     * This method is used to determine if the directory specified 
     * in the container path is valid, i.e. it exists relative to 
     * the project and it is a directory. 
     * 
     * @return true if the configured directory is valid
     */
    public boolean isValid() {
        if(_dir.exists() && _dir.isDirectory()) {
            return true;
        }
        return false;
    }
    
    /** 
     * Returns a set of CPE_LIBRARY entries from the configured project directory 
     * that conform to the configured set of file extensions and attaches a source 
     * archive to the libraries entries if a file with same name ending with 
     * -src is found in the directory. 
     * 
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
        // fetch the names of all files that match our filter
        File[] libs = _dir.listFiles(_dirFilter);
        for( File lib: libs ) {
            // now see if this archive has an associated src jar
            Path srcPath = null;
            String srcAbsPath = retrieveSrcAbsPath(lib);
            if(srcAbsPath != null){
            	srcPath = new Path(srcAbsPath);
            }

            // now see if this archive has an associated javadoc jar
            List<IClasspathAttribute> cpAttributes = new ArrayList<IClasspathAttribute>();
            String javadocAbsPath = retrieveJavadocAbsPath(lib);
            if(javadocAbsPath != null){
            	cpAttributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocAbsPath));
            }
            
            // create a new CPE_LIBRARY type of cp entry with an attached source 
            // archive if it exists
            entryList.add( JavaCore.newLibraryEntry( 
                    new Path(lib.getAbsolutePath()) , srcPath, new Path("/"), 
                    new IAccessRule[0], cpAttributes.toArray(new IClasspathAttribute[0]), false));                
        }
        // convert the list to an array and return it
        IClasspathEntry[] entryArray = new IClasspathEntry[entryList.size()];
        return (IClasspathEntry[])entryList.toArray(entryArray);
    }
    
    /**
     * Will return absolute path of an existing src file corresponding on given lib
     * @param lib Lib to search source on
     * @return null if no src file has been discovered, the absolute path of the source file
     * otherwise
     */
    private String retrieveSrcAbsPath(File lib){
    	return retrieveExistingAbsPath(lib, SRC_POSSIBLE_SUFFIXES);
    }
    
    /**
     * Will return absolute path of an existing javadoc file corresponding on given lib
     * @param lib Lib to search javadoc on
     * @return null if no javadoc file has been discovered, the absolute path of the javadoc file
     * otherwise
     */
    private String retrieveJavadocAbsPath(File lib){
    	return retrieveExistingAbsPath(lib, JAVADOC_POSSIBLE_SUFFIXES);
    }
    
    /**
     * Retrieves absolute path of a concatenation of possibleSuffixes on the given lib
     * If not found, returns null
     * @param lib The lib to start with
     * @param possibleSuffixes An array of suffixes to concatenate with lib
     * @return absolute path of an existing concatenation of possibleSuffixes on the given lib, null otherwise
     */
    private String retrieveExistingAbsPath(File lib, String[] possibleSuffixes){
    	String[] splittedLibWithExtension = splitFileNameWithExtension(lib.getAbsolutePath());
    	String absPath = null;
    	int i=0;
    	while(i<possibleSuffixes.length && absPath==null){
    		
    		Iterator<String> extIter = _exts.iterator();
    		while(extIter.hasNext() && absPath==null){
    			String ext = extIter.next();
                File arc = new File(splittedLibWithExtension[0]+possibleSuffixes[i]+"."+ext);
                // if the source archive exists then get the path to attach it
                if( arc.exists()) {
                    absPath = arc.getAbsolutePath();
                }
    		}
    		
    		i++;
    	}
    	
    	return absPath;
    }
    
    /**
     * Split given filename in an array of two strings :
     * [1] Filename extension part (right to last ".")
     * [0] Filename left part to last "."
     * If no "." is found, returns an array with the current filename string
     * @param filename The filename to split
     * @return If no "." is found in filename : new String[]{ filename }
     * Otherwise : new String[]{ leftPart, extensionPart }
     */
    private static String[] splitFileNameWithExtension(String filename){
    	int extensionPointIndex = filename.lastIndexOf(".");
    	String[] splittedFilename = new String[]{ filename };
    	if(extensionPointIndex != -1){
    		splittedFilename = new String[]{ 
    				filename.substring(0, extensionPointIndex), 
    				filename.substring(extensionPointIndex+1) };
    	}
    	
    	return splittedFilename;
    }
    
    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    public String getDescription() {
        return _desc;
    }
    
    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
     */
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }    
    
    /**
     * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
     */
    public IPath getPath() {
        return _path;
    }
    
    /**
     * @return configured directory for this container
     */
    public File getDir() {
        return _dir;
    }
    
    /**
     * @return whether or not this container would include the file
     */
    public boolean isContained(File file) {
        if(file.getParentFile().equals(_dir)) {
            // peel off file extension
            String fExt = file.toString().substring(file.toString().lastIndexOf('.') + 1);
            // check is it is in the set of cofigured extensions
            if(_exts.contains(fExt.toLowerCase())) {
                return true;
            }
        }        
        return false;
    }    
}
