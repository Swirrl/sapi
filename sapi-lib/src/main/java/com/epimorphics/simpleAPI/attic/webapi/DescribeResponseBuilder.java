/******************************************************************
 * File:        DescribeResponseBuilder.java
 * Created by:  Dave Reynolds
 * Created on:  9 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.attic.webapi;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.epimorphics.appbase.data.WNode;
import com.epimorphics.simpleAPI.attic.core.DescribeEndpointSpec;
import com.epimorphics.simpleAPI.attic.core.RequestParameters;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.DCTerms;

public class DescribeResponseBuilder extends EPResponseBuilder {
    protected Model model;
    protected Resource resource;
    protected DescribeEndpointSpec spec;

    public DescribeResponseBuilder(ServletContext context, UriInfo uriInfo) {
        super(context, uriInfo);
    }

    /**
     * Describe a resource specified by its URI
     */
    public DescribeResponseBuilder describeByURI(String uri) {
        model = ModelFactory.createModelForGraph( getSource().describe( "DESCRIBE <" + uri + ">" ) );
        resource = model.getResource(uri);
        return this;
    }

    /**
     * Describe the requested URI using the default describe specification
     */
    public DescribeResponseBuilder describe() {
        describe( getAPI().getDefaultDescribe() );
        return this;
    }
    
    /**
     * Describe the requested URI using the named describe specification
     */
    public DescribeResponseBuilder describe(String specname) {
        describe( getAPI().getDescribeSpec(specname) );
        return this;
    }
    
    /**
     * Describe the requested URI using the given describe specification
     */
    public DescribeResponseBuilder describe(DescribeEndpointSpec spec) {
        this.spec = spec;
        RequestParameters rp = getRequest();
        model = ModelFactory.createModelForGraph( getSource().describe( spec.getQuery(rp) ) );
        PrefixMapping prefixes = getAPI().getApp().getPrefixes();
        if (prefixes != null) {
            model.setNsPrefixes(prefixes);
        }
        resource = model.getResource( getBaseRequestedURI() );
        return this;
    }
    
    /**
     * Describe the given resource (which has already been fetched)
     */
    public DescribeResponseBuilder describe(Resource resource) {
        this.resource = resource;
        this.model = resource.getModel();
        return this;
    }
    
    /**
     * Describe the given resource (which has already been fetched)
     * using the given JSON mapping
     */
    public DescribeResponseBuilder describe(String specname, Resource resource) {
        this.spec = getAPI().getDescribeSpec(specname);
        return describe(resource);
    }
    
    @Override
    public Object getEntity() {
        if (model == null) {
            describe();
        }
        if (model.size() == 0) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        switch (format) {
        case rdf:
            Resource meta = getAPI().addRDFMetadata(resource);
            for (String format : spec.getFormats(getRequestedURI(), "noskip")) {
                meta.addProperty(DCTerms.hasFormat, ResourceFactory.createResource(format));
            }
            return model;
        case json:
            if (spec == null) spec = getAPI().getDefaultDescribe();
            return spec.getWriter(resource, getRequestedURI());
        case html:
            return wrap( resource );
        case csv:
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        default:
            // can't happen
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    protected WNode wrap(Resource resource) {
        return new WNode(getWSource(), resource, true);
    }

    @Override
    public String getEntityVelocityName() {
        return "resource";
    }

    public Resource getResource() {
        return resource;
    }
}