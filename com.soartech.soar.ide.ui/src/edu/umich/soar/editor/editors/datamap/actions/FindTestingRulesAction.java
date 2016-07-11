package edu.umich.soar.editor.editors.datamap.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.Action;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;

//import edu.umich.soar.editor.editors.SoarRuleParser;
//import edu.umich.soar.editor.editors.SoarRuleParser.SoarParseError;
import edu.umich.soar.editor.editors.datamap.Datamap;
import edu.umich.soar.editor.editors.datamap.DatamapAttribute;
import edu.umich.soar.editor.editors.datamap.DatamapNode;
import edu.umich.soar.editor.editors.datamap.DatamapNode.NodeType;
import edu.umich.soar.editor.editors.datamap.DatamapUtil;
import edu.umich.soar.editor.editors.datamap.TerminalPath;
import edu.umich.soar.editor.editors.datamap.Triple;
import edu.umich.soar.editor.editors.datamap.actions.DatamapSearchResultSet.ResultItem;
import edu.umich.soar.editor.search.SoarSearchResultsView;

public class FindTestingRulesAction extends Action implements ISearchQuery
{
    private DatamapAttribute attribute;
    private boolean test;
    private boolean create;

    public FindTestingRulesAction(DatamapAttribute attribute, boolean test, boolean create)
    {
        super("Find rules that" + (test && !create ? " test " : test && create ? " test or create " : !test && create ? " create " : " ??? ")
                + "this attribute");
        this.attribute = attribute;
        this.test = test;
        this.create = create;
    }

    @Override
    public void run()
    {
        Datamap datamap = attribute.datamap;
        List<Object> attributePathList = attribute.getPathList();
        File datamapFile = datamap.getFile();
        IFile datamapIFile = datamap.getIFile();
        File datamapDir = null;
        IContainer datamapIDir = null;
        try
        {
            datamapDir = datamapFile.getCanonicalFile().getParentFile();
            datamapIDir = datamapIFile.getParent();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            return;
        }
        File[] datamapDirFiles = datamapDir.listFiles();
        IResource[] datamapIDirFiles;
        try
        {
            datamapIDirFiles = datamapIDir.members();
        }
        catch (CoreException e1)
        {
            e1.printStackTrace();
            return;
        }

        List<ResultItem> results = new ArrayList<ResultItem>();

        System.out.println("Finding rules that test: " + attribute.name + " - " + attribute.getPathStringCorrected());
        

        ISoarProject soarProject = null;
        try {

            // get the project from the datamap file
            soarProject = SoarCorePlugin.getDefault().getInternalSoarModel().createSoarProject(datamapIFile.getProject());
            
            System.out.println("Processing SoarProject: " + soarProject.getPath());
            
            //get the agent to check by getting a soar file next to the .dm file
            ISoarAgent agentToCheck = null;
            for (IResource resource : datamapIDirFiles)
            {
                if (resource instanceof IFile)
                {
                      IFile file = (IFile) resource;
                      
                      if (file.getName().endsWith(".soar"))
                      {
                          ISoarFile soarFile = soarProject.getSoarFile(file);
                          if(soarFile != null) 
                          {
                              ISoarAgent soarAgent = soarProject.getPreferredAgent(soarFile);
                              if(soarAgent != null) 
                              {
                                  agentToCheck = soarAgent;
                                  break;
                              }
                          }
                      }
                }
                
            }
            
            System.out.println("Processing Soar Agent: " + agentToCheck.getName());
            
            ISoarDatamap soarDatamap = agentToCheck.getDatamap();
            
            Object[] attrs = soarDatamap.getState().getAttributes().toArray();
            Set<ISoarDatamapAttribute> attrSet = soarDatamap.getState().getAttributes();
            
            System.out.println("Attrs: " + attrs.length);
            for(int i = 0; i < attrs.length; i++)
            {
                ISoarDatamapAttribute attr = (ISoarDatamapAttribute) attrs[i];
                System.out.println("attr: " + attr.getName());
            }
            
            //make a lookupMap of the attrs
            Map<String, ISoarDatamapAttribute> attrMap = new HashMap<String, ISoarDatamapAttribute>();
            for(ISoarDatamapAttribute sda : attrSet)
            {
                attrMap.put(sda.getName(), sda);
            }
            
            //split the attribute name
            String[] attrName = attribute.getPathStringCorrected().split("\\.");
            
            if(attrName.length == 1)
            {
                ISoarDatamapAttribute attr = attrMap.get(attrName[0]);
                
                if(attr != null)
                {
                    System.out.println("Matched full path: " + attribute.getPathStringCorrected());
                    results.addAll(matchAttribute(attr));
                }
                else
                {
                    System.out.println("No attribute with full path: " + attribute.getPathStringCorrected());
                }
            }
            else
            {
                //copy the main attr map so we can drill down
                Map<String, ISoarDatamapAttribute> attrMapCopy = new HashMap<String, ISoarDatamapAttribute>();
                attrMapCopy.putAll(attrMap);
                
                //each loop goes another step in the path
                ISoarDatamapAttribute attrCopy = attrMapCopy.get(attrName[0]);
                for(String attr : attrName)
                {
                    //get the attr from the map
                    attrCopy = attrMapCopy.get(attr);
                    
                    //make a new attrMapInner which is the target of the current attr
                    Map<String, ISoarDatamapAttribute> attrMapInner = new HashMap<String, ISoarDatamapAttribute>();
                    for(ISoarDatamapAttribute sda : attrCopy.getTarget().getAttributes())
                    {
                        attrMapInner.put(sda.getName(), sda);
                    }
                    
                    //set the attrMapCopy as the attrMapInner so we can drill down another level
                    attrMapCopy.clear();
                    attrMapCopy.putAll(attrMapInner);
                }
                
                System.out.println("Matched full path: " + attribute.getPathStringCorrected());
                results.addAll(matchAttribute(attrCopy));
                
            }
            
        } catch (SoarModelException e1) {
            e1.printStackTrace();
        }
        
        

        // TODO: display results, somehow
        /*
         * NewSearchUI.activateSearchResultView(); ISearchResultViewPart
         * resultsView = NewSearchUI.getSearchResultView(); if (resultsView !=
         * null) { ISearchResultPage page = resultsView.getActivePage();
         * page.setInput(results, null); }
         */
        

        if(results.size() == 0)
        {
            System.out.println("No rules matched the attribute: " + attribute.getPathStringCorrected());
        }
        
        //sort the results
        Collections.sort(results, new ResultComparator());
        
        SoarSearchResultsView.setResults(results.toArray());
    }
    
    class ResultComparator implements Comparator<ResultItem>
    {
        @Override
        public int compare(ResultItem o1, ResultItem o2) {
            
            if(o1.file.getName().compareTo(o2.file.getName()) == 0)
            {
                return Integer.compare(o1.offset, o2.offset);
            }
            else
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        }
    }
    
    private List<ResultItem> matchAttribute(ISoarDatamapAttribute attr)
    {
        List<ResultItem> results = new ArrayList<ResultItem>();
        Set<ISoarProduction> attrProductions = attr.getSupportingProductions();
        for(ISoarProduction isp : attrProductions) 
        {
//            IFile soarDatamapFile = isp.getSoarFileProxy().getFile().getFile();
            
            System.out.println("Matched " + attr.getName() + " in " + isp.getProductionName());
            
//            System.out.println("Comparing: " );
//            System.out.println(isp.getSoarFileProxy().getPath().toString());
            
            ResultItem result;
            try {
                result = new ResultItem(isp.getSoarFileProxy().getFile().getFile(), isp.getSourceRange().getOffset(), isp.getSourceRange().getLength());
                results.add(result);
            } catch (SoarModelException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
//            if(!checkedFiles.containsKey(result.toString()))
//            {
//                checkedFiles.put(result.toString(), soarDatamapFile);
//            }
        }
        
        return results;
    }

    /**
     * 
     * @param triples
     *            Triples extracted from an ast of a production
     * @param attributePathList
     *            The path from the attribute we're searching for, back to the
     *            state node
     * @param file
     *            The file that the production came from
     * @param test
     *            If we're searching for rules that test this attribute
     * @param create
     *            If we're searching for tules that create this attribute
     * @return If this ast matches the attribute, a result item for that match;
     *         otherwise null
     */
    private ResultItem pathMatchesTriples(List<Triple> triples, List<Object> attributePathList, IFile file, SoarProductionAst ast, boolean test, boolean create)
    {
        if (attributePathList.size() == 0)
        {
            // Shouldn't happen
            return null;
        }

        if (attributePathList.get(attributePathList.size() - 1) == null)
        {
            // Attribute path was too long
            return null;
        }

        ArrayList<TerminalPath> paths = DatamapUtil.terminalPathsForTriples(triples);

        for (TerminalPath terminalPath : paths)
        {
            ResultItem result = pathMatchesTerminalPath(attributePathList, terminalPath, file, ast, test, create);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    /**
     * 
     * @param triples
     * @param attribute
     * @param file
     * @return
     */
    private List<ResultItem> attributeMatchesTriples(List<Triple> triples, SoarProductionAst ast, DatamapAttribute attribute, IFile file, boolean test,
            boolean create)
    {
        List<ResultItem> ret = new ArrayList<ResultItem>();
        if (attribute.name.equals("operator"))
        {
            Collection<DatamapNode> nameNodes = attribute.getTarget().getChildren("name", NodeType.ENUMERATION);
            HashSet<String> datamapNames = new HashSet<String>();
            for (DatamapNode nameNode : nameNodes)
            {
                assert (nameNode.type == NodeType.ENUMERATION);
                for (String nameValue : nameNode.values)
                {
                    datamapNames.add(nameValue);
                }
            }
            HashMap<String, Triple> operatorNodeNames = new HashMap<String, Triple>();
            for (Triple triple : triples)
            {
                if (triple.attribute.equals("operator") && triple.valueIsVariable())
                {
                    operatorNodeNames.put(triple.value, triple);
                }
            }
            for (Triple triple : triples)
            {
                if ((triple.rhs && create) || (!triple.rhs && test))
                {
                    if (operatorNodeNames.containsKey(triple.variable) && triple.attribute.equals("name") && triple.valueIsConstant()
                            && datamapNames.contains(triple.value))
                    {
                        Triple operatorTriple = operatorNodeNames.get(triple.variable);
                        ResultItem result = new ResultItem(file, ast.getRuleOffset() + operatorTriple.attributeOffset, operatorTriple.attribute.length());
                        ret.add(result);
                    }
                }
            }
        }
        else
        {
            for (Triple triple : triples)
            {
                if ((triple.rhs && create) || (!triple.rhs && test))
                {
                    if (triple.attributeIsConstant() && triple.attribute.equals(attribute.name))
                    {
                        ResultItem result = new ResultItem(file, ast.getRuleOffset() + triple.attributeOffset, triple.attribute.length());
                        ret.add(result);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * 
     * @param attributePathList
     *            The path from the attribute we're searching for, back to the
     *            state node
     * @param terminalPath
     *            A terminal path found from the triples from the current ast
     *            from the current rule
     * @param file
     *            The file that contains the current rule
     * @param test
     *            If we're searching for rules that test this attribute
     * @param create
     *            If we're searching for rules that create this attribute
     * @return If this terminal path contains the attribute that we're looking
     *         for, return a result item indicating that match; otherwise,
     *         return null
     */
    private ResultItem pathMatchesTerminalPath(List<Object> attributePathList, TerminalPath terminalPath, IFile file, SoarProductionAst ast, boolean test,
            boolean create)
    {
        int pathSize = attributePathList.size();
        if (pathSize == 0 || attributePathList.get(0) == null)
        {
            return null;
        }
        for (int i = 0; i < pathSize - 1; ++i) // -1 to skip past state node /
                                               // null node
        {
            if (terminalPath.path.size() <= i)
            {
                return null;
            }
            Triple terminalTriple = terminalPath.path.get(i);
            Object attributePathObject = attributePathList.get(pathSize - 2 - i); // -1
                                                                                  // for
                                                                                  // last
                                                                                  // element,
                                                                                  // -1
                                                                                  // to
                                                                                  // skip
                                                                                  // past
                                                                                  // state
                                                                                  // node
                                                                                  // /
                                                                                  // null
                                                                                  // node
            if (!(attributePathObject instanceof DatamapAttribute))
            {
                return null;
            }
            DatamapAttribute attribute = (DatamapAttribute) attributePathObject;
            if (!attribute.name.equals(terminalTriple.attribute))
            {
                return null;
            }
        }
        Triple matchingTriple = terminalPath.path.get(pathSize - 2);
        if ((test && terminalPath.hasConditionSide()) || (create && terminalPath.hasActionSide()))
        {
            return new ResultItem(file, ast.getRuleOffset() + matchingTriple.attributeOffset, matchingTriple.attribute.length());
        }
        return null;
    }

    @Override
    public boolean canRerun()
    {
        return false;
    }

    @Override
    public boolean canRunInBackground()
    {
        return false;
    }

    @Override
    public String getLabel()
    {
        return "Search thing";
    }

    @Override
    public ISearchResult getSearchResult()
    {
        return null;
    }

    @Override
    public IStatus run(IProgressMonitor arg0) throws OperationCanceledException
    {
        // TODO Auto-generated method stub
        return null;
    }
}
