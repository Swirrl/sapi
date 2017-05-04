package com.epimorphics.simpleAPI.eldalike;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.RDFNode;

import com.epimorphics.simpleAPI.query.Query;
import com.epimorphics.simpleAPI.query.QueryBuilder;

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
		
		public EldaQuery(EldaQueryBuilder origin) {
			this.origin = origin;
		}
	}



}
