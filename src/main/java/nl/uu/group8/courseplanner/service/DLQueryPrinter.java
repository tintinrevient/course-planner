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

    public String printExample() throws IOException  {

        StringBuilder stringBuilder = new StringBuilder();

        String example = "Wines that have medium body";
        stringBuilder.append(example);

        String query = "Wine and hasBody value Medium";

        stringBuilder.append("\nInstances:");
        Set<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(query, false);
        for (OWLEntity entity : individuals) {
            stringBuilder.append(shortFormProvider.getShortForm(entity));
        }

        stringBuilder.append("\nSuperClasses:");
        Set<OWLClass> superClasses = dlQueryEngine.getSuperClasses(query, false);
        for (OWLClass class_ : superClasses) {
            stringBuilder.append(shortFormProvider.getShortForm(class_));
        }

        stringBuilder.append("\nSubClasses:");
        Set<OWLClass> subClasses = dlQueryEngine.getSubClasses(query, false);
        for (OWLClass class_ : subClasses) {
            stringBuilder.append(shortFormProvider.getShortForm(class_));
        }

        return stringBuilder.toString();

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
