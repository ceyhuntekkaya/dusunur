package tr.okul.iletisim.common.notify;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import tr.okul.iletisim.AppProperties;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	public static final String USER_DESTINATION = "/user/queue/events";

	private final AppProperties app;

	public WebSocketConfig(AppProperties app) {
		this.app = app;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		List<String> origins = app.allowedOrigins();
		registry.addEndpoint("/ws").setAllowedOriginPatterns(origins.toArray(new String[0]));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.setDaemon(true);
		scheduler.initialize();
		registry.enableSimpleBroker("/queue").setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(scheduler);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelGuard());
	}

	static class ChannelGuard implements ChannelInterceptor {

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (accessor == null || accessor.getCommand() == null) {
				return message;
			}
			StompCommand command = accessor.getCommand();
			if (StompCommand.CONNECT.equals(command) && accessor.getUser() == null) {
				throw new MessagingException("Oturum doğrulanamadı.");
			}
			if (StompCommand.SUBSCRIBE.equals(command) && !USER_DESTINATION.equals(accessor.getDestination())) {
				throw new MessagingException("Bu kanala abone olunamaz.");
			}
			if (StompCommand.SEND.equals(command)) {
				throw new MessagingException("Bu kanala mesaj gönderilemez.");
			}
			return message;
		}

	}

}
