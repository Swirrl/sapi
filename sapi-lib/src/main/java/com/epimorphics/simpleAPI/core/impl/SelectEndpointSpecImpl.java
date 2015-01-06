/******************************************************************
 * File:        SelectEndpointSpecImpl.java
 * Created by:  Dave Reynolds
 * Created on:  5 Jan 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.core.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.json.JSFullWriter;
import com.epimorphics.json.JSONWritable;
import com.epimorphics.simpleAPI.core.API;
import com.epimorphics.simpleAPI.core.RequestParameters;
import com.epimorphics.simpleAPI.core.SelectEndpointSpec;
import com.epimorphics.simpleAPI.writers.JsonWriterUtil;
import com.epimorphics.simpleAPI.writers.KeyValueSetStream;
import com.hp.hpl.jena.query.ResultSet;

public class SelectEndpointSpecImpl extends EndpointSpecBase implements SelectEndpointSpec {

    public SelectEndpointSpecImpl(API api, JsonObject config) {
        super(api, config);
    }

    @Override
    public String getQuery(RequestParameters request) {
        if (query == null) {
            // TODO construct implicit query from JSON mapping
        }
        return request.bindQueryParams(query);
    }

    @Override
    public JSONWritable getWriter(KeyValueSetStream results) {
        return new Writer(results);
    }

    @Override
    public JSONWritable getWriter(ResultSet results) {
        return new Writer(results);
    }
    
    public class Writer implements JSONWritable {
        KeyValueSetStream values;
        
        public Writer(KeyValueSetStream values) {
            this.values = values;
        }
        
        public Writer(ResultSet results) {
            this.values = new KeyValueSetStream(results);
        }
        
        @Override
        public void writeTo(JSFullWriter out) {
            out.startObject();
            api.writeMetadata(out);
            out.key( getItemName() );
            out.startArray();
            while (values.hasNext()) {
                out.arrayElementProcess();
                JsonWriterUtil.writeKeyValues(map, values.next(), out);
            }
            out.finishArray();
            out.finishObject();
        }

    }    

}
