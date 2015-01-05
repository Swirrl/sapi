/******************************************************************
 * File:        APIConfig.java
 * Created by:  Dave Reynolds
 * Created on:  5 Jan 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.core;

import org.apache.jena.atlas.json.JsonObject;

/**
 * Encapsulates the specification for an API endpoint. The specification includes information on:
 * <ul>
 *   <li>the query to run (an explicit query template or an implicit query built from the JSON map</li>
 *   <li>JSON map guiding how the query result should be mapped to JSON (accessed via sub-interfaces)</li>
 *   <li>metadata (in the form of a JSON Object) which might be used in documenting the API endpoint</li>
 * </ul>
 * <p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface EndpointSpec extends NodeWriterPolicy {

    /**
     * Return the parent API associated with this endpoint 
     */
    public API getAPI();

    /**
     * Return a name for this endpoint, the name is used to retrieve the spec from the parent API, 
     * it is not directly visible to API users
     */
    public String getName();
   
    /**
     * Return metadata on the query (which can include all the original configuration properties).
     */
    public JsonObject getMetadata();
    
    /**
     * Return the query after instantiating it according to the supplied
     * request parameters.
     */
    public String getQuery( RequestParameters request );
    
}
