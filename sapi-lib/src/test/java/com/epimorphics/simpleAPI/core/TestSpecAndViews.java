/******************************************************************
 * File:        TestEndpointSpec.java
 * Created by:  Dave Reynolds
 * Created on:  28 Sep 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.simpleAPI.endpoints.impl.SparqlListEndpointSpec;
import com.epimorphics.simpleAPI.query.Query;
import com.epimorphics.simpleAPI.query.impl.SparqlSelectQuery;
import com.epimorphics.simpleAPI.views.ViewEntry;
import com.epimorphics.simpleAPI.views.ViewMap;
import com.epimorphics.vocabs.SKOS;

public class TestSpecAndViews {
    public static final String RT = "http://environment.data.gov.uk/flood-monitoring/def/core/";
    
    App app;
    API api;
    
    @Before
    public void setUP() throws IOException {
        app = new App("test", new File("src/test/testCases/baseEPTest/app.conf"));
        api = app.getA(API.class);
        app.startup();
    }

    @Test
    public void testEndpointsExist() {
        assertNotNull( api.getSpec("listTest") );
        SparqlListEndpointSpec spec = (SparqlListEndpointSpec) api.getSpec("listTest");
        assertEquals(10, spec.getSoftLimit().longValue());
        assertEquals(100, spec.getHardLimit().longValue());
        Query query = spec.getQueryBuilder().build();
        String qStr = ((SparqlSelectQuery)query).getQuery();        
        assertTrue(  qStr.contains("?id a rt:FloodAlertOrWarning") );
        assertTrue( qStr.contains("PREFIX rt: <http://environment.data.gov.uk/flood-monitoring/def/core/>") );
        
    }
    
    @Test
    public void testViewLoading() {
        assertNull( api.getSpec("otherTest") );
        ViewMap view = api.getView("view-test"); 
        assertNotNull( view );
        
        assertEquals( "view-test", view.getName() );
        List<ViewEntry> children = view.getTree().getChildren();
        assertEquals(4, children.size());
        checkEntry(children.get(0), "severity",  RT + "severity",  false, false, false,  true);
        checkEntry(children.get(1), "message",   RT + "message",   false,  true, false,  true);
        checkEntry(children.get(2), "floodArea", RT + "floodArea",  true, false, false,  true);
        checkEntry(children.get(3), "test",      RT + "test",      false, false,  true, false);
        
        // nested block
        children = children.get(2).getNested().getChildren();
        checkEntry(children.get(0), "notation",  SKOS.notation.getURI(),  false, false, false, true);
        checkEntry(children.get(1), "county",    RT + "county",      false, false, false, true);
    }
    
    private void checkEntry(ViewEntry entry, String json, String prop, boolean nested, boolean optional, boolean multi, boolean filterable) {
        assertEquals(json, entry.getJsonName());
        assertEquals(prop, entry.getProperty());
        assertEquals(nested, entry.isNested());
        assertEquals(optional, entry.isOptional());
        assertEquals(multi, entry.isMultivalued());
        assertEquals(filterable, entry.isFilterable());
    }
    
    @Test
    public void testViewAccess() {
        ViewMap view = api.getView("varnameTest");
        assertNotNull(view);
        
        assertEquals("foo", view.asVariableName("foo"));
        assertEquals("foo_bar", view.asVariableName("bar"));
        assertEquals("foo_bar_test", view.asVariableName("test"));
        assertEquals("foo_baz_label", view.asVariableName("foo.baz.label"));
        assertEquals("label", view.asVariableName("label"));
        assertEquals("foo_fu__bar", view.asVariableName("fu_bar"));
        assertEquals("foo_fu__bar", view.asVariableName("foo.fu_bar"));
        
        assertEquals("foo.fu_bar", view.getTree().pathTo("fu_bar").asDotted());
        
        assertEquals("foo", view.pathTo("foo").asDotted());
        assertEquals("foo.bar", view.pathTo("bar").asDotted());
        assertNull( view.pathTo("notthere") );
        
        assertEquals(RT + "foo", view.findEntry("foo").getProperty());
        assertEquals(RT + "bar", view.findEntry("bar").getProperty());
        assertEquals(RT + "fu_bar", view.findEntry("foo.fu_bar").getProperty());

        simpleCheckEntry(view.findEntryByURI(RT + "foo"), "foo", RT + "foo", true);
        simpleCheckEntry(view.findEntryByURI(RT + "bar"), "bar", RT + "bar", true);
    }
    
    @Test
    public void testEndointQueryRender() {
        // TODO sensitive to details of the string bashing
        ViewMap view = api.getSpec("describeTest2").getView();
        String query = view.asDescribe();
        assertTrue( query.contains("{?id <http://www.w3.org/2004/02/skos/core#narrower> ?narrower }") );
        assertTrue( query.contains("OPTIONAL {?id <http://www.w3.org/2004/02/skos/core#related> ?related }") );
        assertTrue( query.contains("{?related <http://www.w3.org/2004/02/skos/core#related> ?related_related }") );
        String describeLine = query.split("\\{")[0];
        assertTrue( describeLine.contains("DESCRIBE") );
        assertTrue( describeLine.contains("?id") );
        assertTrue( describeLine.contains("?narrower") );
        assertTrue( describeLine.contains("?related") );
        assertTrue( describeLine.contains("?related_related") );
    }
    
    private void simpleCheckEntry(ViewEntry entry, String json, String prop, boolean nested) {
        assertEquals(json, entry.getJsonName());
        assertEquals(prop, entry.getProperty());
        assertEquals(nested, entry.isNested());
    }
}
