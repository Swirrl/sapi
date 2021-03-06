/******************************************************************
 * File:        API.java
 * Created by:  Dave Reynolds
 * Created on:  27 Sep 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.simpleAPI.core;

import static com.epimorphics.simpleAPI.core.ConfigConstants.DEFAULT_MODEL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.GenericConfig;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.monitor.ConfigInstance;
import com.epimorphics.json.JSFullWriter;
import com.epimorphics.simpleAPI.endpoints.EndpointSpec;
import com.epimorphics.simpleAPI.query.DataSource;
import com.epimorphics.simpleAPI.requests.Call;
import com.epimorphics.simpleAPI.requests.Request;
import com.epimorphics.simpleAPI.sapi2.BaseEngine;
import com.epimorphics.simpleAPI.sapi2.Sapi2BaseEndpointSpec;
import com.epimorphics.simpleAPI.util.LastModified;
import com.epimorphics.simpleAPI.views.ModelSpec;
import com.epimorphics.simpleAPI.views.PropertySpec;
import com.epimorphics.simpleAPI.views.ViewMap;
import com.epimorphics.sparql.terms.URI;
import com.epimorphics.util.NameUtils;

/**
 * This is the primary configuration component for sapi. It provides:
 * <ul>
 *   <li>Set of API specs which can be loaded once or dynamically monitored.</li>
 *   <li>Metadata which can be included in the json serialization (version, licence, documentation link etc).</li>
 *   <li>Base URI which can be used to convert incoming URI requests to RDF queries.</li>
 *   <li>An associated Sparql (or other) source which will be queried.</li>
 *   <li>An optional set of defauult shortname/URI mappings to use as a fall back when serializing an RDF instance.</li>
 * </ul>
 */
public class API extends ComponentBase implements Startup {
    public static final String DEFAULT_VIEWNAME = "defaultView";
    
    protected DataSource source;
    
    protected String baseURI = "http://localhost/";
    
    protected String documentation;
    protected String licence;
    protected String version = "0.1";
    protected String publisher;
    protected String contextURL;
    protected String comment;
    protected int  maxAge = 60;
    protected SpecMonitor monitor;
    protected boolean htmlNonDefault = false;
    protected boolean htmlPreferred = false;;
    protected String defaultItemTemplate;
    protected String defaultListTemplate;
    
    protected boolean showSimpleLinks = false;
    protected boolean itemEndpointsReturnArrays = true;
    protected boolean showLang = false;
    protected String showOnlyLang;
    protected boolean fullPathsInCSVHeaders = false;
    protected boolean generateCSVfilenames = false;
    
    protected GenericConfig configExtensions = new GenericConfig();
    
    protected LastModified timestampService;
    
    protected Engine defaultEngine = new BaseEngine();
    
    // TODO review the supported formats
    public static final String[] supportedFormats = new String[]{"json", "csv", "rdf", "ttl"};
    
    /**
     * Return a global default API configuration called "api" if it exists.
     * Useful for simple deployments where a single configuration suffices.
     */
    public static API get() {
        return AppConfig.getApp().getComponentAs("api", API.class);
    }

    // ---- Metadata support --------------------------------------------
    
    public void startMetadata(JSFullWriter out) {
        condOut(out, "@context", contextURL);
        out.key("meta");
        out.startObject();
        condOut(out, "publisher", publisher);
        condOut(out, "licence",   licence);
        condOut(out, "documentation", documentation);
        condOut(out, "version", version);
        condOut(out, "comment", comment);
    }    
    
    public void writeFormats(JSFullWriter out, String requestURI, String... skipFormat) {
        boolean started = false;
        for (String format : getFormats(requestURI, skipFormat)) {
            if (!started) {
                out.key("hasFormat");
                out.startArray();
                started = true;
            }
            out.arrayElement(format);
        }
        if (started) {
            out.finishArray();
        }
    }
    
    protected List<String> getFormats(String requestURI, String... skipFormats) {
        List<String> formats = new ArrayList<>();
        Matcher m = URIPAT.matcher(requestURI);
        String base = m.matches() ? m.group(1) : requestURI;
        String rest = (m.matches() && m.group(3) != null) ? m.group(3) : "";
        for (String format : supportedFormats) {
            boolean skip = false;
            for ( String skipFormat : skipFormats ) { 
                if (skipFormat.equals(format)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                String url = base + "." + format + rest;
                formats.add(url);
            }
        }
        if ( isHtmlSupported() ) {
            formats.add(base + ".html" + rest);
        }
        return formats;
    }

    public void writeMetadata(JSFullWriter out) {
        startMetadata(out);
        finishMetadata(out);
    }
    
    public void finishMetadata(JSFullWriter out) {
        out.finishObject();
    }
    
    /**
     * Inject a page description into a description model
     * @param meta the resource to which the metadata should be attached
     * @param requestedURI the full URI of the request, used for generating hasFormat information
     * @param skipFormat the extension name of a format to omit from the hasFormat list
     */
    public Resource addRDFMetadata(Resource meta, String requestedURI, String... skipFormat) {
        condAddProperty(meta, DCTerms.publisher, publisher);
        condAddProperty(meta, DCTerms.license, licence);
        if (documentation != null) {
            meta.addProperty(RDFS.seeAlso, ResourceFactory.createResource(documentation));
        }
        condAddProperty(meta, OWL.versionInfo, version);
        condAddProperty(meta, RDFS.comment, comment);
        for (String format : getFormats(requestedURI, skipFormat)) {
            meta.addProperty(DCTerms.hasFormat, meta.getModel().createResource(format));
        }
        return meta;
    }
    
    private void condAddProperty(Resource meta, Property prop, String value) {
        if (value != null) {
            meta.addProperty(prop, value);
        }
    }
    
    // ---- Access to configurations ------------------------------------

    public EndpointSpec getSpec(String name) {
        if (monitor != null){
            ConfigInstance item = monitor.get(name);
            if (item instanceof EndpointSpec) {
                return (EndpointSpec) item;
            }
        }
        return null;
    }
    
    public List<Sapi2BaseEndpointSpec> listSpecs() {
        return listConfigs(Sapi2BaseEndpointSpec.class);
    }
    
    @SuppressWarnings("unchecked")
    public <T> List<T> listConfigs(Class<T> cls) {
        List<T> list = new ArrayList<>();
        if (monitor != null){
            for (ConfigInstance ci : monitor.getEntries()) {
                if (cls.isInstance(ci)) {
                    list.add( (T) ci);
                }
            }
        }
        return list;
    }

    public ViewMap getView(String name) {
        if (monitor != null){
            ConfigInstance item = monitor.get(name);
            if (item instanceof ViewMap) {
                return (ViewMap) item;
            }
        }
        return null;
    }
     
    public List<ViewMap> listViews() {
        return listConfigs(ViewMap.class);
    }

    /**
     * Return the default specification for how to render a given property.
     */
    public PropertySpec getDefaultViewForURI(String uri) {
        ViewMap defview = getView(DEFAULT_VIEWNAME);
        if (defview != null) {
            return defview.findEntryByURI(uri);
        } else {
            return new PropertySpec(new URI(uri));
        }
    }

    /**
     * Return the default specification for how to render a given short name
     */
    public PropertySpec getDefaultViewFor(String name) {
        ViewMap defview = getView(DEFAULT_VIEWNAME);
        if (defview != null) {
            return defview.findEntry(name);
        } else {
            return new PropertySpec(name, null);
        }
    }
    
    public PrefixMapping getPrefixes() {
        if (getApp() == null) {
            // Must be in a testing setup
            return new PrefixMappingImpl();
        } else {
            return getApp().getPrefixes();
        }
    }
    
    public ModelSpec getModel(String name) {
        if (monitor != null){
            ConfigInstance item = monitor.get(name);
            if (item instanceof ModelSpec) {
                return (ModelSpec) item;
            }
        }
        return null;
    }
    
    /**
     * Get the default ModelSpec which is either the only one or one explicitly
     * called "defaultModel"
     */
    public ModelSpec getModel() {
        if (monitor != null) {
            ModelSpec model = null;
            ModelSpec defModel = null;
            boolean singleModel = true;
            
            for (ModelSpec m : listConfigs(ModelSpec.class)) {
                if (m != null) {
                    singleModel = false;
                } else {
                    model = m;
                }
                if (DEFAULT_MODEL.equals(m.getName())) {
                    defModel = m;
                }
            }
            if (defModel != null) {
                return defModel;
            } else if (singleModel) {
                return model;
            }
        }
        return null;
    }
 
    /**
     * Set up a call based on a simple GET request. Looks up the endpoint in the 
     * register of templates and extracts the request parameters.
     * @throws NotFoundException if no endpoint matches 
     */
    public Call getCall(UriInfo uriInfo, HttpServletRequest servletRequest) {
        return getCall(uriInfo, Request.from(this, uriInfo, servletRequest));
    }
    
    /**
     * Set up a call based on a simple GET request. Looks up the endpoint in the 
     * register of templates and extracts the request parameters.
     * @throws NotFoundException if no endpoint matches 
     */
    public Call getCall(UriInfo uriInfo, Request request) {
        return monitor.getCall(uriInfo, request);
    }
    
    /**
     * Set up a call based on a POST request. Looks up the endpoint in the 
     * register of templates and extracts the request parameters.
     * @throws NotFoundException if no endpoint matches 
     */
    public Call getCall(UriInfo uriInfo, HttpServletRequest servletRequest, String requestBody) {
        return getCall(uriInfo, Request.from(this, uriInfo, servletRequest, requestBody));
    }
    
    /**
     * Set up a call based on a simple GET request 
     */
    public Call getCall(String endpoint, UriInfo uriInfo, HttpServletRequest servletRequest) {
        return getCall(endpoint, Request.from(this, uriInfo, servletRequest));
    }
    
    /**
     * Set up a call based on a simple GET request 
     */
    public Call getCall(String endpoint, Request request) {
        return new Call(this, endpoint, request);
    }
    
    /**
     * Set up a call based on a POST request using the named endpoint specification
     */
    public Call getCall(String endpoint, UriInfo uriInfo, HttpServletRequest servletRequest, String requestBody) {
        return getCall(endpoint, Request.from(this, uriInfo, servletRequest, requestBody));
    }

    public Engine getDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(Engine defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public Engine getEngine(String name) {
        return getApp().getComponentAs(name, Engine.class);
    }
    
    // ---- Monitor configurations ------------------------------------
    
    /**
     * Set a directory of endpoint specifications to load. 
     */
    public void setEndpointSpecDir(String dir) {
        monitor = new SpecMonitor(this);
        monitor.setDirectory(dir);
        monitor.setUseWatcher(true);
    }   
    
    @Override
    public void startup(App app) {
        super.startup(app);
        if (monitor != null) {
            monitor.startup(app);
        }
    }
    
    // ---- Settings/getters --------------------------------------------
    
    /**
     * Set the source which will be queried
     */
    public void setSource(DataSource source) {
        this.source = source;
    }
    
    /**
     * Set the baseURL which this instance is supposed to be serving from
     */
    public void setBaseURI(String baseURL) {
        this.baseURI = NameUtils.ensureLastSlash( baseURL );
    }


    public DataSource getSource() {
        return source;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String license) {
        this.licence = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String versionNumber) {
        this.version = versionNumber;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    public String getContextURL() {
        return contextURL;
    }

    public void setContextURL(String contextURL) {
        this.contextURL = contextURL;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    
    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = (int)maxAge;
    }

    public void setShowLangTag(boolean showLang) {
        this.showLang = showLang;
    }    
    
    public boolean isShowLangTag() {
        return showLang; 
    }

    public void setShowOnlyLang(String lang) {
        this.showOnlyLang = lang;
    }    
    
    public String getShowOnlyLang() {
        return showOnlyLang; 
    }
    
    public boolean isFullPathsInCSVHeaders() {
        return fullPathsInCSVHeaders;
    }

    /**
     * Set to true to force CSV output headers to show the full nesting structure
     * of the columns (dotted paths)
     */
    public void setFullPathsInCSVHeaders(boolean fullPathsInCSVHeaders) {
        this.fullPathsInCSVHeaders = fullPathsInCSVHeaders;
    }
    
    public String getDefaultItemTemplate() {
        return defaultItemTemplate;
    }

    public void setDefaultItemTemplate(String defaultItemTemplate) {
        this.defaultItemTemplate = defaultItemTemplate;
    }

    public String getDefaultListTemplate() {
        return defaultListTemplate;
    }

    public void setDefaultListTemplate(String defaultListTemplate) {
        this.defaultListTemplate = defaultListTemplate;
    }
    
    /**
     * Set to true to allow HTML but not at as the default. If a resource supports HTML and
     * HTML is the only allowed variant it will be returned but otherwise if htmlPreferred is not true
     * then the non-HTML version will be returned. Set to false to suppress HTML render.
     */
    public void setHtmlNonDefault(boolean support) {
        this.htmlNonDefault = support;
    }

    /**
     * Set to true if HTML is preferred, so from a browser any resource which can be rendered as HTML will be
     * @param htmlPreferred
     */
    public void setHtmlPreferred(boolean htmlPreferred) {
        this.htmlPreferred = htmlPreferred;
    }
    
    /**
     * Regard HTML as supported if there's a default template or we've explicit set it is supported-but-not-default
     */
    public boolean isHtmlSupported() {
        return defaultItemTemplate != null || defaultListTemplate != null || htmlNonDefault || htmlPreferred;
    }
    
    public boolean isHtmlNonDefault() {
        return htmlNonDefault;
    }

    public GenericConfig getConfigExtensions() {
        return configExtensions;
    }

    public void setConfigExtensions(GenericConfig configExtensions) {
        this.configExtensions = configExtensions;
    }

    public boolean isHtmlPreferred() {
        return htmlPreferred;
    }


    public boolean isShowSimpleLinks() {
        return showSimpleLinks;
    }

    /**
     * Set to true to force all leaf resources with no properties to be returned as simple
     * URI values instead of nested <code>{ "@id:" : "uri" }</code> objects.
     * Something of a hack to preserve legacy behaviour in some API instances.
     * @param showSimpleLinks
     */
    public void setShowSimpleLinks(boolean showSimpleLinks) {
        this.showSimpleLinks = showSimpleLinks;
    }

    public boolean isItemEndpointsReturnArrays() {
        return itemEndpointsReturnArrays;
    }

    /**
     * If set to true (default) then an item endpoint returns a singleton array of "items", if set to false
     * then the item is returned directly as the value of "items", no array
     */
    public void setItemEndpointsReturnArrays(boolean itemEndpointsReturnArrays) {
        this.itemEndpointsReturnArrays = itemEndpointsReturnArrays;
    }


    public LastModified getTimestampService() {
        return timestampService;
    }

    public void setTimestampService(LastModified timestampService) {
        this.timestampService = timestampService;
    }
    
    
    // ---- Internals -----------------------------------------------

    public boolean isGenerateCSVfilenames() {
        return generateCSVfilenames;
    }

    public void setGenerateCSVfilenames(boolean generateCSVfilenames) {
        this.generateCSVfilenames = generateCSVfilenames;
    }

    private void condOut(JSFullWriter out, String key, String value) {
        if (value != null) {
            out.pair(key, value);
        }
    }
    protected static final Pattern URIPAT = Pattern
            .compile("([^?]*)(\\.[a-z]*)?(\\?.*)?");    
}
