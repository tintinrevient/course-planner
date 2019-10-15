package nl.uu.group8.courseplanner.service;

import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DLQueryParser {

    @Autowired
    private OWLReasoner reasoner;

    @Autowired
    private ShortFormProvider shortFormProvider;

    public OWLClassExpression parseClassExpression(String classExpressionString) {

        OWLOntology rootOntology = reasoner.getRootOntology();

        OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager().getOWLDataFactory();

        Set<OWLOntology> importsClosure = rootOntology.importsClosure().collect(Collectors.toSet());
        BidirectionalShortFormProvider bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(importsClosure, shortFormProvider);
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);

        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);

        return parser.parse(classExpressionString);
    }
}
