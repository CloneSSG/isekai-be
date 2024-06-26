package com.isekai.ssgserver.member.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.isekai.ssgserver.exception.common.CustomException;
import com.isekai.ssgserver.exception.constants.ErrorCode;
import com.isekai.ssgserver.member.dto.WithdrawInfoDto;
import com.isekai.ssgserver.member.entity.Member;
import com.isekai.ssgserver.member.entity.WithdrawInfo;
import com.isekai.ssgserver.member.repository.MemberRepository;
import com.isekai.ssgserver.member.repository.WithdrawRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawService {
	private final WithdrawRepository withdrawRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public void updateWithdraw(String uuid) {

		Member member = memberRepository.findByUuid(uuid)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		ModelMapper modelMapper = new ModelMapper();
		Member updatedMember = modelMapper.map(member, Member.class);
		updatedMember.setIsWithdraw((byte)1);

		memberRepository.save(updatedMember);
	}

	@Transactional
	public void addWithdrawReasons(String uuid, WithdrawInfoDto withdrawInfoDto) {
		WithdrawInfo withdrawInfo = WithdrawInfo.builder()
			.uuid(uuid)
			.reason(withdrawInfoDto.getReason())
			.build();

		withdrawRepository.save(withdrawInfo);
	}
}
