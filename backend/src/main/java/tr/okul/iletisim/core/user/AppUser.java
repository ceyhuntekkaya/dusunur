package tr.okul.iletisim.core.user;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tr.okul.iletisim.common.util.TextNormalizer;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "search_key")
	private String searchKey;

	private String phone;

	private String discipline;

	@Enumerated(EnumType.STRING)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "system_admin")
	private boolean systemAdmin;

	@Column(name = "kvkk_version")
	private int kvkkVersion;

	@Column(name = "kvkk_accepted_at")
	private OffsetDateTime kvkkAcceptedAt;

	@Column(name = "failed_login_count")
	private int failedLoginCount;

	@Column(name = "locked_until")
	private OffsetDateTime lockedUntil;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void computeSearchKey() {
		this.searchKey = TextNormalizer.fold(firstName + " " + lastName + " " + email);
	}

	public String fullName() {
		return firstName + " " + lastName;
	}

}
