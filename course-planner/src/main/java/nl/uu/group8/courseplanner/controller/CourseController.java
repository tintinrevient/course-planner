package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.domain.Course;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.util.BetaReputation;
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
    private Set<String> courses = new HashSet<>();
    private int max = 3;

    @GetMapping("/ask-eval")
    public int ask(@RequestParam String course) throws Exception {

        String localServerUrl = InetAddress.getLocalHost().getHostAddress() + ":" + environment.getProperty("local.server.port");
        log.info("local server URL: " + localServerUrl);
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(serviceId);

        for(ServiceInstance serviceInstance : serviceInstanceList) {
            String host = serviceInstance.getHost();
            int port = serviceInstance.getPort();
            String url = host + ":" + port;

            if(null != url && url.equalsIgnoreCase(localServerUrl))
                continue;

            String serviceUrl = "http://" + url + "/course/answer-eval?course=" + course;
            ResponseEntity<Integer> responseEntity = restTemplate.getForEntity(serviceUrl, Integer.class);

            log.info("Ask the evaluation from " + url);

            if(!worldStates.containsKey(url)) {
                List<Integer> occurrences = new ArrayList<>();
                occurrences.add(responseEntity.getBody());
                worldStates.put(url, occurrences);

                List<Boolean> feedbacks = new ArrayList<>();
                feedbacks.add(new Random().nextBoolean());
                feedback.put(url, feedbacks);

            } else {
                worldStates.get(url).add(responseEntity.getBody());
                feedback.get(url).add(new Random().nextBoolean());
            }
        }

        Map<String, Double> rating = BetaReputation.reputationRating(feedback);

        log.info("World state ...");
        for(String url : worldStates.keySet())
            log.info(url + ":" + worldStates.get(url));

        log.info("Feedback ...");
        for(String url : feedback.keySet())
            log.info(url + ":" + feedback.get(url));

        log.info("Rating ...");
        for(String url : rating.keySet())
            log.info(url + ":" + rating.get(url));

        if(rating.keySet().size() == 0)
            return 0;
        else {
            List<Integer> scoreList = worldStates.get(rating.entrySet().iterator().next().getKey());
            return scoreList.get(scoreList.size()-1);
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

        return ResponseEntity.ok().body(courseList);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody List<String> selected) throws Exception {

        if(courses.size() >= max) {
            log.info("Number of registered courses is over " + max);
            return ResponseEntity.ok().body(courses);
        }

        Set<String> availability = new HashSet<>();
        for(String course : courses) {
            String[] parts = course.split("[|]");

            for(int i = 1; i < parts.length; i++) {
                availability.add(parts[i]);
            }
        }

        log.info("Availability: " + availability.toString());

        for(String select : selected) {
            String[] parts = select.split("[|]");

            boolean conflict = false;
            for(int i = 1; i < parts.length; i++) {
                if(availability.contains(parts[i])) {
                    log.info("Conflict: " + parts[i]);
                    conflict = true;
                    break;
                }
            }

            if(!conflict)
                courses.add(select);

            if(courses.size() >= max) {
                log.info("Number of registered courses is over " + max);
                break;
            }
        }

        log.info("Registered courses: " + courses.toString());

        return ResponseEntity.ok().body(courses);
    }

}
