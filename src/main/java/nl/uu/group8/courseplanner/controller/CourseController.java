package nl.uu.group8.courseplanner.controller;

import nl.uu.group8.courseplanner.repository.SesameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/")
public class CourseController {

    @Autowired
    SesameRepository repository;

    @GetMapping("/wine")
    public ResponseEntity<?> wine() {
        String prefix = "PREFIX wine:<http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>";
        String select = "SELECT ?wine WHERE {?wine wine:hasBody wine:Medium . }";

        List list = repository.runSPARQL(prefix + " " + select);

        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/course")
    public ResponseEntity<?> course() {

        String prefix_1 = "PREFIX course:<http://www.semanticweb.org/thomas/ontologies/2019/9/course-planner#>";
        String prefix_2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
        String select = "SELECT ?course WHERE {?course rdf:type course:Course . }";

        List list = repository.runSPARQL(prefix_1 + " " + prefix_2 + " " + select);

        return ResponseEntity.ok().body(list);
    }

}
