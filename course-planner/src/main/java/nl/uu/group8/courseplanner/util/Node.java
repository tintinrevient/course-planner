package nl.uu.group8.courseplanner.util;

import nl.uu.group8.courseplanner.service.DLQueryEngine;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.util.ArrayList;
import java.util.Set;

public class Node {

    private ArrayList<String> preferences;
    private Set<OWLNamedIndividual> courseInstances;
    private int totalPreferenceAmount;
    private DLQueryEngine engine;
    private ShortFormProvider shortFormProvider;
    private Node parentNode;
    private ArrayList<Node> childNodes;

    public Node(Node parentNode, ArrayList<String> preferences,
                int totalPreferenceAmount, DLQueryEngine engine, ShortFormProvider shortFormProvider) {
        this.parentNode = parentNode;
        this.preferences = preferences;
        this.totalPreferenceAmount = totalPreferenceAmount;
        this.engine = engine;
        this.shortFormProvider = shortFormProvider;
    }

    public ArrayList<String> getPreferences() {
        return preferences;
    }

    public double getUtility() {
        if (courseInstances == null)
            getCourseInstances();
        //-(x^2) + 4x\

        if(courseInstances.size() == 0)
            return 0.0;

        double amountOfCourses = Math.min(courseInstances.size(), 3);
        double amountOfPreferences = preferences.size();

        double coursesUtility = Math.max(0, (-(Math.pow(amountOfCourses, 2)) + (4 * amountOfCourses)) / 4); // 0-1
        double preferenceUtility = amountOfPreferences / totalPreferenceAmount; // 0-1
        double utility = (coursesUtility * 0.6) + (preferenceUtility * 0.4);

        return utility;
    }

    public Set<OWLNamedIndividual> getCourseInstances() {
        if(courseInstances == null)
            courseInstances = engine.getInstances(getQuery(), false);

        return courseInstances;
    }

    public String getQuery(){
        return String.join("and", preferences);
    }

    public Node getParentNode() {
        return parentNode;
    }

    public ArrayList<Node> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(ArrayList<Node> childNodes) {
        this.childNodes = childNodes;
    }


    public void setCourseInstances(Set<OWLNamedIndividual> courseInstances) {
        this.courseInstances = courseInstances;
    }
}