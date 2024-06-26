package com.isekai.ssgserver.deliveryAddress.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.isekai.ssgserver.common.dto.DataIdDto;
import com.isekai.ssgserver.deliveryAddress.dto.DeliveryAddressCreateDto;
import com.isekai.ssgserver.member.service.MemberService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.isekai.ssgserver.deliveryAddress.dto.DeliveryAddressInfoDto;
import com.isekai.ssgserver.deliveryAddress.dto.DeliveryAddressListDto;
import com.isekai.ssgserver.deliveryAddress.dto.DeliveryAddressNicknameDto;
import com.isekai.ssgserver.deliveryAddress.entity.DeliveryAddress;
import com.isekai.ssgserver.deliveryAddress.repository.DeliveryAddressRepository;
import com.isekai.ssgserver.exception.common.CustomException;
import com.isekai.ssgserver.exception.constants.ErrorCode;
import com.isekai.ssgserver.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryAddressService {

	private final DeliveryAddressRepository deliveryAddressRepository;
	private final MemberService memberService;

	public DeliveryAddressNicknameDto getDeliveryAddressNickname(String uuid, Long deliveryAddressId) {

		DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(deliveryAddressId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		verifyDeliveryAddressByMember(uuid, deliveryAddress.getMemberId());

		return DeliveryAddressNicknameDto.builder()
				.nickname(deliveryAddress.getNickname())
				.build();
	}

	public DeliveryAddressInfoDto getDeliveryAddressInfo(String uuid, Long deliveryAddressId) {

		DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(deliveryAddressId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		if (deliveryAddress.getMemberId() != -1L) { // 회원의 배송지인 경우
			verifyDeliveryAddressByMember(uuid, deliveryAddress.getMemberId());
		}

		return DeliveryAddressInfoDto.builder()
				.nickname(deliveryAddress.getNickname())
				.name(deliveryAddress.getName())
				.cellphone(deliveryAddress.getCellphone())
				.telephone(deliveryAddress.getTelephone())
				.zipcode(deliveryAddress.getZipcode())
				.address(deliveryAddress.getAddress())
				.isDefault(deliveryAddress.isDefault())
				.isDeleted(deliveryAddress.isDeleted())
				.orderHistory(deliveryAddress.isOrderHistory())
				.build();

	}

	public List<DeliveryAddressListDto> getMembersDeliveryAddressList(String uuid) {

		AtomicInteger id = new AtomicInteger(0);

		Long memberId = memberService.getMemberIdByUuid(uuid);

		return deliveryAddressRepository.findAllByMemberId(memberId)
				.stream()
				.map(da -> DeliveryAddressListDto.builder()
						.id(id.getAndIncrement())
						.deliveryAddressId(da.getDeliveryAddressId())
						.build())
				.toList();

	}

	@Transactional
	public void softDeleteDeliveryAddress(String uuid, Long deliveryAddressId) {

		DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(deliveryAddressId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		verifyDeliveryAddressByMember(uuid, deliveryAddress.getMemberId());


		deliveryAddressRepository.delete(deliveryAddress);
	}

	@Transactional
	public DataIdDto createDeliveryAddress(String uuid, DeliveryAddressCreateDto deliveryAddressCreateDto) {

		Long memberId = -1L;  // 비회원
		if (uuid != null) {   // 회원
			memberId = memberService.getMemberIdByUuid(uuid);
		}

		String nickname = deliveryAddressCreateDto.getNickname();
		if (nickname == null) nickname = "자택";

		DeliveryAddress deliveryAddress = DeliveryAddress.builder()
				.memberId(memberId)
				.nickname(nickname)
				.name(deliveryAddressCreateDto.getName())
				.cellphone(deliveryAddressCreateDto.getCellphone())
				.telephone(deliveryAddressCreateDto.getTelephone())
				.zipcode(deliveryAddressCreateDto.getZipcode())
				.address(deliveryAddressCreateDto.getAddress())
				.isDefault(false)
				.isDeleted(false)
				.orderHistory(false)
				.build();

		Long savedId = deliveryAddressRepository.save(deliveryAddress).getDeliveryAddressId();

		return DataIdDto.builder()
				.id(savedId)
				.build();
	}

	@Transactional
	public DataIdDto updateDeliveryAddress(String uuid, Long deliveryAddressId, DeliveryAddressCreateDto deliveryAddressCreateDto) {

		DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(deliveryAddressId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		verifyDeliveryAddressByMember(uuid, deliveryAddress.getMemberId());

		deliveryAddress.update(deliveryAddressCreateDto);

		return DataIdDto.builder()
				.id(deliveryAddressId)
				.build();
	}

	public DataIdDto setDefaultDeliveryAddress(String uuid, Long deliveryAddressId) {

		DeliveryAddress newDefault = deliveryAddressRepository.findById(deliveryAddressId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		verifyDeliveryAddressByMember(uuid, newDefault.getMemberId());

		// 기존 default 배송지 -> false
		Optional<DeliveryAddress> originDefault = deliveryAddressRepository.findByMemberIdAndIsDefault(newDefault.getMemberId(), true);
		if (originDefault.isPresent()) {
			originDefault.get().changeIsDefault(false);
			deliveryAddressRepository.save(originDefault.get());
		}

		newDefault.changeIsDefault(true);
		deliveryAddressRepository.save(newDefault);

		return DataIdDto.builder()
				.id(newDefault.getDeliveryAddressId())
				.build();
	}

	/**
	 * 클라이언트의 인증된 회원 정보 - 배송지 매칭 확인
	 * @param uuid token에서 파싱한 회원 정보 (uuid)
	 * @param deliveryAddressMemberId 클라이언트가 보낸 {deliveryAddressId}에 저장된 {memberId}
	 */
	private void verifyDeliveryAddressByMember(String uuid, Long deliveryAddressMemberId) {
		Long savedMemberId = memberService.getMemberIdByUuid(uuid);

		if (!savedMemberId.equals(deliveryAddressMemberId)) {
			throw new CustomException(ErrorCode.NO_AUTHORITY);
		}
	}

}
