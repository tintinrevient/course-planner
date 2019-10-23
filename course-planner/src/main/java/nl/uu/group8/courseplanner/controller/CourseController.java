package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.domain.Course;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.util.Formula;
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
import java.net.InetAddress;
import java.util.*;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;

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

    // the course dictionary
    private Map<String, Course> courses = new HashMap<>();

    // this agent's availability calendar
    private Map<String, Set<Course>> availability = new HashMap<>();

    // threshold: the maximum number of can-be registered courses per period
    private int max = 3;


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

        List<Course> courseList = parseQuery(query);

        long end = System.currentTimeMillis();
        log.info("Search time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(courseList);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody List<String> selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();
        StringBuilder message = new StringBuilder();

        for(String courseName : selected) {
            Course course = courses.get(courseName);
            String period = course.getPeriod();
            Set<String> timeslot = course.getTimeslot();

            if(null == availability.get(period) || availability.get(period).size() == 0) {
                availability.put(period, new HashSet<>(Arrays.asList(course)));
                continue;
            }

            if(availability.get(period).size() >= max) {
                message.append("Number of registered courses for " + period + " is over " + max + "\n");
                continue;
            }

            //verify timeslot conflict
            boolean flag = false;
            for(Course registeredCourse : availability.get(period)) {
                Set<String> intersection = new HashSet<>(registeredCourse.getTimeslot());
                intersection.retainAll(timeslot);
                if(intersection.size() > 0) {
                    flag = true;
                    for(String conflict : intersection) {
                        message.append("Conflict in " + period + " on " + conflict);
                    }
                    break;
                }
            }

            if(!flag)
                availability.get(period).add(course);

            if(availability.get(period).size() >= max) {
                message.append("Number of registered courses for " + period + " is over " + max);
                continue;
            }
        }

        response.put("courses", availability);
        response.put("msg", message.toString());

        long end = System.currentTimeMillis();
        log.info("Register time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(response);
    }

    private List<Course> parseQuery(String query) {

        List<Course> courseList = new ArrayList<>();

        Set<OWLNamedIndividual> individuals = engine.getInstances(query, false);
        for (OWLEntity entity : individuals) {
            String courseName = shortFormProvider.getShortForm(entity);

            String hasCourseQuery = "hasCourse value " + courseName;
            Set<String> timeslot = new HashSet<>();
            Set<OWLNamedIndividual> timeslotIndividuals = engine.getInstances(hasCourseQuery, false);
            for (OWLEntity timeslotEntity : timeslotIndividuals) {
                timeslot.add(standarize(shortFormProvider.getShortForm(timeslotEntity)));
            }

            String containsCoursesQuery = "containsCourses value " + courseName;
            Set<String> period = new HashSet<>();
            Set<OWLNamedIndividual> periodIndividuals = engine.getInstances(containsCoursesQuery, false);
            for (OWLEntity periodEntity : periodIndividuals) {
                period.add(standarize(shortFormProvider.getShortForm(periodEntity)));
            }

            Course course = new Course();
            course.setId(courseName);
            course.setName(standarize(courseName));
            course.setTimeslot(timeslot);
            course.setPeriod(period.toArray(new String[1])[0]);

            courseList.add(course);
            courses.put(courseName, course);
        }

        return courseList;
    }

    private String standarize(String input) {
        return input.replaceAll("_", " ");
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
