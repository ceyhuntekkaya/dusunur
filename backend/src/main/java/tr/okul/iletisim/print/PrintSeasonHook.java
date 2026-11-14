package tr.okul.iletisim.print;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.core.spi.SeasonActivationHook;

@Component
public class PrintSeasonHook implements SeasonActivationHook {

	private final PrintQueries queries;
	private final PrintRequestService service;
	private final JdbcClient jdbc;

	public PrintSeasonHook(PrintQueries queries, PrintRequestService service, JdbcClient jdbc) {
		this.queries = queries;
		this.service = service;
		this.jdbc = jdbc;
	}

	@Override
	public List<String> preview(long closingSeasonId) {
		int n = queries.openCount(closingSeasonId);
		if (n == 0) {
			return List.of();
		}
		String name = jdbc.sql("SELECT name FROM season WHERE id = :id").param("id", closingSeasonId)
				.query(String.class).single();
		return List.of("%s yılına ait %d açık fotokopi talebi iptal edilecek.".formatted(name, n));
	}

	@Override
	public void onActivated(long closedSeasonId, long newSeasonId) {
		service.cancelOpenForSeason(closedSeasonId);
	}

}
