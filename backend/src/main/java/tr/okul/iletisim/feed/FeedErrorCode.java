package tr.okul.iletisim.feed;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum FeedErrorCode implements ErrorCode {

	FEED_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Gönderi bulunamadı veya silinmiş."),
	FEED_EMPTY_POST(HttpStatus.BAD_REQUEST, "Paylaşım için bir metin yazın veya dosya ekleyin."),
	FEED_TOO_MANY_FILES(HttpStatus.BAD_REQUEST, "Bir paylaşıma en fazla 10 dosya eklenebilir."),
	FEED_TOO_MANY_TARGETS(HttpStatus.BAD_REQUEST, "En fazla 50 alıcı seçebilirsiniz. Daha geniş bir kitle için grup veya şube seçin."),
	FEED_IMPORTANT_FORBIDDEN(HttpStatus.FORBIDDEN, "Önemli duyuruyu yalnızca yöneticiler yayınlayabilir."),
	FEED_GROUP_NOT_MEMBER(HttpStatus.FORBIDDEN, "Yalnızca üyesi olduğunuz gruplarla paylaşım yapabilirsiniz."),
	FEED_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "Yalnızca kendi gönderinizi düzenleyebilirsiniz."),
	FEED_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu gönderiyi silme yetkiniz yok."),
	FEED_PIN_FORBIDDEN(HttpStatus.FORBIDDEN, "Gönderileri yalnızca yöneticiler sabitleyebilir."),
	FEED_PIN_LIMIT(HttpStatus.CONFLICT, "En fazla 3 gönderi sabitlenebilir. Önce sabitlenmiş gönderilerden birini kaldırın."),
	FEED_NOT_IMPORTANT(HttpStatus.BAD_REQUEST, "Bu işlem yalnızca önemli duyurular için yapılabilir."),
	FEED_READS_FORBIDDEN(HttpStatus.FORBIDDEN, "Okuma durumunu yalnızca duyuruyu yayınlayan kişi görebilir."),
	FEED_REMIND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "Hatırlatma 24 saatte bir gönderilebilir. Bir sonraki hatırlatmayı {0} sonrasında gönderebilirsiniz."),
	FEED_REMIND_NONE(HttpStatus.BAD_REQUEST, "Duyuruyu okumayan kimse kalmadı."),
	FEED_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Yorum bulunamadı veya silinmiş."),
	FEED_COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu yorumu silme yetkiniz yok.");

	private final HttpStatus status;
	private final String message;

	FeedErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}

}
