package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.domain.Agent;
import nl.uu.group8.courseplanner.domain.Course;
import nl.uu.group8.courseplanner.domain.Preference;
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

    // multi-agent world state
    private Map<String, Agent> agents = new HashMap<>();

    // known evaluation of courses
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

            //1. all agents with beta reputation rating with their evaluation
            //2. final course score
            response.put("agents", agents);

            return ResponseEntity.ok().body(response);
        }

        String localServerUrl = InetAddress.getLocalHost().getHostAddress() + ":" + environment.getProperty("local.server.port");
        log.info("local server URL: " + localServerUrl);
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(serviceId);

        // return the score from the previous state
        if(serviceInstanceList.size() < 2) {
            response.put("msg", "No neighbouring agents are online!");
            response.put("agents", agents);
            response.put("evaluation", evaluation);
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

            if(!agents.containsKey(url)) {
                Agent agent = new Agent();
                agent.setAddress(url);

                List<Integer> _occurrences = new ArrayList<>();
                _occurrences.add(responseEntity.getBody());
                Map<String, List<Integer>> occurrences = new HashMap<>();
                occurrences.put(course, _occurrences);

                agent.setOccurrences(occurrences);

                List<Boolean> _feedbacks = new ArrayList<>();
                _feedbacks.add(new Random().nextBoolean());
                Map<String, List<Boolean>> feedbacks = new HashMap<>();
                feedbacks.put(course, _feedbacks);

                agent.setFeedbacks(feedbacks);

                agent.setBetaReputationRating(Formula.betaReputationRating(feedbacks));

                agents.put(url, agent);

            } else {
                Agent agent = agents.get(url);

                if(!agent.getOccurrences().containsKey(course)) {
                    List<Integer> _occurrences = new ArrayList<>();
                    _occurrences.add(responseEntity.getBody());
                    agent.getOccurrences().put(course, _occurrences);

                    List<Boolean> _feedbacks = new ArrayList<>();
                    _feedbacks.add(new Random().nextBoolean());
                    agent.getFeedbacks().put(course, _feedbacks);

                    agent.setBetaReputationRating(Formula.betaReputationRating(agent.getFeedbacks()));

                } else {
                    agent.getOccurrences().get(course).add(responseEntity.getBody());
                    agent.getFeedbacks().get(course).add(new Random().nextBoolean());
                    agent.setBetaReputationRating(Formula.betaReputationRating(agent.getFeedbacks()));
                }
            }
        }

        updateEvaluation(course);

        long end = System.currentTimeMillis();
        log.info("Evaluation time: " + (end - start) + " ms");

        response.put("agents", agents);
        response.put("evaluation", evaluation);
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
    public ResponseEntity<?> search(@RequestBody Preference preference) throws Exception {

        long start = System.currentTimeMillis();

        StringBuilder queryBuilder = new StringBuilder();

        // query must be constructed from preferences
        if(null != preference.getPeriod() && preference.getPeriod().size() > 0) {
            List<String> period = preference.getPeriod();

            queryBuilder.append("(Course and (");
            for(int i = 0; i < period.size(); i++) {
                queryBuilder.append("(isTaughtInPeriod value " + period.get(i) + ")");

                if(i != period.size() - 1)
                    queryBuilder.append(" or ");
            }
            queryBuilder.append("))");
        }

        if(null != preference.getDay() && preference.getDay().size() > 0) {
            List<String> day = preference.getDay();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            queryBuilder.append("(Course and (");
            for(int i = 0; i < day.size(); i++) {
                queryBuilder.append("(isTaughtOn some " + day.get(i) + ")");

                if(i != day.size() - 1)
                    queryBuilder.append(" or ");
            }
            queryBuilder.append("))");
        }

        if(null != preference.getTimeslot() && preference.getTimeslot().size() > 0) {
            List<String> timeslot = preference.getTimeslot();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            queryBuilder.append("(Course and (");
            for(int i = 0; i < timeslot.size(); i++) {
                queryBuilder.append("(isTaughtOn some " + timeslot.get(i) + ")");

                if(i != timeslot.size() - 1)
                    queryBuilder.append(" or ");
            }
            queryBuilder.append("))");
        }

        if(null != preference.getTopic() && preference.getTopic().size() > 0) {
            List<String> topic = preference.getTopic();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            queryBuilder.append("(Course and (");
            for(int i = 0; i < topic.size(); i++) {
                queryBuilder.append("(coversTopic value " + topic.get(i) + ")");

                if(i != topic.size() - 1)
                    queryBuilder.append(" or ");
            }
            queryBuilder.append("))");
        }

        if(null != preference.getLecturer() && preference.getLecturer().size() > 0) {
            List<String> lecturer = preference.getLecturer();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            queryBuilder.append("(Course and (");
            for(int i = 0; i < lecturer.size(); i++) {
                queryBuilder.append("(isTaughtBy value " + lecturer.get(i) + ")");

                if(i != lecturer.size() - 1)
                    queryBuilder.append(" or ");
            }
            queryBuilder.append("))");
        }

        List<Course> courseList = parseQuery(queryBuilder.toString());

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
                        message.append("Conflict in " + period + " on " + conflict + "<br>");
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

            log.info("Check the course's period and timeslot for " + courseName);

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

        Collections.sort(courseList);
        return courseList;
    }

    private String standarize(String input) {
        return input.replaceAll("_", " ");
    }

    private void updateEvaluation(String course) {
        double max = -2;
        int score = -1;

        for(String url : agents.keySet()) {
            Agent agent = agents.get(url);
            if(agent.getBetaReputationRating().containsKey(course)) {
                double rating = agent.getBetaReputationRating().get(course);

                if(rating > max) {
                    max = rating;
                    score = agent.getOccurrences().get(course).get(agent.getOccurrences().get(course).size() - 1);
                }
            }
        }

        Course courseObj = courses.get(course);
        courseObj.setEvaluation(score);

        evaluation.put(course, score);
    }

}
