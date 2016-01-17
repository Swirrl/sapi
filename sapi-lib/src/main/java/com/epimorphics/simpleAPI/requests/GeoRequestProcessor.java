/******************************************************************
 * File:        GeoRequestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  17 Jan 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.requests;

import com.epimorphics.geo.LatLonE;
import com.epimorphics.geo.OsGridRef;
import com.epimorphics.simpleAPI.endpoints.EndpointSpec;
import com.epimorphics.simpleAPI.query.ListQueryBuilder;
import com.epimorphics.simpleAPI.query.impl.SparqlQueryBuilder;
import com.epimorphics.sparql.geo.GeoQuery;
import com.epimorphics.sparql.terms.Var;

/**
 * Handles requests of the form:
 * <pre>
 *   ?lat={x} &long={y} &dist={d}
 * </pre>
 * or
 * <pre>
 *   ?easting={x} &northing={y} &dist={d}
 * </pre>
  */
// TODO add support for configuration per-endpoint - id, box/circle, distance
public class GeoRequestProcessor implements RequestProcessor {
    public static final String P_LAT      = "lat";
    public static final String P_LONG     = "long";
    public static final String P_DIST     = "dist";
    public static final String P_EASTING  = "easting";
    public static final String P_NORTHING = "northing";
    
    public static final String DEFAULT_ID  = "id";
    public static final String DEFAULT_ALG = "withinBox";
    
    @Override
    public ListQueryBuilder process(Request request, ListQueryBuilder builder, EndpointSpec spec) {
        if (builder instanceof SparqlQueryBuilder) {
            GeoQuery gq = null;
            if (request.hasParameter(P_LAT) && request.hasParameter(P_LONG) && request.hasParameter(P_DIST)) {
                LatLonE point = new LatLonE(request.getAsDouble(P_LAT), request.getAsDouble(P_LONG));
                OsGridRef osPoint = OsGridRef.fromLatLon(point);
                gq = new GeoQuery(new Var(DEFAULT_ID), DEFAULT_ALG, 
                        osPoint.getEasting(), osPoint.getNorthing(), getDistance(request));
                request.consume(P_LAT);
                request.consume(P_LONG);
                request.consume(P_DIST);
            } else if (request.hasParameter(P_EASTING) && request.hasParameter(P_NORTHING) && request.hasParameter(P_DIST)) {
                gq = new GeoQuery(new Var(DEFAULT_ID), DEFAULT_ALG, 
                        request.getAsLong(P_EASTING), request.getAsLong(P_NORTHING),  getDistance(request));
                request.consume(P_EASTING);
                request.consume(P_NORTHING);
                request.consume(P_DIST);
            }
            if (gq != null) {
                return ((SparqlQueryBuilder)builder).geoQuery(gq);
            }
        }
        return builder;
    }
    
    private long getDistance(Request request) {
        return (long) (request.getAsNumber(P_DIST).doubleValue() * 1000.0);
    }

}