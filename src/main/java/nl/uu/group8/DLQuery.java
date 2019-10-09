package nl.uu.group8;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.nio.file.Paths;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

public class DLQuery {

    public static void main(String[] args) throws Exception {
    	

        String wine = new String(Files.readAllBytes(Paths.get("lib/wine.rdf")), "UTF-8");
        // Load an example ontology.
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager
                .loadOntologyFromOntologyDocument(new StringDocumentSource(wine));
        
        // We need a reasoner to do our query answering
        // This example uses HermiT: http://hermit-reasoner.com/
        OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
        
        ShortFormProvider shortFormProvider = new SimpleShortFormProvider();
        // Create the DLQueryPrinter helper class. This will manage the
        // parsing of input and printing of results
        DLQueryPrinter dlQueryPrinter = new DLQueryPrinter(new DLQueryEngine(reasoner,
                shortFormProvider), shortFormProvider);
        
        // Here is an example for getting instances, subclasses and superclasses with a DL query
        // You can comment out the line below to check the results
        //dlQueryPrinter.printExample();
        
        //Method for writing down the queries and printing the quiz
        dlQueryPrinter.printExample();
            
        
        }

    }


class DLQueryEngine {
    private final OWLReasoner reasoner;
    private final DLQueryParser parser;

    public DLQueryEngine(OWLReasoner reasoner, ShortFormProvider shortFormProvider) {
        this.reasoner = reasoner;
        parser = new DLQueryParser(reasoner.getRootOntology(), shortFormProvider);
    }

    public Set<OWLClass> getSuperClasses(String classExpressionString, boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> superClasses = reasoner
                .getSuperClasses(classExpression, direct);
        return superClasses.getFlattened();
    }

    public Set<OWLClass> getEquivalentClasses(String classExpressionString) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
        Set<OWLClass> result = null;
        if (classExpression.isAnonymous()) {
            result = equivalentClasses.getEntities();
        } else {
            result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass());
        }
        return result;
        }

    public Set<OWLClass> getSubClasses(String classExpressionString, boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
        return subClasses.getFlattened();
        }

    public Set<OWLNamedIndividual> getInstances(String classExpressionString,
            boolean direct) {
        if (classExpressionString.trim().length() == 0) {
            return Collections.emptySet();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression,
                direct);
        return individuals.getFlattened();
        }
    }

class DLQueryParser {
    private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidiShortFormProvider;

    public DLQueryParser(OWLOntology rootOntology, ShortFormProvider shortFormProvider) {
        this.rootOntology = rootOntology;
        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
        Set<OWLOntology> importsClosure = rootOntology.getImportsClosure();
        // Create a bidirectional short form provider to do the actual mapping.
        // It will generate names using the input
        // short form provider.
        bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
                importsClosure, shortFormProvider);
    }

    public OWLClassExpression parseClassExpression(String classExpressionString) {
        OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager()
                .getOWLDataFactory();
        ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(
                dataFactory, classExpressionString);
        parser.setDefaultOntology(rootOntology);
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
        parser.setOWLEntityChecker(entityChecker);
        return parser.parseClassExpression();
        }
    }

class DLQueryPrinter {
    private final DLQueryEngine dlQueryEngine;
    private final ShortFormProvider shortFormProvider;

    public DLQueryPrinter(DLQueryEngine engine, ShortFormProvider shortFormProvider) {
        this.shortFormProvider = shortFormProvider;
        dlQueryEngine = engine;
        }

    public void printExample() throws IOException  {
    	String example = "Wines that have medium body";
    	System.out.println(example);
    	
    	String query = "Wine and hasBody value Medium";
    	
    	System.out.println("\nInstances:");
    	Set<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(
    			query, false);
    	for (OWLEntity entity : individuals) {
        	System.out.println(shortFormProvider.getShortForm(entity));
        }
    	
    	System.out.println("\nSuperClasses:");
    	Set<OWLClass> superClasses = dlQueryEngine.getSuperClasses(
    			query, false);
    	for (OWLClass class_ : superClasses) {
        	System.out.println(shortFormProvider.getShortForm(class_));
        }
    	
    	System.out.println("\nSubClasses:");
    	Set<OWLClass> subClasses = dlQueryEngine.getSubClasses(
    			query, false);
    	for (OWLClass class_ : subClasses) {
        	System.out.println(shortFormProvider.getShortForm(class_));
        }
    	
    }

      
    public String returnEntity(String query){
    	String returnString = "";

        for (OWLEntity entity : quizQuery(query)) {
        	returnString=shortFormProvider.getShortForm(entity);
        	return returnString;
        }
        return "";
    }
    
    public Set<OWLNamedIndividual> quizQuery(String classExpression) {
                Set<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(
                        classExpression, false);
                return individuals;
        }
    
    
    }
