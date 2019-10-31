package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.domain.Agent;
import nl.uu.group8.courseplanner.domain.Course;
import nl.uu.group8.courseplanner.domain.Preference;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import nl.uu.group8.courseplanner.service.BreadthFirstSearch;
import nl.uu.group8.courseplanner.util.Formula;
import nl.uu.group8.courseplanner.domain.Node;
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
    private BreadthFirstSearch breadthFirstSearch;

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

    // depth of the topic tree
    private int depth = 1;


    @PostMapping("/ask-eval")
    public ResponseEntity<?> ask(@RequestBody String selected) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();

        String course = selected.replaceAll("\"", "");

        log.info("Start to evaluate course: " + course);

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
    public ResponseEntity<?> search(@RequestBody List<Preference> preferences) throws Exception {

        long start = System.currentTimeMillis();

        Map response = new HashMap();
        StringBuilder message = new StringBuilder();

        List<Course> courseList = new ArrayList<>();

        int periodIndex = 1;
        if(null != preferences && preferences.size() > 0) {
            for(Preference preference : preferences) {
                String query = parsePreference(preference);

                Map map = parseQuery(query);
                courseList.addAll((List<Course>) map.get("courseList"));
                message.append("Utility for Period " + periodIndex + ": " + (Double) map.get("utility") + "<br>");
                message.append("Preferences for Period " + periodIndex + ": " + map.get("preferences") + "<br>");
                periodIndex++;
            }
        }

        for(Course course : courseList) {
            String period = course.getPeriod();
            Set<String> timeslot = course.getTimeslot();

            if(null == availability.get(period) || availability.get(period).size() == 0) {
                availability.put(period, new HashSet<>(Arrays.asList(course)));
                continue;
            }

            if(availability.get(period).size() >= getMax()) {
//                message.append("Number of registered courses for " + period + " is over " + getMax() + "<br>");
                continue;
            }

            //verify timeslot conflict
            boolean flag = false;
            for(Course registeredCourse : availability.get(period)) {
                Set<String> intersection = new HashSet<>(registeredCourse.getTimeslot());
                intersection.retainAll(timeslot);
                if(intersection.size() > 0) {
                    flag = true;
//                    for(String conflict : intersection) {
//                        message.append("Conflict in " + period + " on " + conflict + "<br>");
//                    }
                    break;
                }
            }

            if(!flag)
                availability.get(period).add(course);

            if(availability.get(period).size() >= getMax()) {
                message.append("Number of registered courses for " + period + " is over " + getMax() + "<br>");
                continue;
            }
        }

        long end = System.currentTimeMillis();
        log.info("Search time: " + (end - start) + " ms");
        message.append("Total search time: " + (end - start)/1000 + " seconds. <br>");

        response.put("courses", availability);
        response.put("msg", message.toString());

        return ResponseEntity.ok().body(response);
    }

    private Map parseQuery(String query) {

        List<Course> courseList = new ArrayList<>();

        Node node = breadthFirstSearch.search(query, getMax());

        Set<OWLNamedIndividual> individuals = node.getCourseInstances();
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

        Map<String, Object> response = new HashMap<>();
        response.put("courseList", courseList);
        response.put("utility", node.getUtility());
        response.put("preferences", node.getPreferences());

        return response;
    }

    private List<String> parseTopic(List<String> topic) {
        Set<String> topicSet = new HashSet<>(topic);

        for(int level = 0; level < depth; level++) {

            log.info("Topic tree search on depth " + level);

            for(String parentTopic : topicSet) {

                String query = "isPartOf value " + parentTopic;
                Set<OWLNamedIndividual> individuals = engine.getInstances(query, false);

                for (OWLEntity entity : individuals) {
                    String childTopic = shortFormProvider.getShortForm(entity);
                    log.info("Depth ["  + level + "] Child topic [" + childTopic + "] has been added");
                    topicSet.add(childTopic);
                }
            }
        }

        return new ArrayList<>(topicSet);
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

    private void setMax(int max) {
        this.max = max;
    }
    private int getMax(){
        return this.max;
    };

    private String parsePreference(Preference preference) {
        StringBuilder queryBuilder = new StringBuilder();

        // Preference - Period
        if(null != preference.getPeriod()) {
            String period = preference.getPeriod();

            log.info("Search courses in " + period);

            queryBuilder.append("(isTaughtInPeriod value " + period + ")");
        }

        // Preference - Day of Week
        if(null != preference.getDay() && preference.getDay().size() > 0) {
            List<String> day = preference.getDay();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < day.size(); i++) {
                queryBuilder.append("(isTaughtOn some " + day.get(i) + ")");

                if(i != day.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Time Slot
        if(null != preference.getTimeslot() && preference.getTimeslot().size() > 0) {
            List<String> timeslot = preference.getTimeslot();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < timeslot.size(); i++) {
                queryBuilder.append("(isTaughtOn some " + timeslot.get(i) + ")");

                if(i != timeslot.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Topic
        if(null != preference.getTopic() && preference.getTopic().size() > 0) {
            List<String> topic = preference.getTopic();
            topic = parseTopic(topic);

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < topic.size(); i++) {
                queryBuilder.append("(coversTopic value " + topic.get(i) + ")");

                if(i != topic.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Lecturer
        if(null != preference.getLecturer() && preference.getLecturer().size() > 0) {
            List<String> lecturer = preference.getLecturer();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < lecturer.size(); i++) {
                queryBuilder.append("(isTaughtBy value " + lecturer.get(i) + ")");

                if(i != lecturer.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Deadline
        if(null != preference.getDeadline() && preference.getDeadline().size() > 0) {
            List<String> deadline = preference.getDeadline();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < deadline.size(); i++) {
                queryBuilder.append("(hasDeadlines value " + deadline.get(i) + ")");

                if(i != deadline.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Exam Form
        if(null != preference.getExam() && preference.getExam().size() > 0) {
            List<String> exam = preference.getExam();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < exam.size(); i++) {
                queryBuilder.append("(hasExamForm value " + exam.get(i) + ")");

                if(i != exam.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Instructional Format
        if(null != preference.getInstruction() && preference.getInstruction().size() > 0) {
            List<String> instruction = preference.getInstruction();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < instruction.size(); i++) {
                queryBuilder.append("(hasInstructionalFormat value " + instruction.get(i) + ")");

                if(i != instruction.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Research Methodology
        if(null != preference.getResearch() && preference.getResearch().size() > 0) {
            List<String> research = preference.getResearch();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < research.size(); i++) {
                queryBuilder.append("(usesMethodology value " + research.get(i) + ")");

                if(i != research.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Faculty
        if(null != preference.getFaculty() && preference.getFaculty().size() > 0) {
            List<String> faculty = preference.getFaculty();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < faculty.size(); i++) {
                queryBuilder.append("(isOfferedBy value " + faculty.get(i) + ")");

                if(i != faculty.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Location
        if(null != preference.getLocation() && preference.getLocation().size() > 0) {
            List<String> location = preference.getLocation();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < location.size(); i++) {
                queryBuilder.append("(isOfferedBy some (isLocatedAt value " + location.get(i) + "))");

                if(i != location.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Communication
        if(null != preference.getCommunication() && preference.getCommunication().size() > 0) {
            List<String> communication = preference.getCommunication();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < communication.size(); i++) {
                queryBuilder.append("isTaughtBy some (hasSkill value " + communication.get(i) + ")");

                if(i != communication.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Freedom
        if(null != preference.getFreedom() && preference.getFreedom().size() > 0) {
            List<String> freedom = preference.getFreedom();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < freedom.size(); i++) {
                queryBuilder.append("isTaughtBy some (hasSkill value " + freedom.get(i) + ")");

                if(i != freedom.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Guidance
        if(null != preference.getGuidance() && preference.getGuidance().size() > 0) {
            List<String> guidance = preference.getGuidance();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < guidance.size(); i++) {
                queryBuilder.append("isTaughtBy some (hasSkill value " + guidance.get(i) + ")");

                if(i != guidance.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Organizing
        if(null != preference.getOrganizing() && preference.getOrganizing().size() > 0) {
            List<String> organizing = preference.getOrganizing();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < organizing.size(); i++) {
                queryBuilder.append("isTaughtBy some (hasSkill value " + organizing.get(i) + ")");

                if(i != organizing.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Speaking
        if(null != preference.getSpeaking() && preference.getSpeaking().size() > 0) {
            List<String> speaking = preference.getSpeaking();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < speaking.size(); i++) {
                queryBuilder.append("isTaughtBy some (hasSkill value " + speaking.get(i) + ")");

                if(i != speaking.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        // Preference - Skill
        if(null != preference.getSkill() && preference.getSkill().size() > 0) {
            List<String> skill = preference.getSkill();

            if(!queryBuilder.toString().isEmpty())
                queryBuilder.append(" and ");

            for(int i = 0; i < skill.size(); i++) {

                if(skill.contains("Programming"))
                    queryBuilder.append("(usesSkill some " + skill.get(i) + ")");
                else
                    queryBuilder.append("(usesSkill value " + skill.get(i) + ")");

                if(i != skill.size() - 1)
                    queryBuilder.append(" or ");
            }
        }

        return queryBuilder.toString();
    }

}