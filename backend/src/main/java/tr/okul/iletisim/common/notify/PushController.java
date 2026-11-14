package tr.okul.iletisim.common.notify;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import tr.okul.iletisim.common.notify.NotifyDtos.PushSubscriptionRequest;
import tr.okul.iletisim.common.notify.NotifyDtos.PushUnsubscribeRequest;
import tr.okul.iletisim.common.security.UserContext;

@RestController
@RequestMapping("/api/push/subscriptions")
public class PushController {

	private final PushSubscriptionService service;

	public PushController(PushSubscriptionService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribe(UserContext me, @RequestBody PushSubscriptionRequest request,
			@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
		service.subscribe(me, request, userAgent);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(UserContext me, @RequestBody PushUnsubscribeRequest request) {
		service.unsubscribe(me, request.endpoint());
	}

}
