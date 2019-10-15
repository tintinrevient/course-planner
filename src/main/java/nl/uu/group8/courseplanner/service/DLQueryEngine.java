package nl.uu.group8.courseplanner.service;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DLQueryEngine {

    @Autowired
    private OWLReasoner reasoner;

    @Autowired
    private DLQueryParser parser;

    public Set<OWLClass> getSuperClasses(String classExpressionString, boolean direct) {

        if (classExpressionString.trim().length() == 0)
            return Collections.emptySet();

        OWLClassExpression classExpression = parser.parseClassExpression(classExpressionString);
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(classExpression, direct);

        return superClasses.entities().collect(Collectors.toSet());
    }


    public Set<OWLClass> getEquivalentClasses(String classExpressionString) {

        if (classExpressionString.trim().length() == 0)
            return Collections.emptySet();

        OWLClassExpression classExpression = parser.parseClassExpression(classExpressionString);
        Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
        Set<OWLClass> result = null;

        if (classExpression.isAnonymous())
            result = equivalentClasses.entities().collect(Collectors.toSet());
        else
            result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass());

        return result;
    }

    public Set<OWLClass> getSubClasses(String classExpressionString, boolean direct) {

        if (classExpressionString.trim().length() == 0)
            return Collections.emptySet();

        OWLClassExpression classExpression = parser.parseClassExpression(classExpressionString);
        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);

        return subClasses.entities().collect(Collectors.toSet());
    }

    public Set<OWLNamedIndividual> getInstances(String classExpressionString, boolean direct) {

        if (classExpressionString.trim().length() == 0)
            return Collections.emptySet();

        OWLClassExpression classExpression = parser.parseClassExpression(classExpressionString);
        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression, direct);

        return individuals.entities().collect(Collectors.toSet());
    }

}
