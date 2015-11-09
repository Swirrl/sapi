/******************************************************************
 * File:        DescribeQueryBuilder.java
 * Created by:  Dave Reynolds
 * Created on:  8 Oct 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.query.impl;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.PrefixMapping;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.simpleAPI.query.ItemQuery;
import com.epimorphics.simpleAPI.query.QueryBuilder;
import com.epimorphics.sparql.graphpatterns.Bind;
import com.epimorphics.sparql.query.Query;
import com.epimorphics.sparql.templates.Settings;
import com.epimorphics.sparql.terms.Literal;
import com.epimorphics.sparql.terms.URI;
import com.epimorphics.sparql.terms.Var;
import com.epimorphics.util.PrefixUtils;

public class DescribeQueryBuilder implements QueryBuilder {
    protected Query query;
    protected PrefixMapping prefixes;
    
    public DescribeQueryBuilder(Query query) {
        this.query = query;
    }
    
    public DescribeQueryBuilder(Query query, PrefixMapping prefixes) {
        this.prefixes = prefixes;
        this.query = query;
    }

    @Override public QueryBuilder bind(String varname, RDFNode value) {
        return new DescribeQueryBuilder( bindQueryParam(query, varname, value), prefixes );
    }

    @Override public ItemQuery build() {
    	String queryString = query.toSparqlDescribe(new Settings());
        return new SparqlDescribeQuery
        	( prefixes == null 
        	? queryString 
        	: PrefixUtils.expandQuery(queryString, prefixes) )
        	;
    }
    
    // TODO move this to somewhere more logical and reusable
    /**
     * Bind a variable in a query by syntactic substitution
     */
    public static Query bindQueryParam(Query query, String var, Object value) {
        String subs = QueryUtil.asSPARQLValue( value );
        Literal val = new Literal(subs, new URI("xsd:string"), "");
        Query q = query.copy().addLaterPattern(new Bind(val, new Var(var)));
        return q;
    }
    protected static final String MARKER="?ILLEGAL-VAR";
}
