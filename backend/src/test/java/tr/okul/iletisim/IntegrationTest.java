package tr.okul.iletisim;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTest {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcClient jdbc;

	@Autowired
	protected JsonMapper mapper;

	@Autowired
	PasswordEncoder encoder;

	protected TestData data;

	@BeforeEach
	void resetData() {
		data = new TestData(jdbc, encoder);
		data.clean();
	}

	protected RequestPostProcessor loginAs(long userId) {
		return request -> csrf().postProcessRequest(user(String.valueOf(userId)).postProcessRequest(request));
	}

	protected String json(Object obj) {
		return mapper.writeValueAsString(obj);
	}

}
