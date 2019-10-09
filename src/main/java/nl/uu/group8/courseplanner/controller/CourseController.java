package nl.uu.group8.courseplanner.controller;

import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.service.DLQueryPrinter;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/course")
public class CourseController {

//    @Autowired
//    DLQueryEngine queryEngine;
//
//    @Autowired
//    DLQueryParser queryParser;
//
//    @Autowired
//    DLQueryPrinter queryPrinter;

    @GetMapping(value = "/test")
    public String find() throws Exception{

        File file = new File(getClass().getClassLoader().getResource("wine.rdf").getFile());
        String wine = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // Load an example ontology.
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(wine));

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

        return dlQueryPrinter.printExample();
    }

}
