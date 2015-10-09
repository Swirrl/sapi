/******************************************************************
 * File:        QueryBuilder.java
 * Created by:  Dave Reynolds
 * Created on:  30 Sep 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.query;

import java.util.Collection;

import org.apache.jena.rdf.model.RDFNode;

/**
 * Represents a generic list query (might be SPARQL or noSQL) that can
 * be modified and extended.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface ListQueryBuilder extends QueryBuilder {

    /**
     * Add an equality filter constraint to the query
     */
    public ListQueryBuilder filter(String shortname, RDFNode value);

    /**
     * Add a one-of filter constraint to the query
     */
    public ListQueryBuilder filter(String shortname, Collection<RDFNode> values);
    
    // TODO more generalized filters?
    
    /**
     * Add a sort directive to the query
     */
    public ListQueryBuilder sort(String shortname, boolean down);
    
    /**
     * Set a paging window on the query results
     */
    public ListQueryBuilder limit(long limit, long offset);
    
    /**
     * Finalize the query, in the case of SPARQL this will include
     * setting prefix bindings.
     */
    public ListQuery build();
}
