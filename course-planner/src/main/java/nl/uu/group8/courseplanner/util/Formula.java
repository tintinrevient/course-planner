package nl.uu.group8.courseplanner.util;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class Formula {

    public static Map<String, Double> betaReputationRating(Map<String, List<Boolean>> feedback) {

        Map<String, Double> rating = new HashMap<>();

        if(feedback.keySet().size() == 0)
            return rating;

        for(String key : feedback.keySet()) {
            double r = 0;
            double s = 0;

            for(Boolean _feedback : feedback.get(key)) {
                if(_feedback)
                    r++;
                else
                    s++;
            }

            double _rating = ((r+1)/(r+s+2) - 0.5)*2;
            rating.put(key, _rating);
        }

        return rating;
    }

}
