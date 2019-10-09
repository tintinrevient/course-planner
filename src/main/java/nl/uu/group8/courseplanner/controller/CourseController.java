package nl.uu.group8.courseplanner.controller;

import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.service.DLQueryPrinter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import nl.uu.group8.courseplanner.domain.Course;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private nl.uu.group8.courseplanner.repository.CourseRepository courseRepository;

    @GetMapping
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @GetMapping(value = "/test")
    public List<Course> find() throws Exception{

        String wine = new String(Files.readAllBytes(Paths.get("wine.rdf")), "UTF-8");
        System.out.print(wine);
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
        return courseRepository.findAll();
    }

}
