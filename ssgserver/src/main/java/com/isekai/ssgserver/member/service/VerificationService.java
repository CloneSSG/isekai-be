package com.isekai.ssgserver.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isekai.ssgserver.exception.common.CustomException;
import com.isekai.ssgserver.exception.constants.ErrorCode;
import com.isekai.ssgserver.member.dto.VerificationDto;
import com.isekai.ssgserver.member.repository.VerificationRepository;
import com.isekai.ssgserver.util.PhoneVerificationUtil;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerificationService {

	private final PhoneVerificationUtil phoneVerificationUtil;
	private final VerificationRepository verificationRepository;

	// 이미 존재하는 회원인지 확인하는 로직 추가 필요
	/*
	@Autowired
	private UserRepository userRepository; // UserRepository 주입
	 */
	public void sendSms(VerificationDto.SmsVerificationRequest smsVerificationRequest) {
		String to = smsVerificationRequest.getPhone();

		/*
		// 휴대폰 번호로 기존 회원 조회
    	Optional<User> existingUser = userRepository.findByPhoneNumber(to);
    	if (existingUser.isPresent()) {
        // 이미 존재하는 회원인 경우 예외 처리
        throw new CustomException(ErrorCode.ALREADY_EXIST_USER);
    	}
		 */

		int randomNumber = (int) (Math.random() * 1000000); // 0부터 999999까지의 6자리 숫자
		String verificationNumber = String.format("%06d", randomNumber); // 6자리 숫자로 포맷
		phoneVerificationUtil.sendSms(to, verificationNumber);
		verificationRepository.createSmsVerification(to, verificationNumber);
	}

	public void verifySms(VerificationDto.SmsVerificationRequest smsVerificationRequest) {
		if (isVerify(smsVerificationRequest)) {
			throw new CustomException(ErrorCode.WRONG_NUMBER);
		}
		verificationRepository.removeSmsVerification(smsVerificationRequest.getPhone());
	}

	public boolean isVerify(VerificationDto.SmsVerificationRequest smsVerificationRequest) {
		return !(verificationRepository.hasKey(smsVerificationRequest.getPhone()) &&
			verificationRepository.getSmsVerification(smsVerificationRequest.getPhone())
				.equals(smsVerificationRequest.getVerificationNumber()));
	}
}