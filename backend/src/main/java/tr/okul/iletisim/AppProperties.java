package tr.okul.iletisim;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
		String institutionName,
		String baseUrl,
		List<String> allowedOrigins,
		String mailFrom,
		Storage storage,
		Files files,
		Kvkk kvkk,
		Auth auth,
		Push push,
		BootstrapAdmin bootstrapAdmin,
		Print print,
		Dms dms,
		Cleanup cleanup) {

	public record Storage(String root) {
	}

	public record Files(int imageMaxMb, int videoMaxMb, int documentMaxMb, int dmsMaxMb, int calendarMaxMb) {
	}

	public record Kvkk(int version) {
	}

	public record Auth(int lockThreshold, int lockMinutes, int resetTokenMinutes, int setupTokenHours,
			int forgotLimitPerHour) {
	}

	public record Push(String vapidPublicKey, String vapidPrivateKey, String subject) {
	}

	public record BootstrapAdmin(String email, String password) {
	}

	public record Print(int fileRetentionDays, int minLeadMinutes, int maxDaysAhead, int maxDelegationDays) {
	}

	public record Dms(int trashRetentionDays) {
	}

	public record Cleanup(int orphanFileHours, int notificationRetentionDays) {
	}

}
