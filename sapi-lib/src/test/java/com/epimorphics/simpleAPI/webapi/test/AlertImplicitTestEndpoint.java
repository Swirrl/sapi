/******************************************************************
 * File:        AlertImplicitTestEndpoint.java
 * Created by:  Dave Reynolds
 * Created on:  10 Jan 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.webapi.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.epimorphics.json.JSONWritable;
import com.epimorphics.simpleAPI.webapi.EndpointsBase;

@Path("implicitQueryTest")
public class AlertImplicitTestEndpoint extends EndpointsBase {

    @Produces({MediaType.APPLICATION_JSON})
    @GET
    public JSONWritable getAlertTest( ) {
        return listItems("alertTestImplicitQuery", getRequestWithParms());
    }
    
}