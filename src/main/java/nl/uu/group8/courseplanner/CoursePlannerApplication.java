package nl.uu.group8.courseplanner;

import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CoursePlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoursePlannerApplication.class, args);
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
