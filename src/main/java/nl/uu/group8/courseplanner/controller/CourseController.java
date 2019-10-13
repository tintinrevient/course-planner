package nl.uu.group8.courseplanner.controller;

import nl.uu.group8.courseplanner.repository.SesameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    SesameRepository repository;

    @GetMapping("/test")
    public ResponseEntity<?> test() {

        StringBuilder stringBuilder = new StringBuilder();

        List list = repository.runSPARQL("PREFIX wine:<http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#> SELECT ?wine WHERE {?wine wine:hasBody wine:Medium . }");

        return ResponseEntity.ok().body(list);
    }

}
