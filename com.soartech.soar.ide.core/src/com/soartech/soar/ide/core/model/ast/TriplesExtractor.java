/*
 *Copyright (c) 2009, Soar Technology, Inc.
 *All rights reserved.
 *
 *Redistribution and use in source and binary forms, with or without modification,   *are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  * Neither the name of Soar Technology, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,   *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,    *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE    *POSSIBILITY OF SUCH *DAMAGE. 
 *
 * 
 */
package com.soartech.soar.ide.core.model.ast;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class extracts triples from the passed in soar production
 * it ignores the condition/action side information it also ignores
 * any relation besides equals, it is not sensitive to negations
 * @author Brad Jones
 */
 
public class TriplesExtractor {
//////////////////////////////////////////////////////////////////////////////
// Data Members
/////////////////////////////////////////////////////////////////////////////
	private int d_currentUnnamedVar = 0;
	private List<Triple> d_triples = new LinkedList<Triple>();
	private SoarProductionAst d_soarProduction;
	private Set<Pair> d_variables = new TreeSet<Pair>();
	private Set<Pair> d_stateVariables = new TreeSet<Pair>();
	private TripleFactory d_tripleFactory;
	private Map<String, Boolean> d_boundMap;
    private Set<Pair> d_rhsFunctionVariables = new TreeSet<Pair>();
	
/////////////////////////////////////////////////////////////////////////////
// Constructors
/////////////////////////////////////////////////////////////////////////////
	public TriplesExtractor(SoarProductionAst soarProduction) {
		d_soarProduction = soarProduction;
		d_tripleFactory = new DefaultTripleFactory();
		extractTriples();
		extractVariables();
		extractStateVariables();
	}
		
/////////////////////////////////////////////////////////////////////////////
// Accessors
/////////////////////////////////////////////////////////////////////////////
	public List<Triple> triples() {
		return d_triples;
	}
	
	public Set<Pair> variables() {
		return d_variables;
	}
    
    public Set<Pair> getRhsFunctionVariables()
    {
        return d_rhsFunctionVariables;
    }
    
    public Set<Pair> getUnboundRhsFunctionVariables()
    {
        Set<Pair> unbound = new TreeSet<Pair>(d_rhsFunctionVariables);
        unbound.removeAll(d_variables);
        return unbound;
    }
	
	public Set<Pair> stateVariables() {
		return d_stateVariables;
	}
	
	public int getStateVariableCount() {
		return d_stateVariables.size();
	}
	
	public Pair stateVariable() {
		Iterator<Pair> i = d_stateVariables.iterator();
		if(i.hasNext())
			return (Pair)i.next();
		else
			return null;
		
	}
	
    public SoarProductionAst getProduction()
    {
        return d_soarProduction;
    }

    // TODO: I don't think this will work. Where do CoverageTriples come from?
	public boolean isBound(String variable) {
		if(d_boundMap == null) {
			d_boundMap = new TreeMap<String, Boolean>();
            for(Pair p : variables())
            {
				String varName = p.getString();
				for(Iterator<?> j = triples().iterator(); j.hasNext();) {
					CoverageTriple ct = (CoverageTriple)j.next();
					if(varName.equals(ct.getVariable().getString())  || 
					   varName.equals(ct.getAttribute().getString()) || 
					   varName.equals(ct.getValue().getString())) {
						if(ct.isChecking()) {
							d_boundMap.put(varName,Boolean.TRUE);
						}
						else {
							if(d_boundMap.get(varName) == null) {
								d_boundMap.put(varName,Boolean.FALSE);
							}
						}
					}
				}
			}
		}
		return ((Boolean)d_boundMap.get(variable)).booleanValue();
	}
	
	
/////////////////////////////////////////////////////////////////////////////
// Manipulators
/////////////////////////////////////////////////////////////////////////////	
	public void sortTriples(List<String> errors) {
		if(d_stateVariables.size() != 1) {
			errors.add(d_soarProduction.getName() + "(" + d_soarProduction.getStartLine() + "): " 
					   + "productions with only one state variable can be checked.");
			d_triples = new LinkedList<Triple>();
			d_variables.clear();
			d_stateVariables.clear();
			return;
		}
		List<Triple> sorted = new LinkedList<Triple>();
		Queue<Pair> variables = new LinkedList<Pair>();
		variables.offer(d_stateVariables.iterator().next());
		while(!variables.isEmpty()) {
			String currentVar = variables.poll().getString();
			Iterator<Triple> i = d_triples.iterator();
			while(i.hasNext()) {
				Triple t = (Triple)i.next();
				if (t.getVariable().getString().equals(currentVar)) {
					sorted.add(t);
					i.remove(); 	
					if (TripleUtils.isVariable(t.getValue().getString()))
						variables.offer(t.getValue());
				}
			}
		}
		
		if (!d_triples.isEmpty()) {
			errors.add(d_soarProduction.getName() + "(" + d_soarProduction.getStartLine() 
						+ "): variable(s) not connected to state");
		}
		d_triples = sorted;
	}
	
	// Implementation Functions
	private void extractTriples() {
		// Extract Triples from the condition side
        for(Condition c : d_soarProduction.getConditions())
        {
            d_triples.addAll(extractTriples(c.getPositiveCondition()) );
        }
			
		// Extract Triples from the action side
        for(Action a : d_soarProduction.getActions())
        {
            if(a.isVarAttrValMake()) 
            {
                d_triples.addAll(extractTriples(a.getVarAttrValMake()));
            }
            else
            {
                extractRhsFunctionVariables(a.getFunctionCall());
            }
        }
	}
	
	private List<Triple> extractTriples(PositiveCondition pc) {
		// If the this positive condition is a conjunctions then extract
		// all the positive conditions out of it and recursively
		// interpret those
		if(pc.isConjunction()) {
			List<Triple> triples = new LinkedList<Triple>();
            for(Condition c : pc.getConjunction())
            {
                triples.addAll(extractTriples((c).getPositiveCondition()));
            }
			return triples;
		}
		else {
		// Just extract the condition for one identifier
			return extractTriples(pc.getConditionForOneIdentifier());
		}
	}
	
	private List<Triple> extractTriples(ConditionForOneIdentifier cfoi) {
		// This function is long and complicated so I'll explain it the best
		// that I can
		List<Triple> triples = new LinkedList<Triple>();
		// Get all the attribute Value tests
		Iterator<AttributeValueTest> i = cfoi.getAttributeValueTests().iterator();
		boolean hasState = cfoi.hasState();
		
		// For all the attribute value tests
		while(i.hasNext()) {
			Pair variable = cfoi.getVariable();
			List<Pair> attributes = null;
			AttributeValueTest avt = i.next();
			
			// Get the attribute chain
            for(AttributeTest at : avt.getAttributeTests())
            {
				// First time switch
				if(attributes == null) {
					attributes = extract(at.getTest());
				}
				else {
				
				// Ok, they are doing the '.' thing so create a variable
				// value and march on down the line
					List<Pair> newAttributes = extract(at.getTest());
					Pair newVariable = getNextUnnamedVar();
					Iterator<Pair> j = attributes.iterator();
					while(j.hasNext()) {
						Pair attr = (Pair)j.next();
						triples.add(d_tripleFactory.createTriple(variable,attr,newVariable,hasState,true, true));
					}
					attributes = newAttributes;
					variable = newVariable;
					hasState = false;
				}
			}
			
			// In case they didn't have any attributes, put a variable one
			// in its place, (my understanding is that this is exactly what
			// soar does)
			if(attributes == null) {
				attributes = new LinkedList<Pair>();
				attributes.add(getNextUnnamedVar());
			}
			
			// Ok get all the values that we are checking
			List<Pair> values = null;
			for(ValueTest vt : avt.getValueTests())
            {
				if(values == null)
                {
					values = extract(vt.getTest());
                }
				else
                {
					values.addAll(extract(vt.getTest()));
                }
			}
			
			// In case they didn't check for any values, put a variable in
			// there, my understanding is that soar does the exact same thing
			if(values == null) {
				values = new LinkedList<Pair>();
				values.add(getNextUnnamedVar());
			}
			
			// Put the attributes and variables together with the 
			// variables into triples
            for(Pair attr : attributes)
            {
				for(Pair val : values)
                {
					triples.add(d_tripleFactory.createTriple(variable,attr,val,hasState,true, true));
				}
			}
		}
		return triples;	
	}
	
	private List<Pair> extract(Test t) {
		if(t.isConjunctiveTest()) {
			List<Pair> strings = new LinkedList<Pair>();
            for(SimpleTest st : t.getConjunctiveTest().getSimpleTests())
            {
				strings.addAll(extract(st));
			}
			return strings;
		}
		else
			return extract(t.getSimpleTest());
	}
	
	private List<Pair> extract(SimpleTest simpleTest) {
		if(simpleTest.isDisjunctionTest()) {
			List<Pair> strings = new LinkedList<Pair>();
            for(Constant c : simpleTest.getDisjunctionTest().getConstants())
            {
                strings.add(c.toPair());
            }
			return strings;
		}
		else {
			SingleTest st = simpleTest.getRelationalTest().getSingleTest();
			List<Pair> strings = new LinkedList<Pair>();
			if(st.isConstant())
            {
				strings.add(st.getConstant().toPair());
            }
			else
            {
				strings.add(st.getVariable());
            }
			return strings;
		}
	}
	
	private List<Triple> extractTriples(VarAttrValMake vavm) {
		List<Triple> triples = new LinkedList<Triple>();
		for(AttributeValueMake avm : vavm.getAttributeValueMakes())
        {
			Pair variable = vavm.getVariable();
			Pair attributeMakes = null;
            for(RHSValue v : avm.getRHSValues())
            {
                if(attributeMakes == null)
                {
                    attributeMakes = extract(v);
                }
                else 
                {
                    Pair newAttributeMakes = extract(v);
                    Pair newVariable = getNextUnnamedVar();
                    triples.add(d_tripleFactory.createTriple(variable,attributeMakes,newVariable,false,false, false));
                    attributeMakes = newAttributeMakes;
                    variable = newVariable;
                }
            }
            for(ValueMake vm : avm.getValueMakes())
            {
                Pair value = extract(vm.getRHSValue());
                triples.add(d_tripleFactory.createTriple(variable,attributeMakes,value,false,false, false));
            }
		}
		return triples;
	}
	
	private Pair extract(RHSValue rhsValue) {
		if(rhsValue.isFunctionCall())
        {
            extractRhsFunctionVariables(rhsValue.getFunctionCall());
			return getNextUnnamedVar();
        }
		if(rhsValue.isVariable())
        {
			return rhsValue.getVariable();
        }
		return rhsValue.getConstant().toPair();
	}
    
    private void extractRhsFunctionVariables(FunctionCall f)
    {
        for(RHSValue value : f.getRHSValues())
        {
            if(value.isFunctionCall())
            {
                extractRhsFunctionVariables(value.getFunctionCall());
            }
            else if(value.isVariable())
            {
                d_rhsFunctionVariables.add(value.getVariable());
            }
        }
    }
	
	private Pair getNextUnnamedVar() {
		return new Pair("< " + d_currentUnnamedVar++ + ">",0, 0);
	}
	
	private void extractVariables() {
		for(Triple t : d_triples) 
        {
			d_variables.add(t.getVariable());
			if(TripleUtils.isVariable(t.getAttribute().getString()))
            {
				d_variables.add(t.getAttribute());
            }
			if(TripleUtils.isVariable(t.getValue().getString()))
            {
				d_variables.add(t.getValue());
            }
		}
	}
	
	private void extractStateVariables() {
        for(Triple t : d_triples) 
        {
			if(t.hasState())
            {
				d_stateVariables.add(t.getVariable());
            }
		}
	}
}
