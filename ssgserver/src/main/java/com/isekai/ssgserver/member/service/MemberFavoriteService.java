package com.isekai.ssgserver.member.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.isekai.ssgserver.category.repository.CategoryMRepository;
import com.isekai.ssgserver.category.repository.CategorySRepository;
import com.isekai.ssgserver.exception.common.CustomException;
import com.isekai.ssgserver.exception.constants.ErrorCode;
import com.isekai.ssgserver.member.dto.FavoriteCountDto;
import com.isekai.ssgserver.member.dto.FavoriteCountResponseDto;
import com.isekai.ssgserver.member.dto.FavoriteDelReqDto;
import com.isekai.ssgserver.member.dto.FavoriteDelRequestDto;
import com.isekai.ssgserver.member.dto.FavoriteListResDto;
import com.isekai.ssgserver.member.dto.FavoritePutReqDto;
import com.isekai.ssgserver.member.dto.FavoriteReqDto;
import com.isekai.ssgserver.member.dto.FavoriteResDto;
import com.isekai.ssgserver.member.entity.Favorite;
import com.isekai.ssgserver.member.enums.FavoriteDivision;
import com.isekai.ssgserver.member.repository.MemberFavoriteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFavoriteService {
	private final MemberFavoriteRepository memberFavoriteRepository;
	private final CategoryMRepository categoryMRepository;
	private final CategorySRepository categorySRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void addFavoriteTotal(FavoritePutReqDto favoriteReqDto) {
		byte division = favoriteReqDto.getDivision().getCode();
		String uuid = favoriteReqDto.getUuid();

		if (division == 2) {
			// 카테고리M (2)
			String mediumName = favoriteReqDto.getIdentifier();

			String modifiedMediumName = mediumName.replace('-', '/');
			Long identifier = categoryMRepository.findByMediumName(modifiedMediumName);

			Favorite favorite = Favorite.builder()
				.uuid(uuid)
				.division(division)
				.identifier(identifier)
				.build();

			memberFavoriteRepository.save(favorite);

		} else if (division == 3) {
			// 카테고리S (3)
			String smallName = favoriteReqDto.getIdentifier();
			String[] categories = smallName.split(">");
			String categoryM = categories[0].trim();
			String categoryS = categories[1].trim();

			String modifiedMediumName = categoryM.replace('-', '/');
			String modifiedSmallName = categoryS.replace('-', '/');

			Long categoryMId = categoryMRepository.findByMediumName(modifiedMediumName);
			Long identifier = categorySRepository.findBySmallAndCategoryMId(modifiedSmallName, categoryMId);

			Favorite favorite = Favorite.builder()
				.uuid(uuid)
				.division(division)
				.identifier(identifier)
				.build();

			memberFavoriteRepository.save(favorite);

		} else {
			// 나머지 ( 단일상품, 묶음상품, 브랜드 )
			String tempId = favoriteReqDto.getIdentifier();

			long identifier = Long.parseLong(tempId);

			Favorite favorite = Favorite.builder()
				.uuid(uuid)
				.division(division)
				.identifier(identifier)
				.build();

			memberFavoriteRepository.save(favorite);
		}
	}

	@Transactional
	public void removeFavoriteOne(String uuid, Long favoriteId) {
		Favorite favoriteOptional = memberFavoriteRepository.findByFavoriteIdAndUuid(favoriteId, uuid)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ENTITY));
		memberFavoriteRepository.deleteById(favoriteId);
	}

	@Transactional
	public void removeFavoriteList(String uuid, FavoriteDelRequestDto favoriteDelRequestDto) {
		List<FavoriteDelReqDto> favoriteDelList = favoriteDelRequestDto.getFavoriteDelList();

		for (FavoriteDelReqDto favoriteDelReqDto : favoriteDelList) {
			Long favoriteId = favoriteDelReqDto.getFavoriteId();
			memberFavoriteRepository.findByFavoriteIdAndUuid(favoriteId, uuid)
				.ifPresentOrElse(
					favorite -> memberFavoriteRepository.deleteById(favoriteId),
					() -> {
						throw new CustomException(ErrorCode.NOT_FOUND_ENTITY);
					}
				);
		}
	}

	@Transactional
	public FavoriteCountResponseDto getFavoriteCountList() {
		// 상품 전체 및 묶음상품 갯수
		Long totalCount = memberFavoriteRepository.countByDivisionEqualsOrDivisionEquals((byte)0, (byte)1);
		// 브랜드 갯수
		Long brandCount = memberFavoriteRepository.countByDivision((byte)4);
		// 카테고리 L, M 갯수
		Long categoryCount = memberFavoriteRepository.countByDivisionEqualsOrDivisionEquals((byte)2, (byte)3);

		return FavoriteCountResponseDto.builder()
			.favoriteCountList(new ArrayList<FavoriteCountDto>() {{
				add(new FavoriteCountDto((byte)1, totalCount));
				add(new FavoriteCountDto((byte)2, brandCount));
				add(new FavoriteCountDto((byte)3, categoryCount));
			}})
			.build();
	}

	@Transactional
	public boolean getFavoriteIsDetails(String uuid, FavoriteReqDto favoriteReqDto) {
		Long identifierModify;
		String identifier = favoriteReqDto.getIdentifier();
		byte division = favoriteReqDto.getDivision().getCode();

		if (division == 2) {
			String modifiedMediumName = identifier.replace('-', '/');
			identifierModify = categoryMRepository.findByMediumName(modifiedMediumName);
		} else if (division == 3) {
			String smallName = favoriteReqDto.getIdentifier();
			String[] categories = smallName.split(">");
			String categoryM = categories[0].trim();
			String categoryS = categories[1].trim();

			String modifiedMediumName = categoryM.replace('-', '/');
			String modifiedSmallName = categoryS.replace('-', '/');

			Long categoryMId = categoryMRepository.findByMediumName(modifiedMediumName);
			identifierModify = categorySRepository.findBySmallAndCategoryMId(modifiedSmallName, categoryMId);

		} else {
			identifierModify = Long.parseLong(identifier);
		}

		return memberFavoriteRepository.existsByUuidAndDivisionAndIdentifier(uuid, division, identifierModify);
	}

	@Transactional
	public FavoriteListResDto getFavoriteCategoryList(String uuid, int page) {
		Pageable pageable = PageRequest.of(page, 30);

		Page<Object[]> categoryList = memberFavoriteRepository.findByUuidCategory(uuid, pageable);
		List<FavoriteResDto> favoriteCategoryResDtoList = new ArrayList<>();
		for (Object[] result : categoryList) {
			Long favoriteId = (Long)result[0];
			byte divisionCode = (byte)result[1];
			FavoriteDivision division = FavoriteDivision.fromCode(divisionCode);
			Long identifier = (Long)result[2];

			FavoriteResDto favoriteCategoryResDto = new FavoriteResDto(favoriteId, division,
				identifier);
			favoriteCategoryResDtoList.add(favoriteCategoryResDto);
		}

		return new FavoriteListResDto(favoriteCategoryResDtoList);
	}

	@Transactional
	public FavoriteListResDto getFavoriteSellerList(String uuid, int page) {
		Pageable pageable = PageRequest.of(page, 30);

		Page<Object[]> sellerList = memberFavoriteRepository.findByUuidSellrer(uuid, pageable);
		List<FavoriteResDto> favoriteSellerResDtoList = new ArrayList<>();
		for (Object[] result : sellerList) {
			Long favoriteId = (Long)result[0];
			byte divisionCode = (byte)result[1];
			FavoriteDivision division = FavoriteDivision.fromCode(divisionCode);
			Long identifier = (Long)result[2];
			FavoriteResDto favoriteSellerResDto = new FavoriteResDto(favoriteId, division, identifier);
			favoriteSellerResDtoList.add(favoriteSellerResDto);
		}

		return new FavoriteListResDto(favoriteSellerResDtoList);
	}
}
