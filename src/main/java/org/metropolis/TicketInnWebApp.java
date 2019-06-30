package org.metropolis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class TicketInnWebApp {
    public static void main(String[] args) {
        SpringApplication.run(TicketInnWebApp.class);
    }
}
