package ardc.cerium.core.common.service;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.util.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Service
public class VocabService {
    Logger logger = LoggerFactory.getLogger(VocabService.class);
    private List<String> notations;
    protected final String ANZSRC_FOR_NOTATIONS_PATH = "for_notations.txt";

    /**
     * Loads all ANZSRC-FOR 2008 and 2020 notations listed in a text file
     * @throws Exception read file ardc.cerium.core.exception
     */
    public void loadNotations() throws Exception {
        String data = Helpers.readFileOnClassPath(ANZSRC_FOR_NOTATIONS_PATH);
        String[] aNotation = data.split(System.lineSeparator());
        this.notations = Arrays.asList(aNotation);
        logger.info("Loaded {} ANZSRC FOR Notations from {}", notations.size(), ANZSRC_FOR_NOTATIONS_PATH);
    }

    @PostConstruct
    public void init() throws Exception {
        loadNotations();
    }

    /**
     * checks if Notaion contained in the valid notaions list
     * @param notation the string value of a 2,4 or 6 digit code
     * @return true (if exists) | false (if doesn't exist)
     */
    public boolean isValidNotation(String notation){
        logger.debug("looking for  '{}' size '{}'",  notation, notations.size());
        return notations.contains(notation);
    }
}
