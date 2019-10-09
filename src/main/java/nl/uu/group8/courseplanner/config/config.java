package nl.uu.group8.courseplanner.config;

import org.apache.commons.io.FileUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Configuration
public class config {

    @Bean
    public OWLReasoner reasoner() throws Exception {
        File file = ResourceUtils.getFile("classpath:wine.rdf");
        String wine = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // Load an example ontology.
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(wine));

        // We need a reasoner to do our query answering
        // This example uses HermiT: http://hermit-reasoner.com/
        OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);

        return reasoner;
    }

    @Bean
    public ShortFormProvider shortFormProvider() {
        return new SimpleShortFormProvider();
    }

    @Autowired
    public OWLReasoner reasoner;

    @Autowired
    public ShortFormProvider shortFormProvider;

    @Bean
    public BidirectionalShortFormProviderAdapter bidiShortFormProvider() {
        return new BidirectionalShortFormProviderAdapter(reasoner.getRootOntology().getOWLOntologyManager(),
                reasoner.getRootOntology().getImportsClosure(), shortFormProvider);
    }

}
