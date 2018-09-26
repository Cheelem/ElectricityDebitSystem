package cn.edu.neu.electricity;

import cn.edu.neu.electricity.database.DatabaseController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.SQLException;

@SpringBootApplication
@EnableScheduling
public class ElectricityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElectricityApplication.class, args);
        try {
            DatabaseController.init();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
