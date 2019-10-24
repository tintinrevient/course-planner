package nl.uu.group8.courseplanner.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent {
    private String address;

    // mapping: course ID -> beta reputation rating
    private Map<String, Double> betaReputationRating;

    // mapping: course ID -> evaluation occurrences
    private Map<String, List<Integer>> occurrences;

    // mapping: course Id - > evaluation feedbacks
    private Map<String, List<Boolean>> feedbacks;
}
