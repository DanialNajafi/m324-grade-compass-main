package ch.bbzw.m324.grade_compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GradeCompassApplication {

	public static void main(String[] args) {
		// Kommentar für Docker-Cache-Test
		SpringApplication.run(GradeCompassApplication.class, args);
	}

}
