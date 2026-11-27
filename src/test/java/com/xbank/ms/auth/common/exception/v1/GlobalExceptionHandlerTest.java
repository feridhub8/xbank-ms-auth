package com.xbank.ms.auth.common.exception.v1;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** specs/api-errors cases that need an endpoint, through a test-only controller. */
class GlobalExceptionHandlerTest {

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

	@Test
	void everyInvalidFieldIsListed() throws Exception {
		mockMvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors", hasSize(2)))
				.andExpect(jsonPath("$.errors[?(@.field == 'first')].message").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'second')].message").exists());
	}

	@Test
	void invalidJsonIsMalformed() throws Exception {
		mockMvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void unexpectedErrorHidesInternals() throws Exception {
		mockMvc.perform(get("/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
				.andExpect(content().string(not(containsString("secret internal message"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	void unreachableUpstreamIs503() throws Exception {
		mockMvc.perform(get("/upstream"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UPSTREAM_UNAVAILABLE"));
	}

	@RestController
	static class TestController {

		@PostMapping("/validated")
		void validated(@Valid @RequestBody TestBody body) {
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("secret internal message");
		}

		@GetMapping("/upstream")
		void upstream() {
			throw new UpstreamUnavailableException("Identity provider");
		}
	}

	@Getter
	@Setter
	static class TestBody {

		@NotBlank
		private String first;

		@NotBlank
		private String second;
	}
}
