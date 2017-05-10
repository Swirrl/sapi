package com.epimorphics.simpleAPI.eldalike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.simpleAPI.query.Query;
import com.epimorphics.simpleAPI.query.QueryBuilder;
import com.epimorphics.sparql.graphpatterns.Values;
import com.epimorphics.sparql.query.QueryShape;
import com.epimorphics.sparql.templates.Settings;
import com.epimorphics.sparql.terms.IsExpr;
import com.epimorphics.sparql.terms.URI;
import com.epimorphics.sparql.terms.Var;

public class EldaQueryBuilder implements QueryBuilder {

	protected Map<String, RDFNode> bindings = new HashMap<String, RDFNode>();
	
	protected EldaQueryBuilder copy() {
		EldaQueryBuilder c = new EldaQueryBuilder();
		c.bindings = new HashMap<String, RDFNode>();
		c.bindings.putAll(bindings);
		return c;
	}
	
	public QueryBuilder bind(String varname, RDFNode value) {
		EldaQueryBuilder it = copy();
		it.bindings.put(varname, value);
		return it;
	}

	public Query build() {
		return new EldaQuery(this);
	}
	
	public static class EldaQuery implements Query {
		
		protected final EldaQueryBuilder origin;
		
		protected List<Resource> items;
		
		public EldaQuery(EldaQueryBuilder origin) {
			this.origin = origin;
		}

		/**
		 	Return the ResultSet corresponding to the query that has been
		 	applied to the items delivered by the SELECT query.
		 * @param source 

		*/
		public ResultSet resultSet(SparqlSource source) {
			List<Resource> items = findItems(source);
			QueryShape s = new QueryShape();
			
			List<Var> vars = new ArrayList<Var>();
			List<IsExpr> data = new ArrayList<IsExpr>();
			
			vars.add(new Var("?item"));
			
			for (Resource i: items) {
				data.add(new URI(i.getURI()));
			}
			
			s.addLaterPattern(new Values(vars, data));
			
			String queryString = s.toSparqlConstruct(new Settings());
			
			Graph constructed = source.construct(queryString);
			
			ModelFactory
				.createModelForGraph(constructed)
				.write(System.err, "TTL");
			
			return null;
		}

		protected List<Resource> findItems(SparqlSource source) {
			if (items == null) {
				String queryString = composeQuery();
				List<Resource> selected = source.selectVar(queryString, "?item", Resource.class);
				items = selected;
			}
			return items;
		}

		protected String composeQuery() {
			// TODO define and create query here
			return "SELECT ?item WHERE {}";
		}

		public String display() {
			// TODO sore information here
			return "EldaQuery ";
		}
	}



}
