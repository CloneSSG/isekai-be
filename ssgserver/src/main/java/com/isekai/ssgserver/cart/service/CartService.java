package com.isekai.ssgserver.cart.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.isekai.ssgserver.cart.dto.CartCountResponseDto;
import com.isekai.ssgserver.cart.dto.CartInfoDto;
import com.isekai.ssgserver.cart.dto.CartRequestDto;
import com.isekai.ssgserver.cart.dto.CartResponseDto;
import com.isekai.ssgserver.cart.entity.Cart;
import com.isekai.ssgserver.cart.repository.CartRepository;
import com.isekai.ssgserver.delivery.repository.ProductDeliveryTypeRepository;
import com.isekai.ssgserver.exception.common.CustomException;
import com.isekai.ssgserver.exception.constants.ErrorCode;
import com.isekai.ssgserver.option.entity.Option;
import com.isekai.ssgserver.option.repository.OptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final OptionRepository optionRepository;
	private final ProductDeliveryTypeRepository productDeliveryTypeRepository;

	// 장바구니 조회
	// 회원
	public CartResponseDto getMemberCart(String uuid) {

		List<Cart> carts = cartRepository.findByUuidOrderByCreatedAtDesc(uuid);
		List<CartInfoDto> normalItems = new ArrayList<>();
		List<CartInfoDto> ssgItems = new ArrayList<>();

		AtomicInteger normalId = new AtomicInteger(0);
		AtomicInteger ssgId = new AtomicInteger(0);

		for (Cart cart : carts) {
			// productCode를 기반으로 DeliveryType 찾기
			productDeliveryTypeRepository.findByProductCode(cart.getOption().getProductCode())
				.ifPresent(productDeliveryType -> {
					CartInfoDto itemDTO = CartInfoDto.builder()
						.id("ssg".equalsIgnoreCase(productDeliveryType.getDeliveryType().getEngName()) ?
							ssgId.getAndIncrement() : normalId.getAndIncrement()) // 조건에 따라 ID 할당
						.code(cart.getOption().getProductCode()) // `Option`을 통해 `productCode` 접근
						.count(cart.getCount())
						.checked(cart.getChecked())
						.optionId(cart.getOption().getOptionsId())
						.build();

					if ("ssg".equalsIgnoreCase(productDeliveryType.getDeliveryType().getEngName())) {
						ssgItems.add(itemDTO);
					} else {
						normalItems.add(itemDTO);
					}
				});
		}

		CartResponseDto cartResponse = CartResponseDto.builder()
			.id(0)
			.normal(normalItems)
			.ssg(ssgItems)
			.build();
		return cartResponse;
	}

	// 비회원
	public CartResponseDto getNonMemberCart(String cartValue) {

		List<Cart> carts = cartRepository.findByCartValueOrderByCreatedAtDesc(cartValue);
		List<CartInfoDto> normalItems = new ArrayList<>();
		List<CartInfoDto> ssgItems = new ArrayList<>();

		AtomicInteger normalId = new AtomicInteger(0);
		AtomicInteger ssgId = new AtomicInteger(0);

		for (Cart cart : carts) {
			// productCode를 기반으로 DeliveryType 찾기
			productDeliveryTypeRepository.findByProductCode(cart.getOption().getProductCode())
				.ifPresent(productDeliveryType -> {
					CartInfoDto itemDTO = CartInfoDto.builder()
						.id("ssg".equalsIgnoreCase(productDeliveryType.getDeliveryType().getEngName()) ?
							ssgId.getAndIncrement() : normalId.getAndIncrement()) // 조건에 따라 ID 할당
						.cartId(cart.getCartId())
						.code(cart.getOption().getProductCode()) // `Option`을 통해 `productCode` 접근
						.count(cart.getCount())
						.checked(cart.getChecked())
						.optionId(cart.getOption().getOptionsId())
						.build();

					if ("ssg".equalsIgnoreCase(productDeliveryType.getDeliveryType().getEngName())) {
						ssgItems.add(itemDTO);
					} else {
						normalItems.add(itemDTO);
					}
				});
		}

		CartResponseDto cartResponse = CartResponseDto.builder()
			.id(0)
			.normal(normalItems)
			.ssg(ssgItems)
			.build();
		return cartResponse;
	}

	// 장바구니 담기
	// 회원
	public ResponseEntity<?> addMemberCartProduct(CartRequestDto cartRequestDto, String uuid) {

		List<Cart> carts = cartRepository.findByUuid(uuid);
		Option option = optionRepository.findById(cartRequestDto.getOptionsId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		// 이미 장바구니에 있는지 확인
		for (Cart cart : carts) {
			if (cart.getOption().getOptionsId().equals(cartRequestDto.getOptionsId())) {
				cartRepository.save(Cart.builder()
					.cartId(cart.getCartId())
					.uuid(uuid)
					.option(option)
					.count(cart.getCount() + cartRequestDto.getCount())
					.checked(cart.getChecked())
					.build());
				return ResponseEntity.ok("Ok");
			}
		}
		// 새롭게 담는 상품
		cartRepository.save(Cart.builder()
			.uuid(uuid)
			.option(option)
			.count(cartRequestDto.getCount())
			.checked((byte)0)
			.build());
		return ResponseEntity.ok("Ok");
	}

	// 비회원
	public ResponseEntity<?> addNonMemberCartProduct(CartRequestDto cartRequestDto, String cartValue) {

		List<Cart> carts = cartRepository.findByCartValue(cartValue);
		Option option = optionRepository.findById(cartRequestDto.getOptionsId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));

		// 이미 장바구니에 있는지 확인
		for (Cart cart : carts) {
			if (cart.getOption().getOptionsId().equals(cartRequestDto.getOptionsId())) {
				cartRepository.save(Cart.builder()
					.cartId(cart.getCartId())
					.cartValue(cartValue)
					.option(option)
					.count(cart.getCount() + cartRequestDto.getCount())
					.checked(cart.getChecked())
					.build());
				return ResponseEntity.ok("Ok");
			}
		}
		// 새롭게 담는 상품
		cartRepository.save(Cart.builder()
			.cartValue(cartValue)
			.option(option)
			.count(cartRequestDto.getCount())
			.checked((byte)0)
			.build());
		return ResponseEntity.ok("Ok");
	}

	// 장바구니 총 개수
	// 회원
	public CartCountResponseDto getMemberCartCount(String uuid) {

		Integer cnt = cartRepository.countByUuid(uuid);

		return CartCountResponseDto.builder()
			.id(0)
			.cnt(cnt)
			.build();
	}

	// 비회원
	public CartCountResponseDto getNonMemberCartCount(String cartValue) {

		Integer cnt = cartRepository.countByCartValue(cartValue);

		return CartCountResponseDto.builder()
			.id(0)
			.cnt(cnt)
			.build();
	}
}