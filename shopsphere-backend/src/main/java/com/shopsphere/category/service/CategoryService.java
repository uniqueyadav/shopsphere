package com.shopsphere.category.service;

import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.shopsphere.category.dto.CategoryPageResponse;
import com.shopsphere.category.dto.CategoryRequest;
import com.shopsphere.category.dto.CategoryResponse;
import com.shopsphere.category.dto.CategoryStatsResponse;
import com.shopsphere.category.entity.Category;
import com.shopsphere.category.repository.CategoryRepository;
import com.shopsphere.product.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

	private final CategoryRepository categories;
	private final ProductRepository products;

	public CategoryService(CategoryRepository categories, ProductRepository products) {
		this.categories = categories;
		this.products = products;
	}

	@Transactional(readOnly = true)
	public CategoryPageResponse listAll(int page, int size, String search) {
		return listAll(page, size, search, null, null, "name_asc");
	}

	@Transactional(readOnly = true)
	public CategoryPageResponse listAll(int page, int size, String search, Boolean active, Long parentId, String sort) {
		Pageable pageable = pageRequest(page, size, sort);
		String query = normalizeSearch(search);
		Specification<Category> specification = (root, criteriaQuery, builder) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			if (query != null) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("name")), pattern),
						builder.like(builder.lower(root.get("slug")), pattern)));
			}
			if (active != null) predicates.add(builder.equal(root.get("isActive"), active));
			if (parentId != null) {
				predicates.add(parentId == 0
						? builder.isNull(root.get("parentCategory"))
						: builder.equal(root.get("parentCategory").get("id"), parentId));
			}
			return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
		return CategoryPageResponse.from(categories.findAll(specification, pageable).map(this::response));
	}

	@Transactional(readOnly = true)
	public CategoryPageResponse listActive(int page, int size, String search) {
		Pageable pageable = pageRequest(page, size, "name_asc");
		String query = normalizeSearch(search);
		Page<Category> result = query == null
				? categories.findByIsActiveTrue(pageable)
				: categories.findByIsActiveTrueAndNameContainingIgnoreCase(query, pageable);
		return CategoryPageResponse.from(result.map(this::response));
	}

	@Transactional(readOnly = true)
	public CategoryResponse getById(Long id) {
		return response(findCategory(id));
	}

	@Transactional(readOnly = true)
	public CategoryStatsResponse statistics() {
		return new CategoryStatsResponse(categories.count(), categories.countByIsActiveTrue(),
				categories.countByIsActiveFalse(), categories.countByParentCategoryIsNotNull());
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> parentCategories() {
		return categories.findByParentCategoryIsNullOrderByNameAsc().stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse getActiveById(Long id) {
		Category category = findCategory(id);
		if (!category.isActive()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found.");
		}
		return response(category);
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = normalizeName(request.name());
		String slug = resolveSlug(request.slug(), name);
		ensureUnique(name, slug, null);
		Category category = new Category(
				name,
				slug,
				normalizeOptional(request.description()),
				normalizeOptional(request.imageUrl()),
				request.isActive() == null || request.isActive());
		category.setParentCategory(resolveParent(request.parentId(), null));
		category.setImagePublicId(normalizeOptional(request.imagePublicId()));
		return response(categories.save(category));
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = findCategory(id);
		String name = normalizeName(request.name());
		String slug = resolveSlug(request.slug(), name);
		ensureUnique(name, slug, id);
		Category parent = resolveParent(request.parentId(), id);
		category.update(
				name,
				slug,
				normalizeOptional(request.description()),
				normalizeOptional(request.imageUrl()),
				normalizeOptional(request.imagePublicId()),
				parent,
				request.isActive());
		return response(category);
	}

	@Transactional
	public CategoryResponse delete(Long id) {
		Category category = findCategory(id);
		category.deactivate();
		return response(category);
	}

	private Pageable pageRequest(int page, int size, String sort) {
		Sort order = switch (sort == null ? "name_asc" : sort) {
			case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
			case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
			case "name_desc" -> Sort.by(Sort.Order.desc("name"), Sort.Order.asc("id"));
			default -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
		};
		return PageRequest.of(page, size, order);
	}

	private CategoryResponse response(Category category) {
		List<Category> children = categories.findByParentCategory_IdOrderByNameAsc(category.getId());
		return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
				category.getImageUrl(), category.getImagePublicId(),
				category.getParentCategory() == null ? null : category.getParentCategory().getId(),
				category.getParentCategory() == null ? null : category.getParentCategory().getName(),
				category.isActive(), category.getCreatedAt(), category.getUpdatedAt(),
				products.countByCategory_Id(category.getId()), children.size(),
				children.stream().map(Category::getName).toList());
	}

	private Category resolveParent(Long parentId, Long currentId) {
		if (parentId == null) return null;
		if (currentId != null && parentId.equals(currentId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A category cannot be its own parent.");
		}
		Category parent = findCategory(parentId);
		Set<Long> visited = new HashSet<>();
		Category ancestor = parent;
		while (ancestor != null) {
			Long ancestorId = ancestor.getId();
			if (!visited.add(ancestorId) || (currentId != null && currentId.equals(ancestorId))) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category hierarchy cannot contain a cycle.");
			}
			ancestor = ancestor.getParentCategory();
		}
		return parent;
	}

	private Category findCategory(Long id) {
		return categories.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category was not found."));
	}

	private void ensureUnique(String name, String slug, Long currentId) {
		boolean duplicateName = currentId == null
				? categories.existsByNameIgnoreCase(name)
				: categories.existsByNameIgnoreCaseAndIdNot(name, currentId);
		if (duplicateName) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists.");
		}

		boolean duplicateSlug = currentId == null
				? categories.existsBySlugIgnoreCase(slug)
				: categories.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
		if (duplicateSlug) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this slug already exists.");
		}
	}

	private String resolveSlug(String requestedSlug, String name) {
		String slug = requestedSlug == null || requestedSlug.isBlank()
				? name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("(^-|-$)", "")
				: requestedSlug.trim().toLowerCase(Locale.ROOT);
		if (slug.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Category name must contain a letter or number to generate a slug.");
		}
		return slug;
	}

	private String normalizeName(String name) {
		return name.trim();
	}

	private String normalizeSearch(String search) {
		return search == null || search.isBlank() ? null : search.trim();
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
