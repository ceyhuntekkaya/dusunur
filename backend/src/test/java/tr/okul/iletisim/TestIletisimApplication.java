package tr.okul.iletisim;

import org.springframework.boot.SpringApplication;

public class TestIletisimApplication {

	public static void main(String[] args) {
		SpringApplication.from(IletisimApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
