package com.shopsphere.category.repository;

import com.shopsphere.category.entity.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

	Page<Category> findByIsActiveTrue(Pageable pageable);

	Page<Category> findByIsActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Category> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

	boolean existsByNameIgnoreCase(String name);

	boolean existsBySlugIgnoreCase(String slug);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

	long countByIsActiveTrue();

	long countByIsActiveFalse();

	long countByParentCategoryIsNotNull();

	long countByParentCategory_Id(Long parentId);

	boolean existsByParentCategory_Id(Long parentId);

	java.util.List<Category> findByParentCategory_IdOrderByNameAsc(Long parentId);

	java.util.List<Category> findByParentCategoryIsNullOrderByNameAsc();

	boolean existsByImagePublicId(String imagePublicId);
}
