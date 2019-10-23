package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.util.Formula;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.*;

@RestController
@RequestMapping("/course")
@Slf4j
public class CourseController {

    @Autowired
    private DLQueryEngine engine;

    @Autowired
    private ShortFormProvider shortFormProvider;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    Environment environment;

    @Value("${spring.application.name}")
    private String serviceId;

    // mapping: url_course -> the history of all the scores this agent has received from neighbouring agents
    private Map<String, List<Integer>> worldStates = new HashMap<>();

    // mapping: url_course -> the history of all the feedback this agent has given to neighbouring agents' scores
    private Map<String, List<Boolean>> feedback = new HashMap<>();

    // mapping: url_course -> beta reputation rating based on this agent's history of all the feedback
    private Map<String, Double> rating = new HashMap<>();

    // mapping: course -> the latest score by the most-trusted neighbouring agent
    private Map<String, Integer> evaluation = new HashMap<>();

    // set: this agent's registered courses
    private Map<String, Set<String>> courses = new HashMap<>();

    // threshold: the maximum number of can-be registered courses per period
    private int max = 3;

    // set: this agent's availability calendar
    private Map<String, Set<String>> availability = new HashMap<>();

    @PostMapping("/ask-eval")
    public ResponseEntity<?> ask(@RequestBody List<String> selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();

        String course = selected.get(0).split("[|]")[0];

        if(selected.size() > 1) {
            response.put("msg", "Evaluation can only be applied to one course!");
            response = getStates(response);

            return ResponseEntity.ok().body(response);
        }

        String localServerUrl = InetAddress.getLocalHost().getHostAddress() + ":" + environment.getProperty("local.server.port");
        log.info("local server URL: " + localServerUrl);
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(serviceId);

        // return the score from the previous state
        if(serviceInstanceList.size() < 2) {
            response.put("msg", "No neighbouring agents are online!");

            response = getScore(response, course);
            response = getStates(response);
            return ResponseEntity.ok().body(response);
        }

        for(ServiceInstance serviceInstance : serviceInstanceList) {
            String host = serviceInstance.getHost();
            int port = serviceInstance.getPort();
            String url = host + ":" + port;

            if(null != url && url.equalsIgnoreCase(localServerUrl))
                continue;

            String serviceUrl = "http://" + url + "/course/answer-eval?course=" + course;
            ResponseEntity<Integer> responseEntity = restTemplate.getForEntity(serviceUrl, Integer.class);

            log.info("Ask the evaluation by " + url);

            String key = url + "_" + course;

            if(!worldStates.containsKey(key)) {
                List<Integer> occurrences = new ArrayList<>();
                occurrences.add(responseEntity.getBody());
                worldStates.put(key, occurrences);

                List<Boolean> feedbacks = new ArrayList<>();
                feedbacks.add(new Random().nextBoolean());
                feedback.put(key, feedbacks);

            } else {
                worldStates.get(key).add(responseEntity.getBody());
                feedback.get(key).add(new Random().nextBoolean());
            }
        }

        rating = Formula.betaReputationRating(feedback);

        String maxRatingKey = getMaxRatingKey(course);

        if(maxRatingKey.isEmpty()) {
            response.put("msg", "Neighbouring agents haven't any feedback for reference!");
            response = getStates(response);
            return ResponseEntity.ok().body(response);
        }

        response = updateScore(response, course, maxRatingKey);
        response = getStates(response);

        long end = System.currentTimeMillis();
        log.info("Evaluation time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/answer-eval")
    public int answer(HttpServletRequest request, @RequestParam String course) throws Exception {
        StringBuffer requestURL = request.getRequestURL();
        String requestURI = request.getRequestURI();
        String url = requestURL.substring(0, requestURL.indexOf(requestURI));

        log.info("Answer the evaluation from " + url);

        return new Random().ints(1, 11).findFirst().getAsInt();
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody String query) throws Exception {

        long start = System.currentTimeMillis();

        List<String> courseList = new ArrayList<>();

        Set<OWLNamedIndividual> individuals = engine.getInstances(query, false);
        for (OWLEntity entity : individuals) {
            String name = shortFormProvider.getShortForm(entity);

            String hasCourseQuery = "hasCourse value " + name;
            Set<String> timeslot = new HashSet<>();
            Set<OWLNamedIndividual> timeslotIndividuals = engine.getInstances(hasCourseQuery, false);
            for (OWLEntity timeslotEntity : timeslotIndividuals) {
                timeslot.add(shortFormProvider.getShortForm(timeslotEntity));
            }

            String containsCoursesQuery = "containsCourses value " + name;
            Set<String> period = new HashSet<>();
            Set<OWLNamedIndividual> periodIndividuals = engine.getInstances(containsCoursesQuery, false);
            for (OWLEntity periodEntity : periodIndividuals) {
                period.add(shortFormProvider.getShortForm(periodEntity));
            }

            courseList.add(name + "|" + String.join("|", timeslot) + "|" + String.join("|", period));
        }

        long end = System.currentTimeMillis();
        log.info("Search time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(courseList);
    }

    @PostMapping("/similar")
    public ResponseEntity<?> similar(@RequestBody String course) throws Exception {

        long start = System.currentTimeMillis();

        String methodology_query = "useMethodology value " + course;

        Set<OWLNamedIndividual> methodology_individuals = engine.getInstances(methodology_query, false);
        Set<String> methodology_hashset = new HashSet<>();
        for (OWLEntity _entity : methodology_individuals) {
            String methodology = shortFormProvider.getShortForm(_entity);
            methodology_hashset.add(methodology);
        }

        String course_query = "Course";
        Set<OWLNamedIndividual> course_individuals = engine.getInstances(course_query, false);
        Set<String> course_hashset = new HashSet<>();
        for (OWLEntity entity : course_individuals) {
            String course_name = shortFormProvider.getShortForm(entity);
            if(course_name.equalsIgnoreCase(course)){
                methodology_query = "useMethodology value " + course_name;

                Set<OWLNamedIndividual> _methodology_individuals = engine.getInstances(methodology_query, false);
                Set<String> _methodology_hashset = new HashSet<>();
                for (OWLEntity _entity : _methodology_individuals) {
                    String methodology = shortFormProvider.getShortForm(_entity);
                    _methodology_hashset.add(methodology);
                }
                Set<String> intersection = new HashSet<String>(_methodology_hashset);
                if(intersection.retainAll(methodology_hashset)){
                    course_hashset.add(course_name);
                }
            }
        }

        String topic_query = "isCoveredBy value " + course;

        Set<OWLNamedIndividual> topic_individuals = engine.getInstances(topic_query, false);
        Set<String> topics = new HashSet<>();
        for (OWLEntity entity : topic_individuals) {
            String topic = shortFormProvider.getShortForm(entity);
            topics.add(topic);
            Set<OWLClass> topic_set = engine.getSuperClasses(topic, false);
            for (OWLEntity _entity : topic_set) {
                String _topic = shortFormProvider.getShortForm(_entity);
                topics.add(_topic);
            }
        }

        Set<String> _course_hashset = new HashSet<>();
        for(String _course : course_hashset){
            String _topic_query = "isCoveredBy value " + _course;

            Set<OWLNamedIndividual> _topic_individuals = engine.getInstances(_topic_query, false);
            Set<String> _topics = new HashSet<>();
            for (OWLEntity entity : _topic_individuals) {
                String topic = shortFormProvider.getShortForm(entity);
                _topics.add(topic);
                Set<OWLClass> topic_set = engine.getSuperClasses(topic, false);
                for (OWLEntity _entity : topic_set) {
                    String _topic = shortFormProvider.getShortForm(_entity);
                    _topics.add(_topic);
                }
            }
            Set<String> intersection = new HashSet<String>(_topics);
            if(intersection.retainAll(topics)){
                _course_hashset.add(_course);
            }
        }

        List<String> courseList = new ArrayList<>();
        for (String name : _course_hashset) {

            String hasCourseQuery = "hasCourse value " + name;
            Set<String> timeslot = new HashSet<>();
            Set<OWLNamedIndividual> timeslotIndividuals = engine.getInstances(hasCourseQuery, false);
            for (OWLEntity timeslotEntity : timeslotIndividuals) {
                timeslot.add(shortFormProvider.getShortForm(timeslotEntity));
            }

            String containsCoursesQuery = "containsCourses value " + name;
            Set<String> period = new HashSet<>();
            Set<OWLNamedIndividual> periodIndividuals = engine.getInstances(containsCoursesQuery, false);
            for (OWLEntity periodEntity : periodIndividuals) {
                period.add(shortFormProvider.getShortForm(periodEntity));
            }

            courseList.add(name + "|" + String.join("|", timeslot) + "|" + String.join("|", period));
        }

        long end = System.currentTimeMillis();
        log.info("Search time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(courseList);
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody List<String> selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();

        availability = updateAvailability();

        log.info("Availability: " + availability.toString());

        for(String select : selected) {
            String[] parts = select.split("[|]");
            String period = parts[parts.length - 1];

            boolean conflict = false;
            for(int i = 1; i < parts.length - 1; i++) {
                if(null != availability.get(period) && availability.get(period).contains(parts[i])) {
                    response.put("msg", "Conflict in " + period + ": " + parts[i]);
                    conflict = true;
                    break;
                }
            }

            if(null != availability.get(period) && courses.get(period).size() >= max) {
                response.put("msg", "Number of registered courses for " + period + " is over " + max);
                response.put("courses", courses);
                return ResponseEntity.ok().body(response);
            }

            if(!conflict) {
                // change of courses -> change of availability calendar
                if(null == courses.get(period))
                    courses.put(period, new HashSet<String>());

                courses.get(period).add(select);
                availability = updateAvailability();
            }

            if(null != availability.get(period) && courses.get(period).size() >= max) {
                response.put("msg", "Number of registered courses for " + period + " is over " + max);
                break;
            }
        }

        response.put("courses", courses);

        long end = System.currentTimeMillis();
        log.info("Register time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(response);
    }

    private Map<String, Set<String>> updateAvailability() {
        for(String key : courses.keySet()) {
            for(String course : courses.get(key)) {
                String[] parts = course.split("[|]");
                availability.put(key, new HashSet<>(Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length - 1))));
            }
        }

        return availability;
    }

    private String getMaxRatingKey(String course) {
        double maxRating = -2;
        String maxRatingKey = "";

        for(String key : rating.keySet()) {
            if(key.contains(course)) {
                if(rating.get(key) > maxRating) {
                    maxRating = rating.get(key);
                    maxRatingKey = key;
                }
            }
        }

        return maxRatingKey;
    }

    private Map getStates(Map response) {

        List<String> worldStatesList = new ArrayList<>();
        List<String> feedbackList = new ArrayList<>();
        List<String> ratingList = new ArrayList<>();

        for(String key : worldStates.keySet()) {
            worldStatesList.add(key + " -> " + worldStates.get(key));
            feedbackList.add(key + " -> " + feedback.get(key));
            ratingList.add(key + " -> " + rating.get(key));
        }

        response.put("worldStates", worldStatesList);
        response.put("feedback", feedbackList);
        response.put("rating", ratingList);

        return response;
    }

    private Map getScore(Map response, String course) {
        int score = -1;
        for(String key : evaluation.keySet()) {
            if(key.contains(course)) {
                score = evaluation.get(key);
                break;
            }
        }

        if(score != -1)
            response.put("score", course + " -> " + score);

        return response;
    }

    private Map updateScore(Map response, String course, String maxRatingKey) {
        List<Integer> scoreList = worldStates.get(maxRatingKey);
        int score = scoreList.get(scoreList.size()-1);
        evaluation.put(course, score);
        response.put("score", course + " -> " + score);

        return response;
    }

}
