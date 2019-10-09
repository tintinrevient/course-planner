package nl.uu.group8.courseplanner.controller;

import nl.uu.group8.courseplanner.service.DLQueryEngine;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Set;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private DLQueryEngine dlQueryEngine;

    @Autowired
    private ShortFormProvider shortFormProvider;

    @GetMapping(value = "/test")
    public String find() throws Exception{

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

}
