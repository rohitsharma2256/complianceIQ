package com.complianceiq;

import com.complianceiq.mcp.ComplianceTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComplianceIqApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComplianceIqApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider complianceToolCallbacks(ComplianceTools complianceTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(complianceTools)
				.build();
	}
}
