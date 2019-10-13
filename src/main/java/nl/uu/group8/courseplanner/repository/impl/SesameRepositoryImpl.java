package nl.uu.group8.courseplanner.repository.impl;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.repository.SesameRepository;
import org.openrdf.model.*;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@Repository
@Slf4j
public class SesameRepositoryImpl implements SesameRepository, InitializingBean, DisposableBean {

    private SailRepository repository = null;
    private RepositoryConnection conn = null;
    private ValueFactory valueFactory = null;

    @PostConstruct
    public void init() throws Exception {
        try {
            repository = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
            repository.initialize();

        } catch(RepositoryException e) {
            log.error("sail repository initialization error: " + e);
        }
    }

    public Literal createLiteral(String s) {
        return valueFactory.createLiteral(s);
    }

    public URI createURI(String uri) {
        return valueFactory.createURI(uri);
    }

    public BNode createBNode() {
        return valueFactory.createBNode();
    }

    public void add(URI s, URI p, Value o) {
        Statement statement = valueFactory.createStatement((Resource) s, p, o);
        conn.add(statement);
    }

    public void addString(String rdfstring, RDFFormat format) {
        try {
            StringReader stringReader = new StringReader(rdfstring);
            conn.add(stringReader, "", format);
        } catch (IOException e) {
            log.error("IO exception: ", e);
        }
    }

    public void addFile(File file, RDFFormat format) {
        try {
            conn.add(file, "", format);
        } catch (IOException e) {
            log.error("IO exception: ", e);
        }
    }

    public void addURI(String urlstring, RDFFormat format) {
        try {
            URL url = new URL(urlstring);
            URLConnection urlConnection = url.openConnection();
            urlConnection.addRequestProperty("accept", format.getDefaultMIMEType());
            InputStream inputStream = urlConnection.getInputStream();
            conn.add(inputStream, urlstring, format);
        } catch (Exception e) {
            log.error("Network exception: ", e);
        }
    }

    public void dumpRDF(OutputStream outputStream, RDFFormat format) {
        RDFWriter writer = Rio.createWriter(format, outputStream);
        conn.export(writer);
    }

    public List tuplePatternQuery(URI s, URI p, Value o) {
        RepositoryResult result = conn.getStatements(s, p, o, true);

        List list = new ArrayList();
        while(result.hasNext())
            list.add(result.next());

        return list;
    }

    public List runSPARQL(String querystring) {
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, querystring);
        TupleQueryResult result = query.evaluate();

        List list = new ArrayList();
        while(result.hasNext()) {
            BindingSet bindings = result.next();

            Set names = bindings.getBindingNames();
            Map hashMap = new HashMap();
            for(Object name : names)
                hashMap.put((String) name, bindings.getValue((String) name).toString());

            list.add(hashMap);
        }

        return list;
    }

    public String runSPARQL(String querystring, RDFFormat format) {
        GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, querystring);

        StringWriter stringWriter = new StringWriter();
        RDFWriter writer = Rio.createWriter(format, stringWriter);

        query.evaluate(writer);
        return stringWriter.toString();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        conn = repository.getConnection();
        valueFactory = conn.getValueFactory();

        File winefile = ResourceUtils.getFile("classpath:wine.rdf");
        addFile(winefile, RDFFormat.RDFXML);

        File coursefile = ResourceUtils.getFile("classpath:course.rdf");
        addFile(coursefile, RDFFormat.RDFXML);
    }

    @Override
    public void destroy() throws Exception {
        conn.close();
    }

}
