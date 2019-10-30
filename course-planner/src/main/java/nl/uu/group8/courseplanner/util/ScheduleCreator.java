package nl.uu.group8.courseplanner.util;

import nl.uu.group8.courseplanner.domain.Course;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ScheduleCreator {

    private DLQueryEngine engine;
    private  ShortFormProvider shortFormProvider;

    public ScheduleCreator(DLQueryEngine engine, ShortFormProvider shortFormProvider){
        this.engine = engine;
        this.shortFormProvider = shortFormProvider;
    }

    public ArrayList<OWLNamedIndividual> create(ArrayList<Node> nodes){
        ArrayList<OWLNamedIndividual> combinedCourses = new ArrayList<>();
        ArrayList<OWLNamedIndividual> timeSlots = new ArrayList<>();
        for(Node node : nodes){
            //verify timeslot conflict
            for (OWLNamedIndividual courseInstance : node.getCourseInstances()){
                String courseName = shortFormProvider.getShortForm(courseInstance);
                String query = "hasCourse value " + courseName;
                Set<OWLNamedIndividual> timeSlotsOnt = engine.getInstances(query, false);

                for (OWLNamedIndividual timeSlotInstance : timeSlotsOnt) {
                    if (timeSlots.contains(timeSlotInstance))
                      System.out.println("FRANCO: Conflict: " + courseName + " on " + timeSlotInstance.toStringID());
                    else
                      timeSlots.add(timeSlotInstance);
              }
          }
          combinedCourses.addAll(node.getCourseInstances());
        }
        return combinedCourses;
    }
}
