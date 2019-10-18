package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
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

    private Map<String, List<Integer>> worldStates = new HashMap<>();
    private Map<String, List<Boolean>> feedback = new HashMap<>();
    private Map<String, Integer> evaluation = new HashMap<>();
    private Set<String> courses = new HashSet<>();
    private int max = 3;

    @PostMapping("/ask-eval")
    public ResponseEntity<?> ask(@RequestBody List<String> selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();
        List<String> msgList = new ArrayList<>();

        String course = selected.get(0).split("[|]")[0];

        if(selected.size() > 1) {
            String msg = "Evaluation can only be applied to one course!";
            log.info(msg);
            msgList.add(msg);
            response.put("msg", msgList);

            return ResponseEntity.ok().body(response);
        }

        String localServerUrl = InetAddress.getLocalHost().getHostAddress() + ":" + environment.getProperty("local.server.port");
        log.info("local server URL: " + localServerUrl);
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(serviceId);

        if(serviceInstanceList.size() <= 1) {
            String msg = "No neighbouring agents are online!";
            msgList.add(msg);
            response.put("msg", msgList);

            int score = -1;
            for(String key : evaluation.keySet()) {
                if(key.contains(course))
                    score = evaluation.get(key);
            }

            if(score != -1)
                response.put("score", course + " -> " + score);
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

        Map<String, Double> rating = Formula.betaReputationRating(feedback);

        log.info("World state ...");
        for(String key : worldStates.keySet())
            log.info(key + ":" + worldStates.get(key));

        log.info("Feedback ...");
        for(String key : feedback.keySet())
            log.info(key + ":" + feedback.get(key));

        log.info("Rating ...");
        for(String key : rating.keySet()){
            String msg = key + " -> Rating: " + rating.get(key);
            log.info(msg);

            msgList.add(msg);
        }
        response.put("msg", msgList);

        long end = System.currentTimeMillis();
        log.info("Evaluation time: " + (end - start) + " ms");

        if(rating.keySet().size() == 0) {
            String msg = "Neighbouring agents haven't any feedback for reference!";
            msgList.add(msg);
            response.put("msg", msgList);
            return ResponseEntity.ok().body(response);
        } else {
            double maxRating = -2;
            String key = "";

            for(String _key : rating.keySet()) {
                if(_key.contains(course)) {
                    if(rating.get(_key) > maxRating) {
                        maxRating = rating.get(_key);
                        key = _key;
                    }
                }
            }

            if(key.isEmpty()) {
                String msg = "Neighbouring agents haven't any feedback for reference!";
                msgList.add(msg);
                response.put("msg", msgList);
                return ResponseEntity.ok().body(response);
            }

            List<Integer> scoreList = worldStates.get(key);
            int score = scoreList.get(scoreList.size()-1);
            evaluation.put(course, score);

            response.put("score", course + " -> " + score);
            return ResponseEntity.ok().body(response);
        }
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
            String _query = "hasCourse value " + name;

            Set<String> timeslot = new HashSet<>();
            Set<OWLNamedIndividual> _individuals = engine.getInstances(_query, false);
            for (OWLEntity _entity : _individuals) {
                timeslot.add(shortFormProvider.getShortForm(_entity));
            }

            courseList.add(name + "|" + String.join("|", timeslot));
        }

        long end = System.currentTimeMillis();
        log.info("Search time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(courseList);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody List<String> selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();

        if(courses.size() >= max) {
            String msg = "Number of registered courses is over " + max;
            log.info(msg);

            response.put("msg", msg);
            response.put("courses", courses);
            return ResponseEntity.ok().body(response);
        }

        Set<String> availability = new HashSet<>();
        availability = updateAvailability(availability);

        log.info("Availability: " + availability.toString());

        for(String select : selected) {
            String[] parts = select.split("[|]");

            boolean conflict = false;
            for(int i = 1; i < parts.length; i++) {
                if(availability.contains(parts[i])) {
                    String msg = "Conflict: " + parts[i];
                    log.info(msg);
                    response.put("msg", msg);

                    conflict = true;
                    break;
                }
            }

            if(!conflict) {
                courses.add(select);
                availability = updateAvailability(availability);
            }

            if(courses.size() >= max) {
                String msg = "Number of registered courses is over " + max;
                log.info(msg);

                response.put("msg", msg);
                break;
            }
        }

        log.info("Registered courses: " + courses.toString());

        response.put("courses", courses);

        long end = System.currentTimeMillis();
        log.info("Register time: " + (end - start) + " ms");

        return ResponseEntity.ok().body(response);
    }

    private Set<String> updateAvailability(Set<String> availability) {
        for(String course : courses) {
            String[] parts = course.split("[|]");

            for(int i = 1; i < parts.length; i++) {
                availability.add(parts[i]);
            }
        }

        return availability;
    }

}
