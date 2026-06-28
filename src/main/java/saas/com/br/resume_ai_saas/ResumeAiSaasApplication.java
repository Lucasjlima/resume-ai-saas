package saas.com.br.resume_ai_saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ResumeAiSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAiSaasApplication.class, args);
    }

}
