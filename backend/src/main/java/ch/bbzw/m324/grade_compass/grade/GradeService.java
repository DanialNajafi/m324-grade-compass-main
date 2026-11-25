package ch.bbzw.m324.grade_compass.grade;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeService.class);
    public static final String DEFAULT_COMMENT = "none";

    private final GradeRepository repository;

    public GradeService(GradeRepository repository) {
        this.repository = repository;
    }

    public GradeResponse create(CreateGradeRequest request) {
        log.info("Creating grade for subject {}", request.subjectId());

        double value = request.value();

        if (value < 1 || value > 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade must be between 1 and 6");

        Grade grade = new Grade(request.subjectId(), Double.valueOf(Math.round(value * 100) / 100.0));
        grade.setComment(DEFAULT_COMMENT);

        grade = repository.save(grade);

        return GradeResponse.from(grade);
    }

    public List<GradeResponse> findAll() {
        List<GradeResponse> result = new ArrayList<>();
        for (Grade grade : repository.findAll()) {
            result.add(GradeResponse.from(grade));
        }
        return result;
    }
}
