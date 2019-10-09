package nl.uu.group8.courseplanner.service;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;
import java.util.Set;
import java.io.IOException;

public class DLQueryPrinter {

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
        Set<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(query, false);
        for (OWLEntity entity : individuals) {
            System.out.println(shortFormProvider.getShortForm(entity));
        }

        System.out.println("\nSuperClasses:");
        Set<OWLClass> superClasses = dlQueryEngine.getSuperClasses(query, false);
        for (OWLClass class_ : superClasses) {
            System.out.println(shortFormProvider.getShortForm(class_));
        }

        System.out.println("\nSubClasses:");
        Set<OWLClass> subClasses = dlQueryEngine.getSubClasses(query, false);
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
