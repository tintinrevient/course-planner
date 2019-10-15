package nl.uu.group8.courseplanner.config;

import lombok.extern.slf4j.Slf4j;
import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.commons.io.FileUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import uk.ac.manchester.cs.jfact.JFactFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class ReasonerConfig {

    @Value("${reasoner.type}")
    private String reasonerType;

    @Value("${ontology.filename}")
    private String ontologyFilename;

    @Bean
    public OWLReasoner reasoner() throws Exception {

        File file = ResourceUtils.getFile("classpath:" + ontologyFilename);
        String wine = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // Load an ontology.
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(wine));

        // We need a reasoner to do our query answering
        OWLReasoner reasoner = null;

        switch(reasonerType) {
            case "hermit":
                reasoner = new Reasoner(new org.semanticweb.HermiT.Configuration(), ontology);
                break;
            case "pellet":
                reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
                break;
            case "jfact":
                reasoner = new JFactFactory().createReasoner(ontology, new SimpleConfiguration());
                break;
            default:
                reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        }

        log.info("Ontology in use is: " + ontologyFilename);
        log.info("Reasoner in use is: " + reasonerType);

        return reasoner;
    }

    @Bean
    public ShortFormProvider shortFormProvider() {
        return new SimpleShortFormProvider();
    }

}
