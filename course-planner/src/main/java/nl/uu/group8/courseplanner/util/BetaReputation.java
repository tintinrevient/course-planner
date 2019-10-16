package nl.uu.group8.courseplanner.util;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class BetaReputation {

    public static Map<String, Double> reputationRating(Map<String, List<Boolean>> feedback) {

        Map<String, Double> rating = new HashMap<>();

        if(feedback.keySet().size() == 0)
            return rating;

        for(String url : feedback.keySet()) {
            for(Boolean _feedback : feedback.get(url)) {
                double r = 0;
                double s = 0;

                if(_feedback)
                    r++;
                else
                    s++;

                double _rating = ((r+1)/(r+s+2) - 0.5)*2;
                rating.put(url, _rating);
            }
        }

        return rating.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }
}
