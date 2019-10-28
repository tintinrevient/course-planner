package nl.uu.group8.courseplanner.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course implements Comparable<Course>{

    private String id;
    private String name;
    private String period;
    private Set<String> timeslot;
    private String lecturer;
    private String deadline;
    private int evaluation;

    @Override
    public int compareTo(Course course) {
        return this.getPeriod().compareTo(course.getPeriod());
    }

}
