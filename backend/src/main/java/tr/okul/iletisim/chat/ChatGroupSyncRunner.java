package tr.okul.iletisim.chat;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class ChatGroupSyncRunner implements ApplicationRunner {

	private final ChatGroupSync sync;

	public ChatGroupSyncRunner(ChatGroupSync sync) {
		this.sync = sync;
	}

	@Override
	public void run(ApplicationArguments args) {
		sync.syncMissing();
	}

}
