package com.isekai.ssgserver.member.entity;

import com.isekai.ssgserver.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Getter
@Table(name = "member")
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long memberId;

	@Column(unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "account_id", nullable = false)
	private String accountId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "phone", nullable = false)
	private String phone;

	@Column(name = "address", nullable = false)
	private String address;

	@Column(name = "gender", nullable = false)
	private byte gender;

	@Column(name = "is_withdraw", nullable = false)
	private byte isWithdraw;

	// @Column(name = "withdraw_at")
	// private LocalDateTime withdrawAt;
	@Builder
	public Member(String uuid, String accountId, String name, String password, String email, String phone,
		String address,
		byte gender, byte isWithdraw) {
		this.uuid = uuid;
		this.accountId = accountId;
		this.name = name;
		this.password = password;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.gender = gender;
		this.isWithdraw = isWithdraw;
	}

	// @Column(name = "credited_at", nullable = false)
	// private LocalDateTime creditedAt;
	//
	// @Column(name = "updated_at", nullable = false)
	// private LocalDateTime updateAt;

	public static Member toWithdrawMember(Member withdrawMember) {
		LocalDateTime currentTime = LocalDateTime.now();

		return Member.builder()
			.memberId(withdrawMember.getMemberId())
			.uuid(withdrawMember.getUuid())
			.accountId(withdrawMember.getAccountId())
			.name(withdrawMember.getName())
			.password(withdrawMember.getPassword())
			.email(withdrawMember.getEmail())
			.phone(withdrawMember.getPhone())
			.address(withdrawMember.getAddress())
			.gender(withdrawMember.getGender())
			.isWithdraw((byte)1)
			.withdrawAt(currentTime)
			.creditedAt(withdrawMember.getCreditedAt())
			.updateAt(currentTime)
			.build();
	}
}
