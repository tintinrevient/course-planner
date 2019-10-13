package nl.uu.group8.courseplanner.repository;

import org.openrdf.rio.RDFFormat;

import java.util.List;

public interface SesameRepository {

    void addFile(String filepath, RDFFormat format);

    List runSPARQL(String querystring);
}
