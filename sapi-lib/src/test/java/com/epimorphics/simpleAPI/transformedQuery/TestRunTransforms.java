/*                                                                                                                            
    LICENCE summary to go here.                                                                                        
    
    (c) Copyright 2014 Epimorphics Limited
*/

package com.epimorphics.simpleAPI.transformedQuery;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.simpleAPI.core.API;
import com.epimorphics.simpleAPI.endpoints.EndpointSpec;
import com.epimorphics.simpleAPI.query.DataSource;
import com.epimorphics.simpleAPI.query.ListQuery;
import com.epimorphics.simpleAPI.query.ListQueryBuilder;
import com.epimorphics.simpleAPI.query.QueryBuilder;
import com.epimorphics.simpleAPI.query.impl.SparqlQueryBuilder;
import com.epimorphics.simpleAPI.requests.Call;
import com.epimorphics.simpleAPI.results.Result;
import com.epimorphics.simpleAPI.results.ResultStream;
import com.epimorphics.sparql.geo.GeoQuery;
import com.epimorphics.sparql.terms.Var;
import com.epimorphics.webapi.test.MockUriInfo;

public class TestRunTransforms {
	
	App app;
	API api;
    DataSource source;
	    
	@Before	public void setUP() throws IOException {
		app = new App("test", new File("src/test/testCases/runTransforms/WEB-INF/app.conf"));
	    api = app.getA(API.class);
	    app.startup();
        source = app.getA(DataSource.class);
	}
	
	@Test public void testSetup() {
	}
	
	@Test public void testEndToEnd() {
        EndpointSpec endpoint = api.getSpec("run-transform-test");
                    
        QueryBuilder baseQB = api.getCall("run-transform-test", new MockUriInfo("test"), null).getQueryBuilder();
        
        ListQueryBuilder geoQB = ((SparqlQueryBuilder) baseQB); 
		
        GeoQuery x = new GeoQuery(new Var("id"), "withinCircle", 364018.0, 190467.0, 12000.0);
        
        ListQuery query = geoQB.geoQuery(x).build();
		
        // String queryString = query.toString();
        // System.err.println(">> query:\n" + queryString);
		
		ResultStream stream = source.query(query, new Call(endpoint, null));
		
		Set<String> obtained = new HashSet<String>();
		
		while (stream.hasNext()) {
			Result next = stream.next();
			obtained.add(next.asJson().get("@id").getAsString().value());
		}
		
		Set<String> expected = new HashSet<String>();
		expected.add("http://environment.data.gov.uk/public-register/waste-carriers-brokers/id/CBDU53093/site/0/location/0");
		expected.add("http://environment.data.gov.uk/public-register/waste-carriers-brokers/id/CBDU53225/site/0/location/0");
		expected.add("http://environment.data.gov.uk/public-register/waste-carriers-brokers/id/CBDU53548/site/0/location/0");
		expected.add("http://environment.data.gov.uk/public-register/waste-carriers-brokers/id/CBDU52542/site/0/location/0");
		
		assertEquals(expected, obtained);
		
//        assertContains( queryString, "?id <http://jena.apache.org/spatial#withinCircle> (60.1 19.2 11.0) ." );
        
    }
}
