package nl.uu.group8.courseplanner.controller;

import lombok.extern.slf4j.Slf4j;
import nl.uu.group8.courseplanner.service.DLQueryEngine;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/ask-eval")
    public int ask(@RequestParam String course) throws Exception {
        ResponseEntity<Integer> responseEntity = restTemplate.getForEntity("http://145.107.86.180:8081/course/answer-eval?course="+course, Integer.class);
        return responseEntity.getBody();
    }

    @GetMapping("/answer-eval")
    public int answer(HttpServletRequest request, @RequestParam String course) throws Exception {
        log.info("Request coming from " + request.getRemoteAddr());
        return 1;
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody String query) throws Exception {

        List instanceList = new ArrayList<String>();
        Set<OWLNamedIndividual> individuals = engine.getInstances(query, false);
        for (OWLEntity entity : individuals) {
            instanceList.add(shortFormProvider.getShortForm(entity));
        }

        List superClassList = new ArrayList<String>();
        Set<OWLClass> superClasses = engine.getSuperClasses(query, false);
        for (OWLClass class_ : superClasses) {
            superClassList.add(shortFormProvider.getShortForm(class_));
        }

        List subClassList = new ArrayList<String>();
        Set<OWLClass> subClasses = engine.getSubClasses(query, false);
        for (OWLClass class_ : subClasses) {
            subClassList.add(shortFormProvider.getShortForm(class_));
        }

        Map map = new HashMap<>();
        map.put("Instances"+"["+instanceList.size()+"]", instanceList);
        map.put("SuperClasses"+"["+superClassList.size()+"]", superClassList);
        map.put("SubClasses"+"["+subClassList.size()+"]", subClassList);

        return ResponseEntity.ok().body(map);
    }

}
