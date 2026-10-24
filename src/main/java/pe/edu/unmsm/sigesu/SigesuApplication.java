package pe.edu.unmsm.sigesu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigesuApplication {
  public static void main(String[] args) {
    SpringApplication.run(SigesuApplication.class, args);
  }
}
