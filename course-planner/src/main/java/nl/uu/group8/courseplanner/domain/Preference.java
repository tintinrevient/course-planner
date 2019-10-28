package nl.uu.group8.courseplanner.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Preference {

    private List<String> period;
    private List<String> day;
    private List<String> timeslot;
    private List<String> topic;
    private List<String> lecturer;
    private List<String> deadline;
    private List<String> exam;
    private List<String> instruction;
    private List<String> research;
    private List<String> faculty;
}
