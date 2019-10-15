package nl.uu.group8.courseplanner.repository;

import java.util.List;

public interface SesameRepository {

    List runSPARQL(String querystring);
}