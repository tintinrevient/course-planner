package nl.uu.group8.courseplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import nl.uu.group8.courseplanner.domain.Course;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private nl.uu.group8.courseplanner.repository.CourseRepository courseRepository;

    @GetMapping(value = "/{id}")
    public Course find(@PathVariable("id") Long id) {
        return courseRepository.findById(id).get();
    }

}
