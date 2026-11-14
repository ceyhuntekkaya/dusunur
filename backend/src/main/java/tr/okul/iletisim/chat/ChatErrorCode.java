package tr.okul.iletisim.chat;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum ChatErrorCode implements ErrorCode {

	CHAT_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Sohbet bulunamadı veya bu sohbete erişiminiz yok."),
	CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Mesaj bulunamadı."),
	CHAT_DIRECT_SELF(HttpStatus.BAD_REQUEST, "Kendinizle sohbet başlatamazsınız."),
	CHAT_USER_INACTIVE(HttpStatus.BAD_REQUEST, "Bu kişi aktif personel olmadığı için mesaj gönderilemez."),
	CHAT_READ_ONLY(HttpStatus.CONFLICT, "{0}"),
	CHAT_EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "Boş mesaj gönderilemez. Bir metin yazın veya dosya ekleyin."),
	CHAT_TOO_MANY_FILES(HttpStatus.BAD_REQUEST, "Bir mesaja en fazla 10 dosya eklenebilir."),
	CHAT_REPLY_INVALID(HttpStatus.BAD_REQUEST, "Yanıtlamak istediğiniz mesaj bulunamadı veya silinmiş."),
	CHAT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "Yalnızca kendi mesajlarınızı silebilirsiniz."),
	CHAT_GROUP_TOO_SMALL(HttpStatus.BAD_REQUEST,
			"Grup sohbeti için en az 2 kişi seçin. Tek kişiyle yazışmak için birebir sohbet açın."),
	CHAT_GROUP_TOO_LARGE(HttpStatus.BAD_REQUEST, "Bir grup sohbetinde en fazla 200 kişi olabilir."),
	CHAT_MEMBER_INVALID(HttpStatus.BAD_REQUEST, "Seçilen kişilerden bazıları bu yıl aktif personel değil."),
	CHAT_MANAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu sohbeti yalnızca sohbeti oluşturan kişi yönetebilir."),
	CHAT_USE_LEAVE(HttpStatus.BAD_REQUEST, "Sohbetten çıkmak için \"Sohbetten ayrıl\" seçeneğini kullanın."),
	CHAT_LEAVE_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
			"Bu sohbetten ayrılamazsınız. Grup sohbetlerinin üyeliği grup yönetiminden belirlenir.");

	private final HttpStatus status;
	private final String message;

	ChatErrorCode(HttpStatus status, String message) {
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
