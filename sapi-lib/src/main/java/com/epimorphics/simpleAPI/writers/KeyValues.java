/******************************************************************
 * File:        KeyValues.java
 * Created by:  Dave Reynolds
 * Created on:  9 Dec 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.writers;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Represents the value(s) of a key in an output stream. The key normally corresponds
 * to the JSON name in an output JSON structure and the variable name in any supplying SPARQL 
 * query solution row. Each value may be an RDFNode or a nested ValueSet (when the
 * mapping includes nested values).
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class KeyValues implements Comparable<KeyValues>{
    protected String key;
    protected List<Object> values = new ArrayList<Object>();
    
    public KeyValues(String key) {
        this.key = key;
    }
    
    public KeyValues(String key, Object value) {
        this.key = key;
        values.add(value);
    }
    
    public String getKey() {
        return key;
    }
    
    public List<Object> values() {
        return values;
    }
    
    public List<Object> getValues() {
        return values;
    }
    
    public Object getValue() {
        if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }
    
    public void add(Object value) {
        if ( ! values.contains(value) ) {
            values.add(value);
        }
    }
    
    /**
     * If this contains a ValueSet with the given ID then return it,
     * otherwise return null;
     */
    public ValueSet findValueSet(RDFNode id) {
        for (Object value : values) {
            if (value instanceof ValueSet) {
                ValueSet vs = (ValueSet)value;
                if ( vs.getId().equals(id) ) {
                    return vs;
                }
            }
        }
        return null;
    }
    
    /**
     * Find or create a ValueSet entry for the given ID
     */
    public ValueSet makeValueSet(RDFNode id) {
        ValueSet vs = findValueSet(id);
        if (vs == null) {
            vs = new ValueSet(id);
            values.add(vs);
        }
        return vs;
    }

    @Override
    public int compareTo(KeyValues o) {
        return key.compareTo(o.key);
    }
    

    @Override
    public String toString() {
        return key + "=" + values;
    }
}
