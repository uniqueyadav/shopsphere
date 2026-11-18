package com.shopsphere.product.service;

public interface ProductImageStorage {

	StoredProductImage upload(byte[] content, String folder);

	void delete(String publicId);
}
