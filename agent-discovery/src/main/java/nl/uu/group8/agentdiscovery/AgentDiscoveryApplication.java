package nl.uu.group8.agentdiscovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaServer
@EnableEurekaClient
@SpringBootApplication
public class AgentDiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentDiscoveryApplication.class, args);
	}

}
