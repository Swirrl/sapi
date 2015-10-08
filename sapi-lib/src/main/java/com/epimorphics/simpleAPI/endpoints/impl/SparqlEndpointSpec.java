/******************************************************************
 * File:        EndpointSpec.java
 * Created by:  Dave Reynolds
 * Created on:  27 Sep 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.endpoints.impl;

import com.epimorphics.simpleAPI.core.API;
import com.epimorphics.simpleAPI.endpoints.EndpointSpec;
import com.epimorphics.simpleAPI.query.QueryBuilder;
import com.epimorphics.simpleAPI.query.impl.DescribeQueryBuilder;
import com.epimorphics.simpleAPI.views.ViewMap;

/**
 * Encapsulates the specification of a single endpoint.
 */
public class SparqlEndpointSpec extends EndpointSpecBase implements EndpointSpec {
    protected String baseQuery;
    
    public SparqlEndpointSpec(API api) {
        super(api);
    }


    @Override
    public QueryBuilder getQueryBuilder() {
        return getQueryBuilder(DEFAULT_VIEWNAME);
    }

    @Override
    public QueryBuilder getQueryBuilder(String viewname) {
        ViewMap view = getView(viewname);
        if (baseQuery == null) {
            if (view == null) {
                baseQuery = "DESCRIBE ?id";
            } else {
                // TODO generate base describe from view
            }
        }
        return new DescribeQueryBuilder(baseQuery, getPrefixes());
    }

    public void setBaseQuery(String query) {
        this.baseQuery = query;
    }

}