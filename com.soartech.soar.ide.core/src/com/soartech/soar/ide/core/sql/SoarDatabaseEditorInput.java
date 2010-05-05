package com.soartech.soar.ide.core.sql;

import java.awt.Point;
import java.util.ArrayList;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import com.soartech.soar.ide.core.model.SoarProblem;

public class SoarDatabaseEditorInput implements IStorageEditorInput {
	
    private SoarDatabaseRowStorage storage;
    private SoarDatabaseRow row;
    private ArrayList<SoarProblem> problems = new ArrayList<SoarProblem>();
    
    public SoarDatabaseEditorInput(SoarDatabaseRow row) {
    	this.row = row;
    	storage = new SoarDatabaseRowStorage(row);
    }
    
    public SoarDatabaseRow getRow() {
    	return row;
    }
    
    public SoarDatabaseRowStorage getSoarDatabaseStorage() {
    	return storage;
    }
    
    public SoarDatabaseEditorInput(SoarDatabaseRowStorage storage) {
    	this.storage = storage;
    }
    
    public boolean exists() {
    	return true;
    }
    
    public ImageDescriptor getImageDescriptor() {
    	return null;
    }
    
    public String getName() {
       return storage.getName();
    }
    public IPersistableElement getPersistable() {
    	return null;
    }
    
    public IStorage getStorage() {
       return storage;
    }
    
    public String getToolTipText() {
		return "Soar database editor input";
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SoarDatabaseEditorInput) {
			boolean ret =  ((SoarDatabaseEditorInput) obj).getSoarDatabaseStorage().equals(this.getSoarDatabaseStorage());
			return ret;
		}
		return false;
	}
	
	public void clearProblems() {
		problems.clear();
	}
	
	public void addProblem(SoarProblem problem) {
		problems.add(problem);
	}
	
	public ArrayList<SoarProblem> getProblems() {
		return problems;
	}
	
	public ArrayList<SoarProblem> problemsAtLine(int line, String document) {
		ArrayList<SoarProblem> ret = new ArrayList<SoarProblem>();
		Point lineRange = characterRangeForLineOfDocument(line, document);
		int lineStart = lineRange.x;
		int lineEnd = lineRange.y;
		for (SoarProblem problem : problems) {
			// -1 for counting from 1.
			
			int start = problem.start - 1;
			int end = problem.start + problem.length - 1;
			if (lineStart <= start && lineEnd > end) {
				ret.add(problem);
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param line
	 * @param document
	 * @return A point that really represents a range.
	 */
	public static Point characterRangeForLineOfDocument(int line, String document) {
		int currentLine = 0;
		int beginIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < document.length(); ++i) {
			char c = document.charAt(i);
			if (c == '\n') {
				++currentLine;
				if (currentLine == line) {
					beginIndex = i;
				} else if (currentLine == line + 1) {
					endIndex = i;
				}
			}
		}
		return new Point(beginIndex, endIndex);
	}
}
