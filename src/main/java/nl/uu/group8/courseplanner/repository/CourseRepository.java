package nl.uu.group8.courseplanner.repository;

import nl.uu.group8.courseplanner.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

}
