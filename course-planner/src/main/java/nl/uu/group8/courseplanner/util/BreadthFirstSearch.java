package nl.uu.group8.courseplanner.util;

import nl.uu.group8.courseplanner.service.DLQueryEngine;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.util.*;

public class BreadthFirstSearch {

    private DLQueryEngine engine;
    private ShortFormProvider shortFormProvider;
    private ArrayList<ArrayList<String>> preferenceCache;
    private Node higestNode;
    private List<Node> bestNodes = new ArrayList<>();
    private ArrayList<String> bestPreference = new ArrayList<>();

    public BreadthFirstSearch(DLQueryEngine engine, ShortFormProvider shortFormProvider) {
        this.engine = engine;
        this.shortFormProvider = shortFormProvider;
    }

    public Node search(String query) {
        System.out.println("franco sto facendo la query "+ query);
        preferenceCache = new ArrayList<>();
        ArrayList<String> splitQuery = new ArrayList<String>(Arrays.asList(query.split("(?i)and")));

        int totalPreferencesAmount = splitQuery.size();
        Node rootNode = new Node(null, splitQuery, totalPreferencesAmount, engine, shortFormProvider);
        createTree(rootNode, totalPreferencesAmount);
        List<Node> list = new ArrayList<>();
        list.add(rootNode);
        searchOnTreeGoal(list);

        for(Node node : bestNodes) {
            System.out.println("franco e un best node" + node.getPreferences());
            if (higestNode == null || node.getPreferences().size() > higestNode.getPreferences().size())
                higestNode = node;
        }

//        ArrayList<ArrayList<String>> outputPreferences = new ArrayList<>();
//        createPreferenceList(splitQuery, outputPreferences);
//        outputPreferences.sort(new PreferenceSizeComparator());
//        searchList(outputPreferences);

        System.out.println("franco e un higest node" + higestNode.getPreferences());
        return higestNode;
    }

    private void createPreferenceList(ArrayList<String> input, ArrayList<ArrayList<String>> outputPreferences) {

        if (input.size() == 1)
            return;

        for (int i = 1; i < input.size(); i++) {
            ArrayList<String> tempClone = (ArrayList<String>) input.clone();
            tempClone.remove(i);
            if (!outputPreferences.contains(tempClone)) {
                outputPreferences.add(tempClone);
                createPreferenceList(tempClone, outputPreferences);
            }

        }
    }

    private void createTree(Node currentNode, int totalPreferenceAmount) {

        if (currentNode.getPreferences().size() == 1)
            return;

        ArrayList<Node> childNodes = new ArrayList<>();
        ArrayList<String> parentPreferences = currentNode.getPreferences();
        for (int i = 1; i < parentPreferences.size(); i++) {
            ArrayList<String> tempClone = (ArrayList<String>) parentPreferences.clone();
            tempClone.remove(i);
            if(!preferenceCache.contains(tempClone)) {
                preferenceCache.add(tempClone);
                Node childNode = new Node(currentNode, tempClone, totalPreferenceAmount, engine, shortFormProvider);
                childNodes.add(childNode);
                createTree(childNode, totalPreferenceAmount);
            }
        }

        currentNode.setChildNodes(childNodes);
    }

    private Set<OWLNamedIndividual> verifyTimeSlots(Set<OWLNamedIndividual> courseInstances){
        Set<OWLNamedIndividual> noConflictsSet = new HashSet<>();
        ArrayList<OWLNamedIndividual> timeSlots = new ArrayList<>();
        for (OWLNamedIndividual courseInstance : courseInstances) {
            String courseName = shortFormProvider.getShortForm(courseInstance);
            String query = "hasCourse value " + courseName;
            Set<OWLNamedIndividual> timeSlotsOnt = engine.getInstances(query, false);
            boolean overlap = false;
            for (OWLNamedIndividual timeSlotInstance : timeSlotsOnt) {
                if (timeSlots.contains(timeSlotInstance)){
                    overlap = true;
                }
            }
            if (!overlap){
                noConflictsSet.add(courseInstance);
                timeSlots.addAll(timeSlotsOnt);
            }
        }
        return noConflictsSet;
    }
    private void searchList(ArrayList<ArrayList<String>> preferences){
        for(ArrayList<String> preference : preferences){
            String query = String.join("and", preference);
            Set<OWLNamedIndividual> courseInstances = engine.getInstances(query, false);
            if(verifyTimeSlots(courseInstances).size() >= 2){
                bestPreference = preference;
                return;
            }
        }
    }

    private void searchOnTreeGoal(List<Node> currentNodes){
        boolean goalReached = false;
        for(Node currentNode: currentNodes){
            Set<OWLNamedIndividual> coursesInstances = verifyTimeSlots(currentNode.getCourseInstances());
            if(coursesInstances.size() >= 2) {
                currentNode.setCourseInstances(coursesInstances);
                bestNodes.add(currentNode);
                goalReached = true;
            }
        }

        if (goalReached){
            return;
        }


        System.out.println("franco sto passando al livello successivo");
        List<Node> children = new ArrayList<>();
        for(Node currentNode: currentNodes){
            if (currentNode.getChildNodes() != null){
                for (Node childnode: currentNode.getChildNodes()){
                    children.add(childnode);
                }
            }

        }
        searchOnTreeGoal(children);
    }

    private void searchOnTreeUtility(Node currentNode) {
        if (currentNode.getChildNodes() == null || currentNode.getChildNodes().size() == 0) {
            if(Double.compare(currentNode.getUtility(), 0.0) > 0)
                bestNodes.add(currentNode);
            return;
        }
        boolean foundBetterNode = false;
        for (Node childNode: currentNode.getChildNodes()) {
            if( Double.compare(childNode.getUtility(),currentNode.getUtility()) >= 0) {
                foundBetterNode = true;
                searchOnTreeUtility(childNode);
            }
        }
        if (!foundBetterNode)
            bestNodes.add(currentNode);
    }
}

class PreferenceSizeComparator implements Comparator<ArrayList<String>>{
    @Override
    public int compare(ArrayList<String> o1, ArrayList<String> o2) {
        return o2.size() - o1.size();
    }
}



