package tr.okul.iletisim.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.internet.MimeMessage;
import tr.okul.iletisim.AppProperties;

@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final AppProperties app;

	public MailService(JavaMailSender mailSender, AppProperties app) {
		this.mailSender = mailSender;
		this.app = app;
	}

	public boolean sendSetupLink(String to, String firstName, String link, boolean passwordSet) {
		String org = app.institutionName();
		String subject;
		String text;
		if (passwordSet) {
			subject = org + " İletişim – Şifre yenileme bağlantısı";
			text = "Merhaba " + firstName + ", okul yönetimi sizin için bir şifre yenileme bağlantısı oluşturdu: "
					+ link + " Bağlantı " + app.auth().setupTokenHours() + " saat geçerlidir.";
		}
		else {
			subject = org + " İletişim – Hesabınız oluşturuldu";
			text = "Merhaba " + firstName + ", " + org + " iç iletişim sistemine hesabınız oluşturuldu. "
					+ "Kullanıcı adınız: " + to + ". Şifrenizi belirlemek için bağlantıya tıklayın: " + link
					+ " Bağlantı " + app.auth().setupTokenHours() + " saat geçerlidir. "
					+ "Şifrenizi belirledikten sonra " + app.baseUrl() + " adresinden giriş yapabilirsiniz.";
		}
		return send(to, subject, text, link);
	}

	@Async
	public void sendResetLink(String to, String firstName, String link) {
		String text = "Merhaba " + firstName + ", hesabınız için şifre yenileme talebinde bulunuldu. "
				+ "Yeni şifre belirlemek için: " + link + " Bağlantı " + app.auth().resetTokenMinutes()
				+ " dakika geçerlidir. Bu talebi siz yapmadıysanız bu e-postayı dikkate almayın; şifreniz değişmez.";
		send(to, app.institutionName() + " İletişim – Şifre yenileme", text, link);
	}

	private boolean send(String to, String subject, String text, String link) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(app.mailFrom());
			helper.setTo(to);
			helper.setSubject(subject);
			String escaped = HtmlUtils.htmlEscape(text);
			String html = "<p>" + escaped.replace(HtmlUtils.htmlEscape(link),
					"<a href=\"" + HtmlUtils.htmlEscape(link) + "\">" + HtmlUtils.htmlEscape(link) + "</a>") + "</p>";
			helper.setText(text, html);
			mailSender.send(message);
			return true;
		}
		catch (Exception e) {
			log.error("E-posta gönderilemedi ({}): {}", to, e.toString());
			return false;
		}
	}

}
