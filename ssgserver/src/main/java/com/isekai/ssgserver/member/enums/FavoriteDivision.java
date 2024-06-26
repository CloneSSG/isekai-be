package com.isekai.ssgserver.member.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FavoriteDivision {
	SINGLE_PRODUCT((byte)0, "단일상품"),
	BUNDLE_PRODUCT((byte)1, "묶음상품"),
	CATEGORYM((byte)2, "카테고리M"),
	CATEGORYS((byte)3, "카테고리S"),
	BRAND((byte)4, "브랜드");

	private final byte code;
	private final String description;

	public static FavoriteDivision fromCode(byte code) {
		for (FavoriteDivision division : FavoriteDivision.values()) {
			if (division.getCode() == code) {
				return division;
			}
		}
		throw new IllegalArgumentException("Invalid division code: " + code);
	}
}